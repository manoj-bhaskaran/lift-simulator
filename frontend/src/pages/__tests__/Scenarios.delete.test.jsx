import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Scenarios from '../Scenarios';
import { scenariosApi } from '../../api/scenariosApi';

vi.mock('../../api/scenariosApi', () => ({
  scenariosApi: {
    getAllScenarios: vi.fn(),
    getScenarioRunCount: vi.fn(),
    deleteScenario: vi.fn(),
  },
}));

vi.mock('../../api/liftSystemsApi', () => ({
  liftSystemsApi: {
    getAllSystems: vi.fn(),
    getVersions: vi.fn(),
  },
}));

const scenario = {
  id: 42,
  name: 'Morning rush',
  scenarioJson: { durationTicks: 120, passengerFlows: [] },
  createdAt: '2026-01-01T00:00:00Z',
};

const renderPage = () => render(
  <MemoryRouter initialEntries={['/scenarios']}>
    <Scenarios />
  </MemoryRouter>
);

describe('Scenarios deletion', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('warns about associated run history and artefacts before deleting', async () => {
    scenariosApi.getAllScenarios
      .mockResolvedValueOnce({ data: [scenario] })
      .mockResolvedValueOnce({ data: [] });
    scenariosApi.getScenarioRunCount.mockResolvedValue({ data: 3 });
    scenariosApi.deleteScenario.mockResolvedValue({ status: 204 });

    renderPage();

    await screen.findByText('Morning rush');
    fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(scenariosApi.getScenarioRunCount).toHaveBeenCalledWith(42));
    expect(screen.getByText(/permanently delete 3 simulation run\(s\), their history, and their artefacts/i))
      .toBeInTheDocument();

    const dialog = screen.getByRole('dialog', { name: 'Delete Scenario' });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(scenariosApi.deleteScenario).toHaveBeenCalledWith(42));
    expect(scenariosApi.getAllScenarios).toHaveBeenCalledTimes(2);
  });
});
