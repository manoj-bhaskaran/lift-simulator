import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import SimulationRuns from '../SimulationRuns';
import { simulationRunsApi } from '../../api/simulationRunsApi';
import { liftSystemsApi } from '../../api/liftSystemsApi';

vi.mock('../../api/simulationRunsApi', () => ({
  simulationRunsApi: {
    listRuns: vi.fn(),
    cancelRun: vi.fn(),
    deleteRun: vi.fn(),
  },
}));

vi.mock('../../api/liftSystemsApi', () => ({
  liftSystemsApi: {
    getAllSystems: vi.fn(),
  },
}));

const buildPage = (runs) => ({
  data: {
    content: runs,
    totalElements: runs.length,
    totalPages: 1,
    number: 0,
  },
});

const succeededRun = {
  id: 7,
  liftSystemName: 'Tower A',
  versionNumber: 1,
  scenarioName: 'Morning',
  status: 'SUCCEEDED',
  currentTick: 100,
  totalTicks: 100,
  startedAt: '2026-01-01T00:00:00Z',
  endedAt: '2026-01-01T00:01:00Z',
  createdAt: '2026-01-01T00:00:00Z',
};

const runningRun = { ...succeededRun, id: 8, status: 'RUNNING' };

const renderPage = () =>
  render(
    <MemoryRouter initialEntries={['/simulation-runs']}>
      <SimulationRuns />
    </MemoryRouter>
  );

describe('SimulationRuns delete action', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    liftSystemsApi.getAllSystems.mockResolvedValue({ data: [] });
  });

  it('shows a Delete action only for completed runs', async () => {
    simulationRunsApi.listRuns.mockResolvedValue(buildPage([succeededRun, runningRun]));
    renderPage();

    await screen.findByText('#7');
    // One delete button for the SUCCEEDED run, none for the RUNNING run.
    expect(screen.getAllByRole('button', { name: 'Delete' })).toHaveLength(1);
  });

  it('confirms then deletes a run and reloads the list with feedback', async () => {
    simulationRunsApi.listRuns
      .mockResolvedValueOnce(buildPage([succeededRun]))
      .mockResolvedValueOnce(buildPage([]));
    simulationRunsApi.deleteRun.mockResolvedValue({ status: 204 });
    renderPage();

    await screen.findByText('#7');
    fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

    // Confirmation dialog warns about artefacts and history removal.
    expect(screen.getByText(/stored artefacts/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Delete run' }));

    await waitFor(() => expect(simulationRunsApi.deleteRun).toHaveBeenCalledWith(7));
    expect(await screen.findByRole('status')).toHaveTextContent(
      'Run #7 and its artefacts were deleted.'
    );
    expect(simulationRunsApi.listRuns).toHaveBeenCalledTimes(2);
  });

  it('steps back a page when deleting the only row on a non-first page', async () => {
    // Page 0 holds run #7; page 1 holds the single run #21 (last page).
    simulationRunsApi.listRuns.mockImplementation((params = {}) => {
      const page = Number(params.page ?? 0);
      const content = page >= 1 ? [{ ...succeededRun, id: 21 }] : [succeededRun];
      return Promise.resolve({
        data: { content, totalElements: 21, totalPages: 2, number: page },
      });
    });
    simulationRunsApi.deleteRun.mockResolvedValue({ status: 204 });
    renderPage();

    await screen.findByText('#7');
    fireEvent.click(screen.getByRole('button', { name: /Next/ }));
    await screen.findByText('#21');

    const callsBeforeDelete = simulationRunsApi.listRuns.mock.calls.length;
    fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
    fireEvent.click(screen.getByRole('button', { name: 'Delete run' }));

    await waitFor(() => expect(simulationRunsApi.deleteRun).toHaveBeenCalledWith(21));
    // The deletion emptied page 1, so the reload must step back to page 0.
    await waitFor(() => {
      const reloadCalls = simulationRunsApi.listRuns.mock.calls.slice(callsBeforeDelete);
      expect(reloadCalls.some((call) => Number(call[0]?.page) === 0)).toBe(true);
    });
  });


  it('bulk cancels selected active runs and reports the outcome', async () => {
    const secondRunningRun = { ...runningRun, id: 9 };
    simulationRunsApi.listRuns
      .mockResolvedValueOnce(buildPage([runningRun, secondRunningRun]))
      .mockResolvedValueOnce(buildPage([]));
    simulationRunsApi.cancelRun.mockResolvedValue({ status: 200 });
    renderPage();

    await screen.findByText('#8');
    fireEvent.click(screen.getByLabelText('Select run #8'));
    fireEvent.click(screen.getByLabelText('Select run #9'));
    fireEvent.click(screen.getByRole('button', { name: 'Cancel Selected' }));
    fireEvent.click(screen.getByRole('button', { name: 'Cancel runs' }));

    await waitFor(() => expect(simulationRunsApi.cancelRun).toHaveBeenCalledWith(8));
    expect(simulationRunsApi.cancelRun).toHaveBeenCalledWith(9);
    expect(await screen.findByRole('status')).toHaveTextContent('2 runs cancelled successfully.');
  });

  it('bulk deletes selected completed runs and reports partial failures', async () => {
    const failedRun = { ...succeededRun, id: 10, status: 'FAILED' };
    simulationRunsApi.listRuns
      .mockResolvedValueOnce(buildPage([succeededRun, failedRun]))
      .mockResolvedValueOnce(buildPage([failedRun]));
    simulationRunsApi.deleteRun
      .mockResolvedValueOnce({ status: 204 })
      .mockRejectedValueOnce({ response: { data: { message: 'Locked' } } });
    renderPage();

    await screen.findByText('#7');
    fireEvent.click(screen.getByLabelText('Select all visible runs'));
    fireEvent.click(screen.getByRole('button', { name: 'Delete Selected' }));
    fireEvent.click(screen.getByRole('button', { name: 'Delete runs' }));

    await waitFor(() => expect(simulationRunsApi.deleteRun).toHaveBeenCalledWith(7));
    expect(simulationRunsApi.deleteRun).toHaveBeenCalledWith(10);
    expect(await screen.findByRole('status')).toHaveTextContent('1 run deleted successfully; 1 failed.');
    expect(await screen.findByText(/Failed to delete 1 selected run/)).toBeInTheDocument();
  });

  it('disables bulk actions for mixed active and completed selections', async () => {
    simulationRunsApi.listRuns.mockResolvedValue(buildPage([succeededRun, runningRun]));
    renderPage();

    await screen.findByText('#7');
    fireEvent.click(screen.getByLabelText('Select run #7'));
    fireEvent.click(screen.getByLabelText('Select run #8'));

    expect(screen.getByRole('button', { name: 'Cancel Selected' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Delete Selected' })).toBeDisabled();
    expect(screen.getByText(/Mixed states selected/)).toBeInTheDocument();
  });

  it('surfaces an error when deletion fails', async () => {
    simulationRunsApi.listRuns.mockResolvedValue(buildPage([succeededRun]));
    simulationRunsApi.deleteRun.mockRejectedValue({
      response: { data: { message: 'Run is still in progress' } },
    });
    renderPage();

    await screen.findByText('#7');
    fireEvent.click(screen.getByRole('button', { name: 'Delete' }));
    fireEvent.click(screen.getByRole('button', { name: 'Delete run' }));

    expect(await screen.findByText(/Run is still in progress/)).toBeInTheDocument();
  });
});
