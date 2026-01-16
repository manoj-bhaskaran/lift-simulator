import { useEffect, useRef } from 'react';
import './AlertModal.css';

function AlertModal({ isOpen, onClose, title = 'Error', message, type = 'error' }) {
  const okButtonRef = useRef(null);
  const modalRef = useRef(null);

  useEffect(() => {
    if (isOpen && okButtonRef.current) {
      // Focus the OK button when modal opens
      okButtonRef.current.focus();
    }
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape' || e.key === 'Enter') {
        onClose();
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

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
    <div className="modal-overlay" onClick={onClose} role="alertdialog" aria-modal="true" aria-labelledby="alert-modal-title">
      <div className="modal-content alert-modal" onClick={(e) => e.stopPropagation()} ref={modalRef}>
        <div className="modal-header">
          <h2 id="alert-modal-title">{title}</h2>
          <button className="close-button" onClick={onClose} aria-label="Close">&times;</button>
        </div>

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
      </div>
    </div>
  );
}

export default AlertModal;
