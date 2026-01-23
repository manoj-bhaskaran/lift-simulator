// @ts-check
import { useState } from 'react';
import './ScenarioEvents.css';

/**
 * Component for managing scenario events (hall calls, car calls, cancellations).
 * Provides UI to add, edit, delete, and reorder passenger flow events.
 *
 * @param {Object} props
 * @param {import('../types/models').ScenarioEvent[]} props.events - Current list of events
 * @param {number} props.totalTicks - Maximum tick value for validation
 * @param {number} props.minFloor - Minimum floor number
 * @param {number} props.maxFloor - Maximum floor number
 * @param {Function} props.onChange - Callback when events change
 * @returns {JSX.Element}
 */
function ScenarioEvents({ events = [], totalTicks, minFloor, maxFloor, onChange }) {
  const [editingIndex, setEditingIndex] = useState(null);
  const [newEvent, setNewEvent] = useState({
    tick: 0,
    eventType: 'HALL_CALL',
    description: '',
    originFloor: minFloor,
    destinationFloor: minFloor,
    direction: 'UP',
  });

  /**
   * Adds a new event to the list.
   */
  const handleAddEvent = () => {
    const event = {
      ...newEvent,
      tick: parseInt(newEvent.tick, 10),
      originFloor: newEvent.originFloor !== '' ? parseInt(newEvent.originFloor, 10) : null,
      destinationFloor: newEvent.destinationFloor !== '' ? parseInt(newEvent.destinationFloor, 10) : null,
    };

    // Basic validation
    if (event.eventType === 'HALL_CALL' && (event.originFloor === null || !event.direction)) {
      alert('Hall calls require origin floor and direction');
      return;
    }
    if (event.eventType === 'CAR_CALL' && event.destinationFloor === null) {
      alert('Car calls require destination floor');
      return;
    }

    onChange([...events, event].sort((a, b) => a.tick - b.tick));

    // Reset form
    setNewEvent({
      tick: 0,
      eventType: 'HALL_CALL',
      description: '',
      originFloor: minFloor,
      destinationFloor: minFloor,
      direction: 'UP',
    });
  };

  /**
   * Deletes an event at the given index.
   */
  const handleDeleteEvent = (index) => {
    onChange(events.filter((_, i) => i !== index));
  };

  /**
   * Updates the new event form field.
   */
  const handleNewEventChange = (field, value) => {
    setNewEvent((prev) => ({ ...prev, [field]: value }));
  };

  /**
   * Gets a human-readable description of an event.
   */
  const getEventDescription = (event) => {
    if (event.description) {
      return event.description;
    }

    switch (event.eventType) {
      case 'HALL_CALL':
        return `Hall call at floor ${event.originFloor}, direction ${event.direction}`;
      case 'CAR_CALL':
        return `Car call to floor ${event.destinationFloor}`;
      case 'CANCEL':
        return 'Cancel request';
      default:
        return 'Unknown event';
    }
  };

  return (
    <div className="scenario-events">
      <div className="events-list">
        {events.length === 0 ? (
          <p className="empty-events">
            No events added yet. Add passenger flow events below.
          </p>
        ) : (
          <table className="events-table">
            <thead>
              <tr>
                <th>Tick</th>
                <th>Type</th>
                <th>Description</th>
                <th>Details</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {events.map((event, index) => (
                <tr key={index}>
                  <td className="tick-cell">{event.tick}</td>
                  <td className="type-cell">
                    <span className={`event-type-badge ${event.eventType.toLowerCase()}`}>
                      {event.eventType.replace('_', ' ')}
                    </span>
                  </td>
                  <td>{getEventDescription(event)}</td>
                  <td className="details-cell">
                    {event.eventType === 'HALL_CALL' && (
                      <span>Floor: {event.originFloor}, Dir: {event.direction}</span>
                    )}
                    {event.eventType === 'CAR_CALL' && (
                      <span>To: {event.destinationFloor}</span>
                    )}
                  </td>
                  <td className="actions-cell">
                    <button
                      className="btn-danger-small"
                      onClick={() => handleDeleteEvent(index)}
                      title="Delete event"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="add-event-form">
        <h4>Add New Event</h4>
        <div className="event-form-row">
          <div className="event-form-field">
            <label htmlFor="event-tick">Tick *</label>
            <input
              type="number"
              id="event-tick"
              value={newEvent.tick}
              onChange={(e) => handleNewEventChange('tick', e.target.value)}
              min="0"
              max={totalTicks - 1}
            />
          </div>
          <div className="event-form-field">
            <label htmlFor="event-type">Event Type *</label>
            <select
              id="event-type"
              value={newEvent.eventType}
              onChange={(e) => handleNewEventChange('eventType', e.target.value)}
            >
              <option value="HALL_CALL">Hall Call</option>
              <option value="CAR_CALL">Car Call</option>
              <option value="CANCEL">Cancel</option>
            </select>
          </div>
        </div>

        {newEvent.eventType === 'HALL_CALL' && (
          <div className="event-form-row">
            <div className="event-form-field">
              <label htmlFor="event-origin">Origin Floor *</label>
              <input
                type="number"
                id="event-origin"
                value={newEvent.originFloor}
                onChange={(e) => handleNewEventChange('originFloor', e.target.value)}
                min={minFloor}
                max={maxFloor}
              />
            </div>
            <div className="event-form-field">
              <label htmlFor="event-direction">Direction *</label>
              <select
                id="event-direction"
                value={newEvent.direction}
                onChange={(e) => handleNewEventChange('direction', e.target.value)}
              >
                <option value="UP">Up</option>
                <option value="DOWN">Down</option>
              </select>
            </div>
          </div>
        )}

        {newEvent.eventType === 'CAR_CALL' && (
          <div className="event-form-row">
            <div className="event-form-field">
              <label htmlFor="event-destination">Destination Floor *</label>
              <input
                type="number"
                id="event-destination"
                value={newEvent.destinationFloor}
                onChange={(e) => handleNewEventChange('destinationFloor', e.target.value)}
                min={minFloor}
                max={maxFloor}
              />
            </div>
          </div>
        )}

        <div className="event-form-row">
          <div className="event-form-field full-width">
            <label htmlFor="event-description">Description (Optional)</label>
            <input
              type="text"
              id="event-description"
              value={newEvent.description}
              onChange={(e) => handleNewEventChange('description', e.target.value)}
              placeholder="e.g., Person A boards at ground floor"
            />
          </div>
        </div>

        <button className="btn-primary" onClick={handleAddEvent}>
          Add Event
        </button>
      </div>
    </div>
  );
}

export default ScenarioEvents;
