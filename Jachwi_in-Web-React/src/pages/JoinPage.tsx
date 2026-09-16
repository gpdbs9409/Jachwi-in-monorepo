import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

type Step = "form" | "verify" | "done";

export function JoinPage() {
  const { sendMailConfirm, verifyMailConfirm, join } = useAuth();
  const navigate = useNavigate();

  const [step, setStep] = useState<Step>("form");
  const [email, setEmail] = useState("");
  const [name, setName] = useState("");
  const [nickname, setNickname] = useState("");
  const [password, setPassword] = useState("");
  const [school, setSchool] = useState("");
  const [code, setCode] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const onSendCode = async (e: FormEvent) => {
    e.preventDefault();
    setMessage(null);
    setLoading(true);
    try {
      await sendMailConfirm(email);
      setStep("verify");
      setMessage("인증코드를 이메일로 보냈어요.");
    } catch {
      setMessage("이메일 확인에 실패했어요. 이미 가입된 이메일일 수 있어요.");
    } finally {
      setLoading(false);
    }
  };

  const onVerifyAndJoin = async (e: FormEvent) => {
    e.preventDefault();
    setMessage(null);
    setLoading(true);
    try {
      const ok = await verifyMailConfirm(email, code);
      if (!ok) {
        setMessage("인증코드가 올바르지 않아요.");
        return;
      }
      await join({ email, name, nickname, password, school });
      setStep("done");
    } catch {
      setMessage("회원가입에 실패했어요.");
    } finally {
      setLoading(false);
    }
  };

  if (step === "done") {
    return (
      <div className="auth-form">
        <h2>회원가입 완료</h2>
        <p>이제 로그인할 수 있어요.</p>
        <button onClick={() => navigate("/login")}>로그인하러 가기</button>
      </div>
    );
  }

  if (step === "verify") {
    return (
      <form className="auth-form" onSubmit={onVerifyAndJoin}>
        <h2>이메일 인증</h2>
        <p>{email} 로 받은 인증코드를 입력해주세요.</p>
        <label>
          인증코드
          <input value={code} onChange={(e) => setCode(e.target.value)} required />
        </label>
        {message && <p className="form-error">{message}</p>}
        <button type="submit" disabled={loading}>
          {loading ? "처리 중..." : "인증하고 가입 완료"}
        </button>
      </form>
    );
  }

  return (
    <form className="auth-form" onSubmit={onSendCode}>
      <h2>회원가입</h2>
      <label>
        이메일
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
      </label>
      <label>
        이름
        <input value={name} onChange={(e) => setName(e.target.value)} required />
      </label>
      <label>
        닉네임
        <input value={nickname} onChange={(e) => setNickname(e.target.value)} required />
      </label>
      <label>
        비밀번호
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
      </label>
      <label>
        학교
        <input value={school} onChange={(e) => setSchool(e.target.value)} required />
      </label>
      {message && <p className="form-error">{message}</p>}
      <button type="submit" disabled={loading}>
        {loading ? "발송 중..." : "인증코드 받기"}
      </button>
    </form>
  );
}
