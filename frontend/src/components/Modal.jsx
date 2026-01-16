import { useEffect, useId, useRef } from 'react';
import './Modal.css';

const FOCUSABLE_SELECTOR =
  'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';

function Modal({
  isOpen,
  onClose,
  title,
  role = 'dialog',
  className = '',
  children,
  initialFocusRef,
  onEnter,
  trapFocus = true
}) {
  const modalRef = useRef(null);
  const generatedTitleId = useId();

  useEffect(() => {
    if (!isOpen) return;

    const modal = modalRef.current;
    if (!modal) return;

    const focusTarget = initialFocusRef?.current || modal.querySelector(FOCUSABLE_SELECTOR);
    focusTarget?.focus();
  }, [isOpen, initialFocusRef]);

  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose();
        return;
      }

      if (onEnter && e.key === 'Enter') {
        onEnter();
        return;
      }

      if (!trapFocus || e.key !== 'Tab') return;

      const modal = modalRef.current;
      if (!modal) return;

      const focusableElements = modal.querySelectorAll(FOCUSABLE_SELECTOR);
      if (focusableElements.length === 0) return;

      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];

      if (focusableElements.length === 1) {
        e.preventDefault();
        firstElement.focus();
        return;
      }

      if (e.shiftKey) {
        if (document.activeElement === firstElement) {
          e.preventDefault();
          lastElement.focus();
        }
      } else if (document.activeElement === lastElement) {
        e.preventDefault();
        firstElement.focus();
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose, onEnter, trapFocus]);

  if (!isOpen) return null;

  const titleId = `${generatedTitleId}-title`;

  return (
    <div className="modal-overlay" onClick={onClose} role={role} aria-modal="true" aria-labelledby={titleId}>
      <div
        className={`modal-content ${className}`.trim()}
        onClick={(e) => e.stopPropagation()}
        ref={modalRef}
      >
        <div className="modal-header">
          <h2 id={titleId}>{title}</h2>
          <button className="close-button" onClick={onClose} aria-label="Close">&times;</button>
        </div>
        {children}
      </div>
    </div>
  );
}

export default Modal;
