import { useEffect, useRef } from 'react';
import './ConfirmModal.css';

function ConfirmModal({ isOpen, onClose, onConfirm, title, message, confirmText = 'Confirm', cancelText = 'Cancel', confirmStyle = 'primary' }) {
  const confirmButtonRef = useRef(null);
  const modalRef = useRef(null);

  useEffect(() => {
    if (isOpen && confirmButtonRef.current) {
      // Focus the confirm button when modal opens
      confirmButtonRef.current.focus();
    }
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    const handleTabKey = (e) => {
      if (e.key === 'Tab') {
        const modal = modalRef.current;
        if (!modal) return;

        const focusableElements = modal.querySelectorAll(
          'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        );
        const firstElement = focusableElements[0];
        const lastElement = focusableElements[focusableElements.length - 1];

        if (e.shiftKey) {
          if (document.activeElement === firstElement) {
            e.preventDefault();
            lastElement.focus();
          }
        } else {
          if (document.activeElement === lastElement) {
            e.preventDefault();
            firstElement.focus();
          }
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    document.addEventListener('keydown', handleTabKey);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.removeEventListener('keydown', handleTabKey);
    };
  }, [isOpen, onClose]);

  const handleConfirm = () => {
    onConfirm();
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose} role="dialog" aria-modal="true" aria-labelledby="confirm-modal-title">
      <div className="modal-content confirm-modal" onClick={(e) => e.stopPropagation()} ref={modalRef}>
        <div className="modal-header">
          <h2 id="confirm-modal-title">{title}</h2>
          <button className="close-button" onClick={onClose} aria-label="Close">&times;</button>
        </div>

        <div className="confirm-modal-body">
          <p>{message}</p>
        </div>

        <div className="modal-actions">
          <button onClick={onClose} className="btn-secondary">
            {cancelText}
          </button>
          <button
            ref={confirmButtonRef}
            onClick={handleConfirm}
            className={`btn-${confirmStyle}`}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}

export default ConfirmModal;
