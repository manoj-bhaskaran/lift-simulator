import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Layout from '../Layout';

vi.mock('../../../package.json', () => ({ default: { version: '1.0.0' } }));

function renderLayout() {
  return render(
    <MemoryRouter>
      <Layout />
    </MemoryRouter>
  );
}

describe('Layout', () => {
  it('renders the app title', () => {
    renderLayout();
    expect(screen.getByText('Lift Simulator Admin')).toBeInTheDocument();
  });

  it('renders all navigation links', () => {
    renderLayout();
    expect(screen.getByRole('link', { name: 'Dashboard' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Lift Systems' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Scenarios' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Simulator' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Runs' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Config Validator' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Health Check' })).toBeInTheDocument();
  });

  it('nav links have correct hrefs', () => {
    renderLayout();
    expect(screen.getByRole('link', { name: 'Dashboard' })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: 'Lift Systems' })).toHaveAttribute('href', '/systems');
    expect(screen.getByRole('link', { name: 'Health Check' })).toHaveAttribute('href', '/health');
  });

  it('renders footer with version number', () => {
    renderLayout();
    expect(screen.getByText(/Version 1\.0\.0/)).toBeInTheDocument();
  });

  it('renders main content area', () => {
    renderLayout();
    expect(document.querySelector('main.main-content')).toBeInTheDocument();
  });
});
