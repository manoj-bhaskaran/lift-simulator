import { Link, Outlet } from 'react-router-dom';
import './Layout.css';
import packageJson from '../../package.json';

/**
 * Main application layout component providing consistent header, navigation, and footer.
 * Uses React Router's Outlet to render child routes in the main content area.
 *
 * Navigation links:
 * - Dashboard: Main overview page
 * - Lift Systems: Manage lift system configurations
 * - Scenarios: Manage passenger flow scenarios
 * - Config Validator: Validate lift configuration JSON
 * - Health Check: System health status
 *
 * @returns {JSX.Element} The application layout component
 */
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
            <Link to="/scenarios" className="nav-link">
              Scenarios
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
        <p>Manoj Bhaskaran &copy; 2026 | Version {packageJson.version}</p>
      </footer>
    </div>
  );
}

export default Layout;
