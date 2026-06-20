import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import ConfirmModal from '../ConfirmModal';

describe('ConfirmModal', () => {
  const defaultProps = {
    isOpen: true,
    onClose: vi.fn(),
    onConfirm: vi.fn(),
    title: 'Delete Item',
    message: 'Are you sure you want to delete?',
  };

  it('renders nothing when isOpen is false', () => {
    const { container } = render(<ConfirmModal {...defaultProps} isOpen={false} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders title and message when open', () => {
    render(<ConfirmModal {...defaultProps} />);
    expect(screen.getByText('Delete Item')).toBeInTheDocument();
    expect(screen.getByText('Are you sure you want to delete?')).toBeInTheDocument();
  });

  it('renders default button labels', () => {
    render(<ConfirmModal {...defaultProps} />);
    expect(screen.getByText('Confirm')).toBeInTheDocument();
    expect(screen.getByText('Cancel')).toBeInTheDocument();
  });

  it('renders custom button labels', () => {
    render(<ConfirmModal {...defaultProps} confirmText="Yes, Delete" cancelText="No, Keep" />);
    expect(screen.getByText('Yes, Delete')).toBeInTheDocument();
    expect(screen.getByText('No, Keep')).toBeInTheDocument();
  });

  it('calls onConfirm and onClose when confirm button clicked', async () => {
    const onClose = vi.fn();
    const onConfirm = vi.fn();
    render(<ConfirmModal {...defaultProps} onClose={onClose} onConfirm={onConfirm} />);
    fireEvent.click(screen.getByText('Confirm'));
    expect(onConfirm).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(onClose).toHaveBeenCalledTimes(1));
  });

  it('calls onClose when cancel button clicked', () => {
    const onClose = vi.fn();
    render(<ConfirmModal {...defaultProps} onClose={onClose} />);
    fireEvent.click(screen.getByText('Cancel'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('applies confirmStyle class to confirm button', () => {
    const { container } = render(
      <ConfirmModal {...defaultProps} confirmStyle="danger" confirmText="Delete" />
    );
    expect(container.querySelector('.btn-danger')).toBeInTheDocument();
  });

  it('uses primary style by default for confirm button', () => {
    const { container } = render(<ConfirmModal {...defaultProps} />);
    expect(container.querySelector('.btn-primary')).toBeInTheDocument();
  });
});
