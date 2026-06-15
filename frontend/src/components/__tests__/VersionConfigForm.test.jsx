import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import VersionConfigForm from '../VersionConfigForm';
import { getDefaultVersionFormData, VERSION_CONFIG_FIELDS } from '../../utils/versionConfigSchema';

describe('VersionConfigForm', () => {
  it('renders an input for every configuration field', () => {
    render(<VersionConfigForm value={getDefaultVersionFormData()} onChange={vi.fn()} />);
    for (const field of VERSION_CONFIG_FIELDS) {
      expect(screen.getByLabelText(new RegExp(field.label, 'i'))).toBeInTheDocument();
    }
  });

  it('renders the current values', () => {
    const value = { ...getDefaultVersionFormData(), lifts: '7' };
    render(<VersionConfigForm value={value} onChange={vi.fn()} />);
    expect(screen.getByLabelText(/Number of Lifts/i)).toHaveValue(7);
  });

  it('calls onChange with field name and value when a field is edited', () => {
    const onChange = vi.fn();
    render(<VersionConfigForm value={getDefaultVersionFormData()} onChange={onChange} />);
    fireEvent.change(screen.getByLabelText(/Number of Lifts/i), { target: { value: '4' } });
    expect(onChange).toHaveBeenCalledWith('lifts', '4');
  });

  it('renders select options for enum fields', () => {
    render(<VersionConfigForm value={getDefaultVersionFormData()} onChange={vi.fn()} />);
    const select = screen.getByLabelText(/Controller Strategy/i);
    expect(select.tagName).toBe('SELECT');
    expect(screen.getByRole('option', { name: 'Directional Scan' })).toBeInTheDocument();
  });

  it('shows inline error messages and marks fields invalid', () => {
    render(
      <VersionConfigForm
        value={getDefaultVersionFormData()}
        errors={{ lifts: 'Number of Lifts must be at least 1.' }}
        onChange={vi.fn()}
      />
    );
    expect(screen.getByText('Number of Lifts must be at least 1.')).toBeInTheDocument();
    expect(screen.getByLabelText(/Number of Lifts/i)).toHaveAttribute('aria-invalid', 'true');
  });
});
