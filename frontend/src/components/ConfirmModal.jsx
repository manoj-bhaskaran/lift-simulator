import { useRef, useState } from 'react';
import Modal from './Modal';
import './ConfirmModal.css';

/**
 * Modal component for displaying confirmation dialogs with customizable buttons.
 * Used to confirm destructive or important actions before proceeding.
 *
 * @param {Object} props - Component props
 * @param {boolean} props.isOpen - Whether the modal is open
 * @param {Function} props.onClose - Callback function invoked when modal is closed or cancelled
 * @param {Function} props.onConfirm - Callback function invoked when user confirms the action
 * @param {string} props.title - Modal title displayed in the header
 * @param {string} props.message - Confirmation message content to display
 * @param {string} [props.confirmText='Confirm'] - Text for the confirm button
 * @param {string} [props.cancelText='Cancel'] - Text for the cancel button
 * @param {'primary'|'secondary'|'success'|'danger'} [props.confirmStyle='primary'] - Style variant for the confirm button
 * @returns {JSX.Element|null} The confirmation modal component or null if not open
 */
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
  const [confirming, setConfirming] = useState(false);

  /**
   * Handles the confirm action by waiting for the confirm callback to finish
   * before closing the modal.
   */
  const handleConfirm = async () => {
    setConfirming(true);
    try {
      await onConfirm();
      onClose();
    } finally {
      setConfirming(false);
    }
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
        <button onClick={onClose} className="btn-secondary" disabled={confirming}>
          {cancelText}
        </button>
        <button
          ref={confirmButtonRef}
          onClick={handleConfirm}
          className={`btn-${confirmStyle}`}
          disabled={confirming}
        >
          {confirming ? `${confirmText}...` : confirmText}
        </button>
      </div>
    </Modal>
  );
}

export default ConfirmModal;
