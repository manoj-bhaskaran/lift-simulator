import { useState } from 'react';
import './CreateSystemModal.css';

function CreateSystemModal({ isOpen, onClose, onSubmit }) {
  const [formData, setFormData] = useState({
    systemKey: '',
    displayName: '',
    description: ''
  });
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const validateForm = () => {
    const newErrors = {};

    if (!formData.systemKey.trim()) {
      newErrors.systemKey = 'System key is required';
    } else if (!/^[a-zA-Z0-9_-]+$/.test(formData.systemKey)) {
      newErrors.systemKey = 'System key can only contain letters, numbers, hyphens, and underscores';
    } else if (formData.systemKey.length > 120) {
      newErrors.systemKey = 'System key must be 120 characters or less';
    }

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

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setSubmitting(true);
    try {
      await onSubmit(formData);
      setFormData({ systemKey: '', displayName: '', description: '' });
      setErrors({});
    } catch (err) {
      // Error is handled by parent component
    } finally {
      setSubmitting(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    // Clear error for this field when user starts typing
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const handleClose = () => {
    setFormData({ systemKey: '', displayName: '', description: '' });
    setErrors({});
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={handleClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Create New Lift System</h2>
          <button className="close-button" onClick={handleClose}>&times;</button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="systemKey">
              System Key <span className="required">*</span>
            </label>
            <input
              type="text"
              id="systemKey"
              name="systemKey"
              value={formData.systemKey}
              onChange={handleChange}
              placeholder="e.g., office-building-main"
              className={errors.systemKey ? 'error' : ''}
            />
            {errors.systemKey && <span className="error-message">{errors.systemKey}</span>}
            <p className="help-text">Unique identifier using letters, numbers, hyphens, and underscores</p>
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
              {submitting ? 'Creating...' : 'Create System'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default CreateSystemModal;
