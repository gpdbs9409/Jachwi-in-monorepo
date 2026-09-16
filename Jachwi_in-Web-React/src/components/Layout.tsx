import { Link, Outlet } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";

export function Layout() {
  const { isAuthenticated, email, logout } = useAuth();

  return (
    <div className="app-shell">
      <header className="navbar">
        <Link to="/" className="brand">자취인</Link>
        <nav>
          <Link to="/map">지도</Link>
          <Link to="/posts">커뮤니티</Link>
        </nav>
        <div className="auth-area">
          {isAuthenticated ? (
            <>
              <span className="user-email">{email}</span>
              <button onClick={() => logout()}>로그아웃</button>
            </>
          ) : (
            <>
              <Link to="/login">로그인</Link>
              <Link to="/join">회원가입</Link>
            </>
          )}
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
