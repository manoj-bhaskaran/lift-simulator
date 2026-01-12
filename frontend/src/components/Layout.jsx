import { Link, Outlet } from 'react-router-dom';
import './Layout.css';

function Layout() {
  return (
    <div className="layout">
      <header className="header">
        <div className="header-content">
          <h1>Lift Simulator Admin</h1>
          <nav className="nav">
            <Link to="/" className="nav-link">
              Dashboard
            </Link>
            <Link to="/systems" className="nav-link">
              Lift Systems
            </Link>
            <Link to="/config-validator" className="nav-link">
              Config Validator
            </Link>
            <Link to="/health" className="nav-link">
              Health Check
            </Link>
          </nav>
        </div>
      </header>
      <main className="main-content">
        <Outlet />
      </main>
      <footer className="footer">
        <p>Lift Simulator Admin &copy; 2026</p>
      </footer>
    </div>
  );
}

export default Layout;
