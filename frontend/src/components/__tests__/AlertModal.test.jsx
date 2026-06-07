import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import AlertModal from '../AlertModal';

describe('AlertModal', () => {
  it('renders nothing when isOpen is false', () => {
    const { container } = render(
      <AlertModal isOpen={false} onClose={vi.fn()} message="Oops" />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it('renders title and message when open', () => {
    render(<AlertModal isOpen={true} onClose={vi.fn()} message="Something went wrong" />);
    expect(screen.getByText('Error')).toBeInTheDocument();
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
  });

  it('uses custom title when provided', () => {
    render(<AlertModal isOpen={true} onClose={vi.fn()} title="Warning" message="Watch out" />);
    expect(screen.getByText('Warning')).toBeInTheDocument();
  });

  it('renders error icon for error type', () => {
    render(<AlertModal isOpen={true} onClose={vi.fn()} message="Error" type="error" />);
    expect(screen.getByText('✗')).toBeInTheDocument();
  });

  it('renders warning icon for warning type', () => {
    render(<AlertModal isOpen={true} onClose={vi.fn()} message="Warning" type="warning" />);
    expect(screen.getByText('⚠')).toBeInTheDocument();
  });

  it('renders success icon for success type', () => {
    render(<AlertModal isOpen={true} onClose={vi.fn()} message="Done" type="success" />);
    expect(screen.getByText('✓')).toBeInTheDocument();
  });

  it('renders info icon for info type', () => {
    render(<AlertModal isOpen={true} onClose={vi.fn()} message="FYI" type="info" />);
    expect(screen.getByText('ℹ')).toBeInTheDocument();
  });

  it('renders error icon for unknown type', () => {
    render(<AlertModal isOpen={true} onClose={vi.fn()} message="msg" type="unknown" />);
    expect(screen.getByText('✗')).toBeInTheDocument();
  });

  it('applies type-specific CSS class to body', () => {
    const { container } = render(
      <AlertModal isOpen={true} onClose={vi.fn()} message="Success!" type="success" />
    );
    expect(container.querySelector('.alert-success')).toBeInTheDocument();
  });

  it('calls onClose when OK button is clicked', () => {
    const onClose = vi.fn();
    render(<AlertModal isOpen={true} onClose={onClose} message="msg" />);
    fireEvent.click(screen.getByText('OK'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('has alertdialog role', () => {
    render(<AlertModal isOpen={true} onClose={vi.fn()} message="msg" />);
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
  });
});
