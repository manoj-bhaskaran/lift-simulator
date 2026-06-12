export const CONFIG_EXAMPLE = {
  minFloor: 0,
  maxFloor: 9,
  lifts: 2,
  travelTicksPerFloor: 1,
  doorTransitionTicks: 2,
  doorDwellTicks: 3,
  doorReopenWindowTicks: 2,
  homeFloor: 0,
  idleTimeoutTicks: 5,
  controllerStrategy: 'NEAREST_REQUEST_ROUTING',
  idleParkingMode: 'PARK_TO_HOME_FLOOR'
};

export const CONFIG_EXAMPLE_JSON = JSON.stringify(CONFIG_EXAMPLE, null, 2);

export const CONFIG_REQUIRED_FIELDS = [
  'minFloor',
  'maxFloor',
  'lifts',
  'travelTicksPerFloor',
  'doorTransitionTicks',
  'doorDwellTicks',
  'doorReopenWindowTicks',
  'homeFloor',
  'idleTimeoutTicks',
  'controllerStrategy',
  'idleParkingMode'
];

export const CONFIG_SCHEMA_HELP_TEXT =
  'Required integer fields: minFloor, maxFloor, lifts, travelTicksPerFloor, doorTransitionTicks, doorDwellTicks, doorReopenWindowTicks, homeFloor, idleTimeoutTicks. controllerStrategy must be NEAREST_REQUEST_ROUTING or DIRECTIONAL_SCAN; idleParkingMode must be STAY_AT_CURRENT_FLOOR or PARK_TO_HOME_FLOOR.';

export const CONFIG_SCHEMA_DOCS_URL = 'https://github.com/manoj-bhaskaran/lift-simulator/blob/main/docs/API.md#configuration-structure';
