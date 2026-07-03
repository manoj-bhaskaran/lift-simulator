import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Simulator from '../Simulator';
import { simulationRunsApi } from '../../api/simulationRunsApi';
import { liftSystemsApi } from '../../api/liftSystemsApi';
import { scenariosApi } from '../../api/scenariosApi';

vi.mock('../../api/simulationRunsApi', () => ({
  simulationRunsApi: {
    createRun: vi.fn(),
    getRun: vi.fn(),
    getResults: vi.fn(),
    getArtefacts: vi.fn(),
    cancelRun: vi.fn(),
  },
}));

vi.mock('../../api/liftSystemsApi', () => ({
  liftSystemsApi: {
    getAllSystems: vi.fn(),
    getVersions: vi.fn(),
  },
}));

vi.mock('../../api/scenariosApi', () => ({
  scenariosApi: {
    getAllScenarios: vi.fn(),
  },
}));

const system = { id: 1, displayName: 'Tower A' };
const version = { id: 1, versionNumber: 1, status: 'PUBLISHED' };
const scenario = { id: 100, name: 'Morning Rush', liftSystemVersionId: 1 };

const createdRun = {
  id: 55,
  status: 'CREATED',
  createdAt: '2026-01-01T00:00:00.000Z',
  totalTicks: 100,
  currentTick: 0,
};

const runningRun = { ...createdRun, status: 'RUNNING', currentTick: 40 };
const succeededRun = { ...createdRun, status: 'SUCCEEDED', currentTick: 100 };
const failedRun = { ...createdRun, status: 'FAILED', errorMessage: 'Engine crashed' };

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

const renderSimulator = () =>
  render(
    <MemoryRouter initialEntries={['/simulator']}>
      <Simulator />
    </MemoryRouter>
  );

const selectRunSetup = async () => {
  await screen.findByText('Run Setup');
  fireEvent.change(screen.getByLabelText('Lift System'), { target: { value: '1' } });
  await waitFor(() => expect(liftSystemsApi.getVersions).toHaveBeenCalled());
  fireEvent.change(screen.getByLabelText('Published Version'), { target: { value: '1' } });
  fireEvent.change(screen.getByLabelText('Scenario'), { target: { value: '100' } });
};

const startRun = async () => {
  await act(async () => {
    fireEvent.click(screen.getByRole('button', { name: 'Start Run' }));
  });
};

describe('Simulator lifecycle', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers({ shouldAdvanceTime: true });
    liftSystemsApi.getAllSystems.mockResolvedValue({ data: [system] });
    liftSystemsApi.getVersions.mockResolvedValue({ data: [version] });
    scenariosApi.getAllScenarios.mockResolvedValue({ data: [scenario] });
    simulationRunsApi.getArtefacts.mockResolvedValue({ data: [] });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('starts a run, polls RUNNING -> SUCCEEDED, and renders results', async () => {
    simulationRunsApi.createRun.mockResolvedValue({ data: createdRun });
    // Return the *same* RUNNING object reference for the first two polls (the
    // component polls once immediately, then again on the 3s interval) so
    // React bails out of re-rendering until the run actually turns terminal.
    let pollCount = 0;
    simulationRunsApi.getRun.mockImplementation(() => {
      pollCount += 1;
      return Promise.resolve({ data: pollCount < 3 ? runningRun : succeededRun });
    });
    simulationRunsApi.getResults.mockResolvedValue(resultsPayload);

    renderSimulator();
    await selectRunSetup();
    await startRun();

    // The run-status effect polls immediately once a non-terminal run exists,
    // so the first getRun response (RUNNING) is already reflected.
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
    simulationRunsApi.createRun.mockResolvedValue({ data: createdRun });
    simulationRunsApi.getRun.mockResolvedValue({ data: runningRun });
    const cancelledRun = { ...runningRun, status: 'CANCELLED' };
    simulationRunsApi.cancelRun.mockResolvedValue({ data: cancelledRun });

    renderSimulator();
    await selectRunSetup();
    await startRun();

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
    simulationRunsApi.createRun.mockResolvedValue({ data: createdRun });
    simulationRunsApi.getRun.mockResolvedValue({ data: failedRun });
    simulationRunsApi.getResults.mockResolvedValue({ data: { errorMessage: 'Engine crashed' } });

    renderSimulator();
    await selectRunSetup();
    await startRun();

    await waitFor(() => expect(screen.getByText('FAILED')).toBeInTheDocument());
    expect(await screen.findByText('Simulation failed.')).toBeInTheDocument();
    expect(screen.getByText('Engine crashed')).toBeInTheDocument();
  });

  it('surfaces a poll error without crashing the page', async () => {
    simulationRunsApi.createRun.mockResolvedValue({ data: createdRun });
    simulationRunsApi.getRun.mockRejectedValue({
      response: { data: { message: 'Run not found' } },
    });

    renderSimulator();
    await selectRunSetup();
    await startRun();

    expect(
      await screen.findByText(/Failed to refresh run status: Run not found/)
    ).toBeInTheDocument();
    // The run stays in its last known (non-terminal) state rather than crashing.
    expect(screen.getByText('CREATED')).toBeInTheDocument();
  });
});
