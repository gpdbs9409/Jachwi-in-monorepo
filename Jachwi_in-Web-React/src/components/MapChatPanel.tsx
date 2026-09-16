import { useEffect, useRef, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { chatRecommend } from "../api/llm";
import { useAuth } from "../contexts/AuthContext";
import type { ChatMessage, MapBounds, RecommendedBuilding } from "../api/types";

interface Props {
  bounds: MapBounds | null;
  onRecommendations: (buildings: RecommendedBuilding[]) => void;
}

const MAX_HISTORY_TURNS = 6;

export function MapChatPanel({ bounds, onRecommendations }: Props) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      role: "assistant",
      content:
        "안녕하세요! 지도에 보이는 영역 안에서 원하는 자취방 조건을 편하게 말씀해주세요. " +
        "예: \"학교 근처에 조용하고 CCTV 많은 곳, 월 50만원 이하로 추천해줘\"",
    },
  ]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, loading]);

  const send = async () => {
    const text = input.trim();
    if (!text || loading) return;

    if (!bounds) {
      setMessages((prev) => [
        ...prev,
        { role: "user", content: text },
        { role: "assistant", content: "지도가 아직 준비되지 않았어요. 잠시 후 다시 시도해주세요." },
      ]);
      setInput("");
      return;
    }

    const nextMessages: ChatMessage[] = [...messages, { role: "user", content: text }];
    setMessages(nextMessages);
    setInput("");
    setLoading(true);

    try {
      const history = nextMessages.slice(-1 - MAX_HISTORY_TURNS, -1);
      const res = await chatRecommend({ message: text, history, ...bounds });
      setMessages((prev) => [...prev, { role: "assistant", content: res.reply }]);
      onRecommendations(res.buildings);
    } catch (err: any) {
      console.error("채팅 추천 실패", err);
      const status = err?.response?.status;
      const friendly =
        status === 401 || status === 403
          ? "로그인이 필요한 기능이에요. 다시 로그인 후 시도해주세요."
          : "추천을 가져오는 중 문제가 발생했어요. 잠시 후 다시 시도해주세요.";
      setMessages((prev) => [...prev, { role: "assistant", content: friendly }]);
    } finally {
      setLoading(false);
    }
  };

  // 비회원은 애초에 호출 자체가 백엔드에서 403으로 막히므로(다른 LLM 기능과 동일한 정책),
  // 실패 응답을 보여주는 대신 로그인 유도 화면을 먼저 보여준다.
  if (!isAuthenticated) {
    return (
      <div className="map-chat-panel">
        <div className="map-chat-header">🤖 AI 자취방 추천</div>
        <div className="map-chat-login-gate">
          <p>로그인하면 지도에 보이는 영역 안에서 AI에게 자취방을 추천받을 수 있어요.</p>
          <Link to="/login" state={{ from: location }} className="map-chat-login-link">
            로그인하러 가기
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="map-chat-panel">
      <div className="map-chat-header">🤖 AI 자취방 추천</div>
      <div className="map-chat-messages">
        {messages.map((m, i) => (
          <div key={i} className={`chat-bubble chat-bubble-${m.role}`}>
            {m.content}
          </div>
        ))}
        {loading && <div className="chat-bubble chat-bubble-assistant chat-loading">생각 중...</div>}
        <div ref={bottomRef} />
      </div>
      <div className="map-chat-input">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") send();
          }}
          placeholder="예: 편의점 가깝고 안전한 곳 추천해줘"
          disabled={loading}
        />
        <button onClick={send} disabled={loading || !input.trim()}>
          전송
        </button>
      </div>
      <p className="map-chat-hint">현재 지도에 보이는 영역 안에서 찾아드려요. 원하는 지역으로 지도를 옮겨보세요.</p>
    </div>
  );
}
