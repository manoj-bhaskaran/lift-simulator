import { useState, useEffect } from 'react';
import './Modal.css';
import './CreateSystemModal.css';

/**
 * @typedef {Object} LiftSystemFormData
 * @property {string} displayName - Human-readable name for the lift system
 * @property {string} description - Optional description of the lift system
 */

/**
 * Modal component for editing an existing lift system's name and description.
 * Includes validation for required fields and character limits.
 * Note: System key cannot be edited after creation.
 *
 * @param {Object} props - Component props
 * @param {boolean} props.isOpen - Whether the modal is open
 * @param {Function} props.onClose - Callback function invoked when modal is closed
 * @param {Function} props.onSubmit - Async callback function invoked with form data when form is submitted
 * @param {Object} props.system - The lift system to edit
 * @param {string} props.system.displayName - Current display name
 * @param {string} props.system.description - Current description
 * @returns {JSX.Element|null} The edit system modal component or null if not open
 */
function EditSystemModal({ isOpen, onClose, onSubmit, system }) {
  const [formData, setFormData] = useState({
    displayName: '',
    description: ''
  });
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  // Initialize form data when system prop changes
  useEffect(() => {
    if (system) {
      setFormData({
        displayName: system.displayName || '',
        description: system.description || ''
      });
    }
  }, [system]);

  /**
   * Validates the form data according to business rules.
   * Checks for required fields and length limits.
   *
   * @returns {boolean} True if form is valid, false otherwise
   */
  const validateForm = () => {
    const newErrors = {};

    if (!formData.displayName.trim()) {
      newErrors.displayName = 'Display name is required';
    } else if (formData.displayName.length > 200) {
      newErrors.displayName = 'Display name must be 200 characters or less';
    }

    if (formData.description.length > 5000) {
      newErrors.description = 'Description must be 5000 characters or less';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  /**
   * Handles form submission with validation and error handling.
   *
   * @param {React.FormEvent} e - Form submission event
   */
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setSubmitting(true);
    try {
      await onSubmit(formData);
      setErrors({});
    } catch {
      // Error is handled by parent component
    } finally {
      setSubmitting(false);
    }
  };

  /**
   * Handles input field changes and clears field-specific errors.
   *
   * @param {React.ChangeEvent<HTMLInputElement|HTMLTextAreaElement>} e - Input change event
   */
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    // Clear error for this field when user starts typing
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  /**
   * Handles modal close by resetting errors and calling onClose callback.
   */
  const handleClose = () => {
    setErrors({});
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={handleClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Edit Lift System</h2>
          <button className="close-button" onClick={handleClose}>&times;</button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="systemKey">System Key</label>
            <input
              type="text"
              id="systemKey"
              name="systemKey"
              value={system?.systemKey || ''}
              disabled
              className="disabled-field"
            />
            <p className="help-text">System key cannot be changed after creation</p>
          </div>

          <div className="form-group">
            <label htmlFor="displayName">
              Display Name <span className="required">*</span>
            </label>
            <input
              type="text"
              id="displayName"
              name="displayName"
              value={formData.displayName}
              onChange={handleChange}
              placeholder="e.g., Office Building Main Lift System"
              className={errors.displayName ? 'error' : ''}
            />
            {errors.displayName && <span className="error-message">{errors.displayName}</span>}
            <p className="help-text">Human-readable name for the system</p>
          </div>

          <div className="form-group">
            <label htmlFor="description">Description</label>
            <textarea
              id="description"
              name="description"
              value={formData.description}
              onChange={handleChange}
              placeholder="Optional description of the lift system"
              rows="4"
              className={errors.description ? 'error' : ''}
            />
            {errors.description && <span className="error-message">{errors.description}</span>}
          </div>

          <div className="modal-actions">
            <button type="button" onClick={handleClose} className="btn-secondary">
              Cancel
            </button>
            <button type="submit" className="btn-primary" disabled={submitting}>
              {submitting ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default EditSystemModal;
