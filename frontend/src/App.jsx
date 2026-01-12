import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import LiftSystems from './pages/LiftSystems';
import ConfigValidator from './pages/ConfigValidator';
import HealthCheck from './pages/HealthCheck';
import './App.css';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="systems" element={<LiftSystems />} />
          <Route path="config-validator" element={<ConfigValidator />} />
          <Route path="health" element={<HealthCheck />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
