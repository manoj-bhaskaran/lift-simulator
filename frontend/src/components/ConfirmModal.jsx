import { useRef } from 'react';
import Modal from './Modal';
import './ConfirmModal.css';

function ConfirmModal({
  isOpen,
  onClose,
  onConfirm,
  title,
  message,
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  confirmStyle = 'primary'
}) {
  const confirmButtonRef = useRef(null);

  const handleConfirm = () => {
    onConfirm();
    onClose();
  };

  if (!isOpen) return null;

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={title}
      role="dialog"
      className="confirm-modal"
      initialFocusRef={confirmButtonRef}
    >
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
    </Modal>
  );
}

export default ConfirmModal;
