// @ts-check
import { useEffect, useRef } from 'react';

/**
 * Simulation-run statuses that will never transition further. Polling stops
 * once a run reaches one of these.
 */
export const RUN_TERMINAL_STATUSES = new Set(['SUCCEEDED', 'FAILED', 'CANCELLED']);

/**
 * @param {string|null|undefined} status
 * @returns {boolean} Whether the given run status is terminal.
 */
export function isTerminalRunStatus(status) {
  return status ? RUN_TERMINAL_STATUSES.has(status) : false;
}

/**
 * Runs `callback` on a fixed interval while `enabled` is true, and stops
 * automatically when `enabled` becomes false (e.g. because a run reached a
 * terminal status) or the component unmounts.
 *
 * @param {() => void} callback - Invoked on each tick (and immediately, if `immediate` is set).
 * @param {{ intervalMs: number, enabled: boolean, immediate?: boolean }} options
 */
export function useRunPolling(callback, { intervalMs, enabled, immediate = false }) {
  const callbackRef = useRef(callback);

  useEffect(() => {
    callbackRef.current = callback;
  }, [callback]);

  useEffect(() => {
    if (!enabled) {
      return undefined;
    }

    if (immediate) {
      callbackRef.current();
    }

    const intervalId = setInterval(() => {
      callbackRef.current();
    }, intervalMs);

    return () => clearInterval(intervalId);
  }, [enabled, intervalMs, immediate]);
}
