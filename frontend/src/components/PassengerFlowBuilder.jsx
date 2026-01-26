// @ts-check
import { useState } from 'react';
import './PassengerFlowBuilder.css';

/**
 * @typedef {Object} PassengerFlow
 * @property {number} startTick - When passengers board (0+)
 * @property {number} originFloor - Boarding floor
 * @property {number} destinationFloor - Alighting floor
 * @property {number} passengers - Number of passengers (1+)
 */

/**
 * Component for building and managing passenger flows in a scenario.
 * Allows adding, editing, and removing individual passenger flow entries.
 *
 * @param {Object} props - Component props
 * @param {PassengerFlow[]} props.flows - Current passenger flows
 * @param {Function} props.onChange - Callback when flows change
 * @param {number} props.maxTick - Maximum tick value (from scenario duration)
 * @param {Object} props.floorRange - Floor range constraints {minFloor, maxFloor}
 * @returns {JSX.Element} The passenger flow builder component
 */
function PassengerFlowBuilder({ flows, onChange, maxTick, floorRange }) {
  const [editingIndex, setEditingIndex] = useState(null);
  const [newFlow, setNewFlow] = useState({
    startTick: 0,
    originFloor: 0,
    destinationFloor: 1,
    passengers: 1
  });
  const [showAddForm, setShowAddForm] = useState(false);

  /**
   * Handles adding a new flow.
   *
   * @param {PassengerFlow} flowData - The flow data from the form
   */
  const handleAddFlow = (flowData) => {
    const flow = {
      startTick: parseInt(flowData.startTick, 10),
      originFloor: parseInt(flowData.originFloor, 10),
      destinationFloor: parseInt(flowData.destinationFloor, 10),
      passengers: parseInt(flowData.passengers, 10)
    };

    onChange([...flows, flow]);
    setNewFlow({
      startTick: 0,
      originFloor: 0,
      destinationFloor: 1,
      passengers: 1
    });
    setShowAddForm(false);
  };

  /**
   * Handles updating an existing flow.
   *
   * @param {number} index - Index of the flow to update
   * @param {PassengerFlow} updatedFlow - Updated flow data
   */
  const handleUpdateFlow = (index, updatedFlow) => {
    const newFlows = [...flows];
    newFlows[index] = {
      startTick: parseInt(updatedFlow.startTick, 10),
      originFloor: parseInt(updatedFlow.originFloor, 10),
      destinationFloor: parseInt(updatedFlow.destinationFloor, 10),
      passengers: parseInt(updatedFlow.passengers, 10)
    };
    onChange(newFlows);
    setEditingIndex(null);
  };

  /**
   * Handles removing a flow.
   *
   * @param {number} index - Index of the flow to remove
   */
  const handleRemoveFlow = (index) => {
    const newFlows = flows.filter((_, i) => i !== index);
    onChange(newFlows);
  };

  /**
   * Handles moving a flow up in the list.
   *
   * @param {number} index - Index of the flow to move
   */
  const handleMoveUp = (index) => {
    if (index === 0) return;
    const newFlows = [...flows];
    [newFlows[index - 1], newFlows[index]] = [newFlows[index], newFlows[index - 1]];
    onChange(newFlows);
  };

  /**
   * Handles moving a flow down in the list.
   *
   * @param {number} index - Index of the flow to move
   */
  const handleMoveDown = (index) => {
    if (index === flows.length - 1) return;
    const newFlows = [...flows];
    [newFlows[index], newFlows[index + 1]] = [newFlows[index + 1], newFlows[index]];
    onChange(newFlows);
  };

  return (
    <div className="passenger-flow-builder">
      {flows.length === 0 ? (
        <div className="empty-flows">
          <p>No passenger flows defined yet. Add your first flow to get started.</p>
        </div>
      ) : (
        <div className="flows-list">
          {flows.map((flow, index) => (
            <div key={index} className="flow-item">
              {editingIndex === index ? (
                <FlowEditForm
                  flow={flow}
                  maxTick={maxTick}
                  floorRange={floorRange}
                  onSave={(updatedFlow) => handleUpdateFlow(index, updatedFlow)}
                  onCancel={() => setEditingIndex(null)}
                />
              ) : (
                <FlowDisplayItem
                  flow={flow}
                  index={index}
                  onEdit={() => setEditingIndex(index)}
                  onRemove={() => handleRemoveFlow(index)}
                  onMoveUp={() => handleMoveUp(index)}
                  onMoveDown={() => handleMoveDown(index)}
                  isFirst={index === 0}
                  isLast={index === flows.length - 1}
                />
              )}
            </div>
          ))}
        </div>
      )}

      {showAddForm ? (
        <div className="add-flow-form">
          <h4>Add New Passenger Flow</h4>
          <FlowEditForm
            flow={newFlow}
            maxTick={maxTick}
            floorRange={floorRange}
            onSave={handleAddFlow}
            onCancel={() => setShowAddForm(false)}
          />
        </div>
      ) : (
        <button
          type="button"
          className="btn-add-flow"
          onClick={() => setShowAddForm(true)}
        >
          + Add Passenger Flow
        </button>
      )}
    </div>
  );
}

/**
 * Component for displaying a passenger flow item.
 *
 * @param {Object} props - Component props
 * @param {PassengerFlow} props.flow - The flow to display
 * @param {number} props.index - Index of the flow
 * @param {Function} props.onEdit - Edit callback
 * @param {Function} props.onRemove - Remove callback
 * @param {Function} props.onMoveUp - Move up callback
 * @param {Function} props.onMoveDown - Move down callback
 * @param {boolean} props.isFirst - Whether this is the first item
 * @param {boolean} props.isLast - Whether this is the last item
 * @returns {JSX.Element} The flow display component
 */
function FlowDisplayItem({ flow, index, onEdit, onRemove, onMoveUp, onMoveDown, isFirst, isLast }) {
  return (
    <div className="flow-display">
      <div className="flow-index">#{index + 1}</div>
      <div className="flow-details">
        <div className="flow-detail-row">
          <span className="detail-label">Tick:</span>
          <span className="detail-value">{flow.startTick}</span>
        </div>
        <div className="flow-detail-row">
          <span className="detail-label">From Floor:</span>
          <span className="detail-value">{flow.originFloor}</span>
        </div>
        <div className="flow-detail-row">
          <span className="detail-label">To Floor:</span>
          <span className="detail-value">{flow.destinationFloor}</span>
        </div>
        <div className="flow-detail-row">
          <span className="detail-label">Passengers:</span>
          <span className="detail-value">{flow.passengers}</span>
        </div>
      </div>
      <div className="flow-actions">
        <button type="button" onClick={onEdit} className="btn-icon" title="Edit">
          ✎
        </button>
        <button type="button" onClick={onRemove} className="btn-icon btn-danger-icon" title="Remove">
          ✕
        </button>
        <button
          type="button"
          onClick={onMoveUp}
          className="btn-icon"
          disabled={isFirst}
          title="Move Up"
        >
          ▲
        </button>
        <button
          type="button"
          onClick={onMoveDown}
          className="btn-icon"
          disabled={isLast}
          title="Move Down"
        >
          ▼
        </button>
      </div>
    </div>
  );
}

/**
 * Component for editing a passenger flow.
 *
 * @param {Object} props - Component props
 * @param {PassengerFlow} props.flow - The flow to edit
 * @param {number} props.maxTick - Maximum tick value
 * @param {Object} props.floorRange - Floor range constraints {minFloor, maxFloor}
 * @param {Function} props.onSave - Save callback
 * @param {Function} props.onCancel - Cancel callback
 * @returns {JSX.Element} The flow edit component
 */
function FlowEditForm({ flow, maxTick, floorRange, onSave, onCancel }) {
  const [formData, setFormData] = useState({ ...flow });
  const [errors, setErrors] = useState({});

  const handleChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    // Clear error for this field when user modifies it
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: '' }));
    }
  };

  /**
   * Validates form data before saving.
   *
   * @returns {boolean} True if valid, false otherwise
   */
  const validateForm = () => {
    const newErrors = {};

    // Validate startTick
    const startTick = parseInt(formData.startTick, 10);
    if (isNaN(startTick) || startTick < 0) {
      newErrors.startTick = 'Start tick must be a non-negative number';
    } else if (startTick >= maxTick) {
      newErrors.startTick = `Start tick must be less than ${maxTick}`;
    }

    // Validate originFloor
    const originFloor = parseInt(formData.originFloor, 10);
    if (isNaN(originFloor)) {
      newErrors.originFloor = 'Origin floor is required';
    } else if (floorRange && (originFloor < floorRange.minFloor || originFloor > floorRange.maxFloor)) {
      newErrors.originFloor = `Origin floor must be between ${floorRange.minFloor} and ${floorRange.maxFloor}`;
    }

    // Validate destinationFloor
    const destinationFloor = parseInt(formData.destinationFloor, 10);
    if (isNaN(destinationFloor)) {
      newErrors.destinationFloor = 'Destination floor is required';
    } else if (floorRange && (destinationFloor < floorRange.minFloor || destinationFloor > floorRange.maxFloor)) {
      newErrors.destinationFloor = `Destination floor must be between ${floorRange.minFloor} and ${floorRange.maxFloor}`;
    }

    // Validate passengers
    const passengers = parseInt(formData.passengers, 10);
    if (isNaN(passengers) || passengers < 1) {
      newErrors.passengers = 'At least 1 passenger is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSave = () => {
    if (validateForm()) {
      onSave(formData);
    }
  };

  return (
    <div className="flow-edit-form">
      <div className="flow-edit-grid">
        <div className="flow-edit-field">
          <label htmlFor="startTick">Start Tick</label>
          <input
            type="number"
            id="startTick"
            value={formData.startTick}
            onChange={(e) => handleChange('startTick', e.target.value)}
            min="0"
            max={maxTick - 1}
            className={errors.startTick ? 'error' : ''}
          />
          {errors.startTick ? (
            <span className="field-error">{errors.startTick}</span>
          ) : (
            <span className="field-hint">0 to {maxTick - 1}</span>
          )}
        </div>

        <div className="flow-edit-field">
          <label htmlFor="originFloor">Origin Floor</label>
          <input
            type="number"
            id="originFloor"
            value={formData.originFloor}
            onChange={(e) => handleChange('originFloor', e.target.value)}
            min={floorRange?.minFloor}
            max={floorRange?.maxFloor}
            className={errors.originFloor ? 'error' : ''}
          />
          {errors.originFloor ? (
            <span className="field-error">{errors.originFloor}</span>
          ) : (
            <span className="field-hint">
              {floorRange ? `${floorRange.minFloor} to ${floorRange.maxFloor}` : 'Floor number'}
            </span>
          )}
        </div>

        <div className="flow-edit-field">
          <label htmlFor="destinationFloor">Destination Floor</label>
          <input
            type="number"
            id="destinationFloor"
            value={formData.destinationFloor}
            onChange={(e) => handleChange('destinationFloor', e.target.value)}
            min={floorRange?.minFloor}
            max={floorRange?.maxFloor}
            className={errors.destinationFloor ? 'error' : ''}
          />
          {errors.destinationFloor ? (
            <span className="field-error">{errors.destinationFloor}</span>
          ) : (
            <span className="field-hint">
              {floorRange ? `${floorRange.minFloor} to ${floorRange.maxFloor}` : 'Floor number'}
            </span>
          )}
        </div>

        <div className="flow-edit-field">
          <label htmlFor="passengers">Passengers</label>
          <input
            type="number"
            id="passengers"
            value={formData.passengers}
            onChange={(e) => handleChange('passengers', e.target.value)}
            min="1"
            className={errors.passengers ? 'error' : ''}
          />
          {errors.passengers ? (
            <span className="field-error">{errors.passengers}</span>
          ) : (
            <span className="field-hint">At least 1</span>
          )}
        </div>
      </div>

      <div className="flow-edit-actions">
        <button type="button" onClick={onCancel} className="btn-secondary">
          Cancel
        </button>
        <button type="button" onClick={handleSave} className="btn-primary">
          Save
        </button>
      </div>
    </div>
  );
}

export default PassengerFlowBuilder;
