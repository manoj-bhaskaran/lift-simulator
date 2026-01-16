import { useRef } from 'react';
import Modal from './Modal';
import './AlertModal.css';

function AlertModal({ isOpen, onClose, title = 'Error', message, type = 'error' }) {
  const okButtonRef = useRef(null);

  if (!isOpen) return null;

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
