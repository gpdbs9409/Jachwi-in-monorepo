import { Link } from "react-router-dom";

export function HomePage() {
  return (
    <section className="hero">
      <h1>자취인</h1>
      <p>자취생을 위한 AI 기반 지도 + 커뮤니티 플랫폼</p>
      <div className="hero-actions">
        <Link to="/map" className="btn primary">지도 둘러보기</Link>
        <Link to="/posts" className="btn">커뮤니티 가기</Link>
      </div>
    </section>
  );
}
