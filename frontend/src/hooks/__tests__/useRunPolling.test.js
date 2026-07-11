import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook } from '@testing-library/react';
import { isTerminalRunStatus, useRunPolling } from '../useRunPolling';

describe('isTerminalRunStatus', () => {
  it.each(['SUCCEEDED', 'FAILED', 'CANCELLED'])('treats %s as terminal', (status) => {
    expect(isTerminalRunStatus(status)).toBe(true);
  });

  it.each(['RUNNING', 'CREATED', undefined, null, ''])('treats %s as non-terminal', (status) => {
    expect(isTerminalRunStatus(status)).toBe(false);
  });
});

describe('useRunPolling', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('does not call back or start a timer when disabled', () => {
    const callback = vi.fn();
    renderHook(() => useRunPolling(callback, { intervalMs: 3000, enabled: false }));

    vi.advanceTimersByTime(10000);

    expect(callback).not.toHaveBeenCalled();
  });

  it('invokes the callback on every tick while enabled', () => {
    const callback = vi.fn();
    renderHook(() => useRunPolling(callback, { intervalMs: 3000, enabled: true }));

    expect(callback).not.toHaveBeenCalled();

    vi.advanceTimersByTime(3000);
    expect(callback).toHaveBeenCalledTimes(1);

    vi.advanceTimersByTime(3000);
    expect(callback).toHaveBeenCalledTimes(2);
  });

  it('invokes the callback immediately when immediate is true, then on each tick', () => {
    const callback = vi.fn();
    renderHook(() => useRunPolling(callback, { intervalMs: 3000, enabled: true, immediate: true }));

    expect(callback).toHaveBeenCalledTimes(1);

    vi.advanceTimersByTime(3000);
    expect(callback).toHaveBeenCalledTimes(2);
  });

  it('stops polling once enabled turns false (terminal status reached)', () => {
    const callback = vi.fn();
    const { rerender } = renderHook(
      ({ enabled }) => useRunPolling(callback, { intervalMs: 3000, enabled }),
      { initialProps: { enabled: true } }
    );

    vi.advanceTimersByTime(3000);
    expect(callback).toHaveBeenCalledTimes(1);

    rerender({ enabled: false });
    vi.advanceTimersByTime(10000);

    expect(callback).toHaveBeenCalledTimes(1);
  });

  it('clears the interval on unmount', () => {
    const clearIntervalSpy = vi.spyOn(global, 'clearInterval');
    const callback = vi.fn();
    const { unmount } = renderHook(() => useRunPolling(callback, { intervalMs: 3000, enabled: true }));

    unmount();

    expect(clearIntervalSpy).toHaveBeenCalled();
    vi.advanceTimersByTime(10000);
    expect(callback).not.toHaveBeenCalled();

    clearIntervalSpy.mockRestore();
  });

  it('always calls the latest callback without restarting the timer', () => {
    const firstCallback = vi.fn();
    const secondCallback = vi.fn();
    const { rerender } = renderHook(
      ({ callback }) => useRunPolling(callback, { intervalMs: 3000, enabled: true }),
      { initialProps: { callback: firstCallback } }
    );

    rerender({ callback: secondCallback });
    vi.advanceTimersByTime(3000);

    expect(firstCallback).not.toHaveBeenCalled();
    expect(secondCallback).toHaveBeenCalledTimes(1);
  });
});
