import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import LiftSystems from './pages/LiftSystems';
import LiftSystemDetail from './pages/LiftSystemDetail';
import ConfigEditor from './pages/ConfigEditor';
import ConfigValidator from './pages/ConfigValidator';
import HealthCheck from './pages/HealthCheck';
import Scenarios from './pages/Scenarios';
import ScenarioForm from './pages/ScenarioForm';
import './App.css';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="systems" element={<LiftSystems />} />
          <Route path="systems/:id" element={<LiftSystemDetail />} />
          <Route path="systems/:systemId/versions/:versionNumber/edit" element={<ConfigEditor />} />
          <Route path="scenarios" element={<Scenarios />} />
          <Route path="scenarios/new" element={<ScenarioForm />} />
          <Route path="scenarios/:id/edit" element={<ScenarioForm />} />
          <Route path="config-validator" element={<ConfigValidator />} />
          <Route path="health" element={<HealthCheck />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
