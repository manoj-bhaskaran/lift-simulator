import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import Modal from '../Modal';

describe('Modal', () => {
  it('renders nothing when isOpen is false', () => {
    const { container } = render(
      <Modal isOpen={false} onClose={vi.fn()} title="Test">
        <p>Content</p>
      </Modal>
    );
    expect(container).toBeEmptyDOMElement();
  });

  it('renders title and children when open', () => {
    render(
      <Modal isOpen={true} onClose={vi.fn()} title="My Modal">
        <p>Modal body</p>
      </Modal>
    );
    expect(screen.getByText('My Modal')).toBeInTheDocument();
    expect(screen.getByText('Modal body')).toBeInTheDocument();
  });

  it('calls onClose when overlay is clicked', () => {
    const onClose = vi.fn();
    render(
      <Modal isOpen={true} onClose={onClose} title="Test">
        <p>Content</p>
      </Modal>
    );
    fireEvent.click(screen.getByRole('dialog'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('does not call onClose when modal content is clicked', () => {
    const onClose = vi.fn();
    render(
      <Modal isOpen={true} onClose={onClose} title="Test">
        <p>Content</p>
      </Modal>
    );
    fireEvent.click(screen.getByText('Content'));
    expect(onClose).not.toHaveBeenCalled();
  });

  it('calls onClose when close button is clicked', () => {
    const onClose = vi.fn();
    render(
      <Modal isOpen={true} onClose={onClose} title="Test">
        <p>Content</p>
      </Modal>
    );
    fireEvent.click(screen.getByLabelText('Close'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('calls onClose when Escape key is pressed', () => {
    const onClose = vi.fn();
    render(
      <Modal isOpen={true} onClose={onClose} title="Test">
        <p>Content</p>
      </Modal>
    );
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('calls onEnter when Enter key is pressed', () => {
    const onEnter = vi.fn();
    render(
      <Modal isOpen={true} onClose={vi.fn()} onEnter={onEnter} title="Test">
        <p>Content</p>
      </Modal>
    );
    fireEvent.keyDown(document, { key: 'Enter' });
    expect(onEnter).toHaveBeenCalledTimes(1);
  });

  it('applies custom className to modal content', () => {
    const { container } = render(
      <Modal isOpen={true} onClose={vi.fn()} title="Test" className="my-custom-class">
        <p>Content</p>
      </Modal>
    );
    expect(container.querySelector('.my-custom-class')).toBeInTheDocument();
  });

  it('uses the provided role attribute', () => {
    render(
      <Modal isOpen={true} onClose={vi.fn()} title="Alert" role="alertdialog">
        <p>Content</p>
      </Modal>
    );
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
  });

  it('has aria-modal and aria-labelledby attributes', () => {
    render(
      <Modal isOpen={true} onClose={vi.fn()} title="Accessible Modal">
        <p>Content</p>
      </Modal>
    );
    const overlay = screen.getByRole('dialog');
    expect(overlay).toHaveAttribute('aria-modal', 'true');
    expect(overlay).toHaveAttribute('aria-labelledby');
  });
});
