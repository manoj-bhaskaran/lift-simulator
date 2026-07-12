// @ts-check
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { liftSystemsApi } from '../api/liftSystemsApi';
import { simulationRunsApi } from '../api/simulationRunsApi';
import { getApiErrorMessage, handleApiError, logApiError } from '../utils/errorHandlers';
import { isTerminalRunStatus, useRunPolling } from '../hooks/useRunPolling';
import RunFilters from '../components/simulation-runs/RunFilters';
import RunActionModals from '../components/simulation-runs/RunActionModals';
import RunListContent from '../components/simulation-runs/RunListContent';
import './SimulationRuns.css';

const statusOptions = ['ALL', 'SUCCEEDED', 'FAILED', 'RUNNING', 'CREATED', 'CANCELLED'];
const activeStatuses = new Set(['RUNNING', 'CREATED']);

function SimulationRuns() {
  const [searchParams, setSearchParams] = useSearchParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [runs, setRuns] = useState([]);
  const [systems, setSystems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [runToDelete, setRunToDelete] = useState(null);
  const [bulkAction, setBulkAction] = useState(null);
  const [selectedRunIds, setSelectedRunIds] = useState([]);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isBulkProcessing, setIsBulkProcessing] = useState(false);
  const [actionError, setActionError] = useState(null);
  const [actionErrorTitle, setActionErrorTitle] = useState('Action failed');
  const [actionNotice, setActionNotice] = useState(null);
  const [now, setNow] = useState(() => Date.now());

  const [selectedSystemId, setSelectedSystemId] = useState(searchParams.get('systemId') || '');
  const [selectedStatus, setSelectedStatus] = useState(searchParams.get('status') || 'ALL');

  useEffect(() => {
    const loadSystems = async () => {
      try {
        const response = await liftSystemsApi.getAllSystems();
        setSystems(response.data);
      } catch (err) {
        handleApiError(err, setError, 'Failed to load lift systems');
      }
    };
    loadSystems();
  }, []);

  const loadRuns = useCallback(
    async (isPolling = false, page = currentPage) => {
      try {
        if (!isPolling) {
          setLoading(true);
        }
        const params = { page };
        if (selectedSystemId) {
          params.systemId = selectedSystemId;
        }
        if (selectedStatus && selectedStatus !== 'ALL') {
          params.status = selectedStatus;
        }
        const response = await simulationRunsApi.listRuns(params);
        const pageData = response.data;
        setRuns(pageData.content);
        setTotalPages(pageData.totalPages);
        setTotalElements(pageData.totalElements);
        setCurrentPage(pageData.number);
        setError(null);
      } catch (err) {
        handleApiError(err, setError, 'Failed to load simulation runs');
      } finally {
        if (!isPolling) {
          setLoading(false);
        }
      }
    },
    [selectedSystemId, selectedStatus, currentPage]
  );

  useEffect(() => {
    loadRuns(false);
  }, [loadRuns]);

  useEffect(() => {
    if (location.state?.notice) {
      setActionNotice(location.state.notice);
      navigate(location.pathname + location.search, { replace: true, state: null });
    }
  }, [location, navigate]);

  const hasActiveRuns = useMemo(() => runs.some((run) => activeStatuses.has(run.status)), [runs]);

  useRunPolling(useCallback(() => loadRuns(true), [loadRuns]), { intervalMs: 3000, enabled: hasActiveRuns });

  useRunPolling(() => setNow(Date.now()), { intervalMs: 1000, enabled: hasActiveRuns });

  useEffect(() => {
    setSelectedRunIds((currentIds) =>
      currentIds.filter((id) => runs.some((run) => run.id === id))
    );
  }, [runs]);

  const selectedRuns = useMemo(
    () => selectedRunIds.map((id) => runs.find((run) => run.id === id)).filter(Boolean),
    [selectedRunIds, runs]
  );
  const allVisibleSelected = runs.length > 0 && selectedRunIds.length === runs.length;
  const selectedCount = selectedRuns.length;
  const canBulkCancel = selectedCount > 0 && selectedRuns.every((run) => activeStatuses.has(run.status));
  const canBulkDelete = selectedCount > 0 && selectedRuns.every((run) => isTerminalRunStatus(run.status));

  useEffect(() => {
    const params = new URLSearchParams();
    if (selectedSystemId) params.set('systemId', selectedSystemId);
    if (selectedStatus && selectedStatus !== 'ALL') params.set('status', selectedStatus);
    setSearchParams(params, { replace: true });
  }, [selectedSystemId, selectedStatus, setSearchParams]);

  const handleSystemChange = (event) => {
    setCurrentPage(0);
    setSelectedSystemId(event.target.value);
  };

  const handleStatusChange = (event) => {
    setCurrentPage(0);
    setSelectedStatus(event.target.value);
  };

  const handleClearFilters = () => {
    setCurrentPage(0);
    setSelectedSystemId('');
    setSelectedStatus('ALL');
  };

  const handlePageChange = (newPage) => {
    loadRuns(false, newPage);
  };

  const handleToggleRunSelection = (runId) => {
    setSelectedRunIds((currentIds) => (
      currentIds.includes(runId) ? currentIds.filter((id) => id !== runId) : [...currentIds, runId]
    ));
  };

  const handleToggleAllVisible = () => {
    setSelectedRunIds(allVisibleSelected ? [] : runs.map((run) => run.id));
  };

  const handleRequestBulkAction = (type) => {
    setActionError(null);
    setActionNotice(null);
    setBulkAction(type);
  };

  const handleRequestDelete = (run) => {
    setActionError(null);
    setActionNotice(null);
    setRunToDelete(run);
  };

  const handleConfirmDelete = useCallback(async () => {
    if (!runToDelete) {
      return;
    }
    const runId = runToDelete.id;
    try {
      setIsDeleting(true);
      await simulationRunsApi.deleteRun(runId);
      setActionNotice(`Run #${runId} and its artefacts were deleted.`);
      setActionError(null);
      const emptiesCurrentPage = runs.length === 1 && currentPage > 0;
      const targetPage = emptiesCurrentPage ? currentPage - 1 : currentPage;
      await loadRuns(false, targetPage);
    } catch (err) {
      setActionErrorTitle('Failed to delete run');
      setActionError(getApiErrorMessage(err, `Failed to delete run #${runId}`));
      logApiError(err, `Failed to delete run #${runId}`);
    } finally {
      setIsDeleting(false);
      setRunToDelete(null);
    }
  }, [runToDelete, loadRuns, runs.length, currentPage]);

  const handleConfirmBulkAction = useCallback(async () => {
    if (!bulkAction || selectedRuns.length === 0) {
      return;
    }
    const runIds = selectedRuns.map((run) => run.id);
    const actionName = bulkAction === 'cancel' ? 'cancel' : 'delete';
    const actionPastTense = bulkAction === 'cancel' ? 'cancelled' : 'deleted';
    const apiAction = bulkAction === 'cancel'
      ? simulationRunsApi.cancelRun
      : simulationRunsApi.deleteRun;
    try {
      setIsBulkProcessing(true);
      const outcomes = await Promise.allSettled(runIds.map((id) => apiAction(id)));
      const successCount = outcomes.filter((outcome) => outcome.status === 'fulfilled').length;
      const failureCount = outcomes.length - successCount;
      setActionNotice(
        `${successCount} run${successCount === 1 ? '' : 's'} ${actionPastTense} successfully`
        + (failureCount > 0 ? `; ${failureCount} failed.` : '.')
      );
      if (failureCount > 0) {
        setActionErrorTitle(`Failed to ${actionName} selected runs`);
        setActionError(
          `Failed to ${actionName} ${failureCount} selected run${failureCount === 1 ? '' : 's'}. `
          + 'Refresh the list and try again if needed.'
        );
      } else {
        setActionError(null);
      }
      setSelectedRunIds([]);
      const emptiesCurrentPage = bulkAction === 'delete' && successCount >= runs.length && currentPage > 0;
      await loadRuns(false, emptiesCurrentPage ? currentPage - 1 : currentPage);
    } catch (err) {
      setActionErrorTitle(`Failed to ${actionName} selected runs`);
      setActionError(getApiErrorMessage(err, `Failed to ${actionName} selected runs`));
      logApiError(err, `Failed to ${actionName} selected runs`);
    } finally {
      setIsBulkProcessing(false);
      setBulkAction(null);
    }
  }, [bulkAction, selectedRuns, loadRuns, runs.length, currentPage]);

  const hasFilters = selectedSystemId || (selectedStatus && selectedStatus !== 'ALL');

  return (
    <div className="simulation-runs">
      <div className="page-header">
        <div className="page-title">
          <h2>Simulation Runs</h2>
          <p className="page-subtitle">View history of all simulation runs and access their results.</p>
        </div>
        <div className="page-actions">
          <Link to="/simulator" className="btn-primary">New Simulation</Link>
        </div>
      </div>

      <RunFilters
        systems={systems}
        statusOptions={statusOptions}
        selectedSystemId={selectedSystemId}
        selectedStatus={selectedStatus}
        hasFilters={hasFilters}
        onSystemChange={handleSystemChange}
        onStatusChange={handleStatusChange}
        onClearFilters={handleClearFilters}
      />

      {actionNotice && <p className="action-notice" role="status">{actionNotice}</p>}

      <RunListContent
        loading={loading}
        error={error}
        runs={runs}
        hasFilters={hasFilters}
        currentPage={currentPage}
        totalPages={totalPages}
        totalElements={totalElements}
        selectedRunIds={selectedRunIds}
        allVisibleSelected={allVisibleSelected}
        selectedCount={selectedCount}
        canBulkCancel={canBulkCancel}
        canBulkDelete={canBulkDelete}
        isDeleting={isDeleting}
        isBulkProcessing={isBulkProcessing}
        now={now}
        onClearFilters={handleClearFilters}
        onPageChange={handlePageChange}
        onToggleRunSelection={handleToggleRunSelection}
        onToggleAllVisible={handleToggleAllVisible}
        onRequestBulkAction={handleRequestBulkAction}
        onRequestDelete={handleRequestDelete}
      />

      <RunActionModals
        runToDelete={runToDelete}
        bulkAction={bulkAction}
        selectedCount={selectedCount}
        actionError={actionError}
        actionErrorTitle={actionErrorTitle}
        onCloseDelete={() => setRunToDelete(null)}
        onConfirmDelete={handleConfirmDelete}
        onCloseBulkAction={() => setBulkAction(null)}
        onConfirmBulkAction={handleConfirmBulkAction}
        onCloseError={() => setActionError(null)}
      />
    </div>
  );
}

export default SimulationRuns;
