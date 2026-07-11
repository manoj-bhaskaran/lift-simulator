import { Link } from 'react-router-dom';
import {
  formatBytes,
  formatKpiValue,
  formatNumber,
  getPickupLegUtilisation,
  kpiLabels,
} from '../../utils/simulationResultsUtils';

function SimulationResultsPanel({
  cardClassName = 'detail-card',
  runInfo,
  runStatus,
  results,
  artefacts,
  resultsError,
  artefactDownloadUrl,
  onArtefactDownload,
  showFullDetailsLink = false,
  showMissingInputMessage = false,
  onlyShowReproduceOnSuccess = true,
}) {
  const runSummary = results?.results?.runSummary;
  const kpis = results?.results?.kpis;
  const perLift = results?.results?.perLift || [];
  const perFloor = results?.results?.perFloor || [];
  const inputArtefact = artefacts.find((item) => item.name === 'input.scenario');
  const showReproduce = inputArtefact && (!onlyShowReproduceOnSuccess || runStatus === 'SUCCEEDED');

  return (
    <section className={cardClassName}>
      <div className="results-header">
        <h3>Results</h3>
        {showFullDetailsLink && (
          <Link to={`/simulation-runs/${runInfo.id}`} className="btn-secondary btn-small">
            View Full Details
          </Link>
        )}
      </div>
      {resultsError && <p className="error">{resultsError}</p>}

      {runStatus === 'FAILED' && (
        <div className="result-banner error">
          <strong>Simulation failed.</strong>
          <p>{results?.errorMessage || runInfo.errorMessage || 'Unknown error.'}</p>
        </div>
      )}

      {runStatus === 'CANCELLED' && (
        <div className="result-banner warning">
          <strong>Simulation was cancelled.</strong>
        </div>
      )}

      {runStatus === 'SUCCEEDED' && results?.results && (
        <>
          <div className="result-banner success">
            <strong>Simulation completed successfully.</strong>
            {results.errorMessage && <p>{results.errorMessage}</p>}
          </div>
          <div className="kpi-grid">
            {kpis && Object.entries(kpis).map(([key, value]) => (
              <div key={key} className="kpi-card">
                <span>{kpiLabels[key] || key}</span>
                <strong>{formatKpiValue(key, value)}</strong>
              </div>
            ))}
          </div>
          <div className="results-section">
            <h4>Run Summary</h4>
            <div className="summary-grid">
              <div><span className="label">Generated</span><p>{runSummary?.generatedAt ? new Date(runSummary.generatedAt).toLocaleString() : '—'}</p></div>
              <div><span className="label">Ticks</span><p>{runSummary?.ticks ?? '—'}</p></div>
              <div><span className="label">Duration</span><p>{runSummary?.durationTicks ?? '—'} ticks</p></div>
              <div><span className="label">Seed</span><p>{runSummary?.seed ?? runInfo.seed ?? '—'}</p></div>
            </div>
          </div>
          <div className="results-section">
            <h4>Per Lift</h4>
            {perLift.length === 0 ? <p>No lift metrics available.</p> : (
              <div className="table-wrapper"><table><thead><tr><th>Lift</th><th>Controller</th><th>Parking</th><th>Pickup-leg Utilisation</th><th>Idle</th><th>Moving</th><th>Door</th><th>Status Counts</th></tr></thead><tbody>
                {perLift.map((lift) => <tr key={lift.liftId}><td>{lift.liftId}</td><td>{lift.controllerStrategy || '—'}</td><td>{lift.idleParkingMode || '—'}</td><td>{formatKpiValue('pickupLegUtilisation', getPickupLegUtilisation(lift))}</td><td>{formatNumber(lift.idleTicks)}</td><td>{formatNumber(lift.movingTicks)}</td><td>{formatNumber(lift.doorTicks)}</td><td>{lift.statusCounts ? Object.entries(lift.statusCounts).map(([status, count]) => `${status}: ${count}`).join(', ') : '—'}</td></tr>)}
              </tbody></table></div>
            )}
          </div>
          <div className="results-section">
            <h4>Per Floor</h4>
            {perFloor.length === 0 ? <p>No floor metrics available.</p> : (
              <div className="table-wrapper"><table><thead><tr><th>Floor</th><th>Origin Passengers</th><th>Destination Passengers</th><th>Lift Visits</th></tr></thead><tbody>
                {perFloor.map((floor) => <tr key={floor.floor}><td>{floor.floor}</td><td>{formatNumber(floor.originPassengers)}</td><td>{formatNumber(floor.destinationPassengers)}</td><td>{formatNumber(floor.liftVisits)}</td></tr>)}
              </tbody></table></div>
            )}
          </div>
        </>
      )}

      {runStatus === 'SUCCEEDED' && !results?.results && (
        <div className="result-banner warning">
          <strong>Simulation completed, but results were unavailable.</strong>
          <p>{results?.errorMessage || 'Results file could not be read.'}</p>
        </div>
      )}

      <div className="results-section">
        <h4>Artefacts</h4>
        {artefacts.length === 0 ? <p>No artefacts available.</p> : (
          <div className="table-wrapper"><table><thead><tr><th>Name</th><th>Size</th><th>Type</th><th>Download</th></tr></thead><tbody>
            {artefacts.map((artefact) => <tr key={`${artefact.name}-${artefact.path}`}><td>{artefact.name}</td><td>{formatBytes(artefact.size)}</td><td>{artefact.mimeType}</td><td><a className="link" href={artefactDownloadUrl(artefact.path)} onClick={(event) => onArtefactDownload(event, artefact)}>Download</a></td></tr>)}
          </tbody></table></div>
        )}
      </div>

      {(showReproduce || showMissingInputMessage) && (
        <div className="results-section reproduce-section">
          <h4>Reproduce via CLI</h4>
          <p>Use the generated batch input file to reproduce this run with the CLI simulator.</p>
          {inputArtefact ? (
            <div className="reproduce-actions">
              <a className="btn-secondary" href={artefactDownloadUrl(inputArtefact.path)} onClick={(event) => onArtefactDownload(event, inputArtefact)}>Download input.scenario</a>
              <code>lift-simulator --input {inputArtefact.path}</code>
            </div>
          ) : <p className="warning">Input file not available for this run.</p>}
        </div>
      )}
    </section>
  );
}

export default SimulationResultsPanel;
