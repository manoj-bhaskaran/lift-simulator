export const kpiLabels = {
  requestsTotal: 'Requests',
  pickupRequestsServed: 'Pickup Requests Served',
  pickupRequestsCancelled: 'Pickup Requests Cancelled',
  passengersServed: 'Passengers Served',
  passengersCancelled: 'Passengers Cancelled',
  avgPickupWaitTicks: 'Avg Wait to Pickup (ticks)',
  maxPickupWaitTicks: 'Max Wait to Pickup (ticks)',
  idleTicks: 'Idle Ticks',
  movingTicks: 'Moving Ticks',
  doorTicks: 'Door Ticks',
  pickupLegUtilisation: 'Pickup-leg Utilisation',
  utilisation: 'Pickup-leg Utilisation',
};

export const formatNumber = (value) => {
  if (value == null || Number.isNaN(value)) return '—';
  if (typeof value === 'number') {
    return Number.isInteger(value) ? value.toString() : value.toFixed(2);
  }
  return String(value);
};

export const formatKpiValue = (key, value) => {
  if ((key === 'pickupLegUtilisation' || key === 'utilisation') && typeof value === 'number') {
    return `${(value * 100).toFixed(1)}%`;
  }
  return formatNumber(value);
};

export const getPickupLegUtilisation = (lift) => lift?.pickupLegUtilisation ?? lift?.utilisation;

export const formatBytes = (bytes) => {
  if (!bytes && bytes !== 0) return '—';
  const units = ['B', 'KB', 'MB', 'GB'];
  let size = bytes;
  let index = 0;
  while (size >= 1024 && index < units.length - 1) {
    size /= 1024;
    index += 1;
  }
  return `${size.toFixed(1)} ${units[index]}`;
};
