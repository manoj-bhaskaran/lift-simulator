import { useEffect, useId, useRef } from 'react';
import './Modal.css';

/**
 * CSS selector for focusable elements within the modal.
 * Used for focus management and keyboard navigation.
 * @constant {string}
 */
const FOCUSABLE_SELECTOR =
  'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';

/**
 * Base modal component with accessibility features including focus management,
 * keyboard navigation, and ARIA attributes. Supports focus trapping and custom keyboard handlers.
 *
 * @param {Object} props - Component props
 * @param {boolean} props.isOpen - Whether the modal is open
 * @param {Function} props.onClose - Callback function invoked when modal is closed (via overlay click, Escape key, or close button)
 * @param {string} props.title - Modal title displayed in the header
 * @param {string} [props.role='dialog'] - ARIA role for the modal (e.g., 'dialog', 'alertdialog')
 * @param {string} [props.className=''] - Additional CSS class names to apply to modal content
 * @param {React.ReactNode} props.children - Modal body content
 * @param {React.RefObject} [props.initialFocusRef] - Ref to element that should receive focus when modal opens
 * @param {Function} [props.onEnter] - Optional callback invoked when Enter key is pressed
 * @param {boolean} [props.trapFocus=true] - Whether to trap focus within the modal using Tab navigation
 * @returns {JSX.Element|null} The modal component or null if not open
 */
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

  // Effect to set initial focus when modal opens
  useEffect(() => {
    if (!isOpen) return;

    const modal = modalRef.current;
    if (!modal) return;

    const focusTarget = initialFocusRef?.current || modal.querySelector(FOCUSABLE_SELECTOR);
    focusTarget?.focus();
  }, [isOpen, initialFocusRef]);

  // Effect to handle keyboard navigation and focus trapping
  useEffect(() => {
    if (!isOpen) return;

    /**
     * Handles keyboard events for modal navigation.
     * - Escape: Closes the modal
     * - Enter: Triggers onEnter callback if provided
     * - Tab: Traps focus within modal if trapFocus is true
     *
     * @param {KeyboardEvent} e - Keyboard event
     */
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
