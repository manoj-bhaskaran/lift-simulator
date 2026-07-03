import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import SimulationRunDetail from '../SimulationRunDetail';
import { simulationRunsApi } from '../../api/simulationRunsApi';

vi.mock('../../api/simulationRunsApi', () => ({
  simulationRunsApi: {
    getRun: vi.fn(),
    getResults: vi.fn(),
    getArtefacts: vi.fn(),
    cancelRun: vi.fn(),
    deleteRun: vi.fn(),
  },
}));

const baseRun = {
  id: 55,
  liftSystemId: 1,
  versionNumber: 1,
  scenarioId: 100,
  createdAt: '2026-01-01T00:00:00.000Z',
  startedAt: '2026-01-01T00:00:01.000Z',
  totalTicks: 100,
  currentTick: 0,
  seed: 42,
};

const runningRun = { ...baseRun, status: 'RUNNING', currentTick: 40 };
const succeededRun = { ...baseRun, status: 'SUCCEEDED', currentTick: 100, endedAt: '2026-01-01T00:02:00.000Z' };
const failedRun = {
  ...baseRun,
  status: 'FAILED',
  endedAt: '2026-01-01T00:01:00.000Z',
  errorMessage: 'Engine crashed',
};

const resultsPayload = {
  data: {
    results: {
      runSummary: { generatedAt: '2026-01-01T00:02:00.000Z', ticks: 100, durationTicks: 100, seed: 42 },
      kpis: { requestsTotal: 5 },
      perLift: [],
      perFloor: [],
    },
  },
};

const renderDetail = () =>
  render(
    <MemoryRouter initialEntries={['/simulation-runs/55']}>
      <Routes>
        <Route path="/simulation-runs/:id" element={<SimulationRunDetail />} />
      </Routes>
    </MemoryRouter>
  );

describe('SimulationRunDetail lifecycle', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers({ shouldAdvanceTime: true });
    simulationRunsApi.getArtefacts.mockResolvedValue({ data: [] });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('polls RUNNING -> SUCCEEDED and renders results', async () => {
    let pollCount = 0;
    simulationRunsApi.getRun.mockImplementation(() => {
      pollCount += 1;
      return Promise.resolve({ data: pollCount < 2 ? runningRun : succeededRun });
    });
    simulationRunsApi.getResults.mockResolvedValue(resultsPayload);

    renderDetail();

    expect(await screen.findByText('RUNNING')).toBeInTheDocument();
    expect(screen.getByText('40 / 100 ticks')).toBeInTheDocument();
    expect(screen.queryByText('SUCCEEDED')).not.toBeInTheDocument();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });

    await waitFor(() => expect(screen.getByText('SUCCEEDED')).toBeInTheDocument());
    expect(simulationRunsApi.getResults).toHaveBeenCalledWith(55);
    expect(await screen.findByText('Simulation completed successfully.')).toBeInTheDocument();
  });

  it('cancels a running simulation via the confirm modal', async () => {
    simulationRunsApi.getRun.mockResolvedValue({ data: runningRun });
    const cancelledRun = { ...runningRun, status: 'CANCELLED' };
    simulationRunsApi.cancelRun.mockResolvedValue({ data: cancelledRun });

    renderDetail();
    await screen.findByText('RUNNING');

    fireEvent.click(screen.getByRole('button', { name: 'Cancel Run' }));
    expect(screen.getByText(/This will stop the current simulation run/)).toBeInTheDocument();

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Cancel run' }));
    });

    await waitFor(() => expect(simulationRunsApi.cancelRun).toHaveBeenCalledWith(55));
    expect(await screen.findByText('CANCELLED')).toBeInTheDocument();
    expect(await screen.findByText('Simulation was cancelled.')).toBeInTheDocument();
  });

  it('renders the FAILED banner with the run error message', async () => {
    simulationRunsApi.getRun.mockResolvedValue({ data: failedRun });
    simulationRunsApi.getResults.mockResolvedValue({ data: { errorMessage: 'Engine crashed' } });

    renderDetail();

    await waitFor(() => expect(screen.getByText('FAILED')).toBeInTheDocument());
    expect(await screen.findByText('Simulation failed.')).toBeInTheDocument();
    expect(screen.getByText('Engine crashed')).toBeInTheDocument();
  });

  it('surfaces a poll error without crashing the page', async () => {
    simulationRunsApi.getRun.mockRejectedValue({
      response: { data: { message: 'Run not found' } },
    });

    renderDetail();

    expect(
      await screen.findByText(/Failed to load simulation run: Run not found/)
    ).toBeInTheDocument();
  });
});
