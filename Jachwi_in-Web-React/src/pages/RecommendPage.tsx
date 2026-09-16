import { useState, type FormEvent } from "react";
import { recommendRooms } from "../api/llm";

const PREFERENCE_OPTIONS = ["편의점", "카페", "CCTV", "가로등", "병원", "식당", "버스정류장"];

export function RecommendPage() {
  const [school, setSchool] = useState("");
  const [budget, setBudget] = useState(50);
  const [centerX, setCenterX] = useState(127.0432);
  const [centerY, setCenterY] = useState(37.5567);
  const [radius, setRadius] = useState(1000);
  const [preferences, setPreferences] = useState<string[]>([]);
  const [result, setResult] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const togglePreference = (p: string) => {
    setPreferences((prev) => (prev.includes(p) ? prev.filter((x) => x !== p) : [...prev, p]));
  };

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    setResult(null);
    try {
      const res = await recommendRooms({ school, budget, centerX, centerY, radius, preferences });
      setResult(res);
    } catch {
      setError("추천을 불러오지 못했어요.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="recommend-page">
      <h2>AI 자취방 추천</h2>
      <form className="post-form" onSubmit={onSubmit}>
        <label>
          인근 학교
          <input value={school} onChange={(e) => setSchool(e.target.value)} required />
        </label>
        <label>
          월세 예산 (만원)
          <input
            type="number"
            value={budget}
            onChange={(e) => setBudget(Number(e.target.value))}
            required
          />
        </label>
        <div className="category-row">
          <label>
            검색 중심 경도
            <input
              type="number"
              step="0.0001"
              value={centerX}
              onChange={(e) => setCenterX(Number(e.target.value))}
            />
          </label>
          <label>
            검색 중심 위도
            <input
              type="number"
              step="0.0001"
              value={centerY}
              onChange={(e) => setCenterY(Number(e.target.value))}
            />
          </label>
          <label>
            검색 반경 (m)
            <input
              type="number"
              value={radius}
              onChange={(e) => setRadius(Number(e.target.value))}
            />
          </label>
        </div>
        <div>
          <p>선호 편의시설</p>
          <div className="preference-chips">
            {PREFERENCE_OPTIONS.map((p) => (
              <button
                type="button"
                key={p}
                className={preferences.includes(p) ? "active" : ""}
                onClick={() => togglePreference(p)}
              >
                {p}
              </button>
            ))}
          </div>
        </div>
        {error && <p className="form-error">{error}</p>}
        <button type="submit" disabled={loading}>
          {loading ? "추천 받는 중..." : "추천받기"}
        </button>
      </form>
      {result && (
        <div className="recommend-result">
          <h3>추천 결과</h3>
          <p>{result}</p>
        </div>
      )}
    </div>
  );
}
