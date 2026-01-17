import { useRef } from 'react';
import Modal from './Modal';
import './AlertModal.css';

/**
 * Modal component for displaying alert messages to users.
 * Supports different alert types (error, warning, success, info) with appropriate icons.
 *
 * @param {Object} props - Component props
 * @param {boolean} props.isOpen - Whether the modal is open
 * @param {Function} props.onClose - Callback function invoked when modal is closed
 * @param {string} [props.title='Error'] - Modal title displayed in the header
 * @param {string} props.message - Alert message content to display
 * @param {'error'|'warning'|'success'|'info'} [props.type='error'] - Alert type that determines styling and icon
 * @returns {JSX.Element|null} The alert modal component or null if not open
 */
function AlertModal({ isOpen, onClose, title = 'Error', message, type = 'error' }) {
  const okButtonRef = useRef(null);

  if (!isOpen) return null;

  /**
   * Gets the appropriate icon character based on alert type.
   *
   * @returns {string} Unicode character representing the alert type icon
   */
  const getIcon = () => {
    switch (type) {
      case 'error':
        return '✗';
      case 'warning':
        return '⚠';
      case 'success':
        return '✓';
      case 'info':
        return 'ℹ';
      default:
        return '✗';
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={title}
      role="alertdialog"
      className="alert-modal"
      initialFocusRef={okButtonRef}
      onEnter={onClose}
      trapFocus={false}
    >
      <div className={`alert-modal-body alert-${type}`}>
        <div className="alert-icon">{getIcon()}</div>
        <p>{message}</p>
      </div>

      <div className="modal-actions">
        <button
          ref={okButtonRef}
          onClick={onClose}
          className="btn-primary"
        >
          OK
        </button>
      </div>
    </Modal>
  );
}

export default AlertModal;
