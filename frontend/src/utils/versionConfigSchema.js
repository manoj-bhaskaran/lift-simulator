import { CONFIG_EXAMPLE } from './configSchemaHelp';

/**
 * Shared schema metadata for the guided "Create New Version" form.
 *
 * The field definitions, default values, and client-side validation rules in
 * this module mirror the backend contract enforced by {@code LiftConfigDTO} and
 * {@code ConfigValidationService}. Keeping them in one place lets the guided
 * form and the advanced JSON editor stay in sync and produce equivalent output.
 */

export const FIELD_TYPE_NUMBER = 'number';
export const FIELD_TYPE_SELECT = 'select';

/** Allowed controller strategies (mirrors ControllerStrategy enum). */
export const CONTROLLER_STRATEGY_OPTIONS = [
  { value: 'NEAREST_REQUEST_ROUTING', label: 'Nearest Request Routing' },
  { value: 'DIRECTIONAL_SCAN', label: 'Directional Scan' }
];

/** Allowed idle parking modes (mirrors IdleParkingMode enum). */
export const IDLE_PARKING_MODE_OPTIONS = [
  { value: 'STAY_AT_CURRENT_FLOOR', label: 'Stay at Current Floor' },
  { value: 'PARK_TO_HOME_FLOOR', label: 'Park to Home Floor' }
];

/**
 * Ordered field definitions rendered by the guided form. Order is grouped for
 * usability (floors together, door timings together) and does not affect the
 * generated JSON.
 *
 * @typedef {Object} VersionConfigField
 * @property {string} name - Configuration key.
 * @property {string} label - Human-readable label.
 * @property {'number'|'select'} type - Input control type.
 * @property {string} help - Inline help text describing the field.
 * @property {number} [min] - Minimum allowed value for numeric fields.
 * @property {Array<{value: string, label: string}>} [options] - Select options.
 */

/** @type {VersionConfigField[]} */
export const VERSION_CONFIG_FIELDS = [
  {
    name: 'minFloor',
    label: 'Minimum Floor',
    type: FIELD_TYPE_NUMBER,
    help: 'Lowest floor the lifts serve.'
  },
  {
    name: 'maxFloor',
    label: 'Maximum Floor',
    type: FIELD_TYPE_NUMBER,
    help: 'Highest floor the lifts serve. Must be greater than the minimum floor.'
  },
  {
    name: 'homeFloor',
    label: 'Home Floor',
    type: FIELD_TYPE_NUMBER,
    help: 'Floor lifts return to when idle. Must be within the floor range.'
  },
  {
    name: 'lifts',
    label: 'Number of Lifts',
    type: FIELD_TYPE_NUMBER,
    min: 1,
    help: 'How many lift cars the system has (at least 1).'
  },
  {
    name: 'travelTicksPerFloor',
    label: 'Travel Ticks per Floor',
    type: FIELD_TYPE_NUMBER,
    min: 1,
    help: 'Simulation ticks to travel one floor (at least 1).'
  },
  {
    name: 'doorTransitionTicks',
    label: 'Door Transition Ticks',
    type: FIELD_TYPE_NUMBER,
    min: 1,
    help: 'Ticks for doors to open or close (at least 1).'
  },
  {
    name: 'doorDwellTicks',
    label: 'Door Dwell Ticks',
    type: FIELD_TYPE_NUMBER,
    min: 1,
    help: 'Ticks doors stay open before closing (at least 1).'
  },
  {
    name: 'doorReopenWindowTicks',
    label: 'Door Reopen Window Ticks',
    type: FIELD_TYPE_NUMBER,
    min: 0,
    help: 'Ticks while closing during which doors can reopen. Cannot exceed door transition ticks.'
  },
  {
    name: 'idleTimeoutTicks',
    label: 'Idle Timeout Ticks',
    type: FIELD_TYPE_NUMBER,
    min: 0,
    help: 'Ticks of inactivity before a lift is treated as idle.'
  },
  {
    name: 'controllerStrategy',
    label: 'Controller Strategy',
    type: FIELD_TYPE_SELECT,
    options: CONTROLLER_STRATEGY_OPTIONS,
    help: 'Algorithm used to dispatch lifts to requests.'
  },
  {
    name: 'idleParkingMode',
    label: 'Idle Parking Mode',
    type: FIELD_TYPE_SELECT,
    options: IDLE_PARKING_MODE_OPTIONS,
    help: 'What a lift does once it becomes idle.'
  }
];

/**
 * Builds the default form state, pre-filled with the canonical example
 * configuration so the guided form starts in a valid, editable state.
 *
 * @returns {Record<string, string>} Form state keyed by field name (string values).
 */
export function getDefaultVersionFormData() {
  return configToFormData(CONFIG_EXAMPLE);
}

/**
 * Converts a parsed configuration object into form state. Every value is
 * represented as a string (the native representation of form inputs); missing
 * values become empty strings.
 *
 * @param {Record<string, unknown> | null | undefined} config - Parsed config object.
 * @returns {Record<string, string>} Form state keyed by field name.
 */
export function configToFormData(config) {
  const source = config && typeof config === 'object' ? config : {};
  /** @type {Record<string, string>} */
  const formData = {};
  for (const field of VERSION_CONFIG_FIELDS) {
    const value = source[field.name];
    formData[field.name] = value === null || value === undefined ? '' : String(value);
  }
  return formData;
}

/**
 * Converts form state back into a configuration object suitable for JSON
 * serialization. Numeric fields are parsed to numbers; blank values become
 * null so that downstream validation reports them as missing.
 *
 * @param {Record<string, string>} formData - Form state keyed by field name.
 * @returns {Record<string, unknown>} Configuration object.
 */
export function formDataToConfig(formData) {
  /** @type {Record<string, unknown>} */
  const config = {};
  for (const field of VERSION_CONFIG_FIELDS) {
    const raw = formData?.[field.name];
    const trimmed = typeof raw === 'string' ? raw.trim() : raw;
    if (trimmed === '' || trimmed === null || trimmed === undefined) {
      config[field.name] = null;
    } else if (field.type === FIELD_TYPE_NUMBER) {
      const num = Number(trimmed);
      config[field.name] = Number.isNaN(num) ? null : num;
    } else {
      config[field.name] = trimmed;
    }
  }
  return config;
}

/**
 * Serializes form state into a pretty-printed JSON configuration string.
 *
 * @param {Record<string, string>} formData - Form state keyed by field name.
 * @returns {string} Pretty-printed JSON.
 */
export function formDataToJson(formData) {
  return JSON.stringify(formDataToConfig(formData), null, 2);
}

const NUMERIC_PATTERN = /^-?\d+$/;

/**
 * Performs client-side validation of the guided form, mirroring the backend
 * structural and cross-field rules. Returns a map of field name to the first
 * error message for that field so the form can surface inline feedback.
 *
 * @param {Record<string, string>} formData - Form state keyed by field name.
 * @returns {Record<string, string>} Map of field name to error message.
 */
export function validateVersionFormData(formData) {
  /** @type {Record<string, string>} */
  const errors = {};
  /** @type {Record<string, number>} */
  const numbers = {};

  for (const field of VERSION_CONFIG_FIELDS) {
    const raw = formData?.[field.name];
    const trimmed = typeof raw === 'string' ? raw.trim() : '';

    if (trimmed === '') {
      errors[field.name] = `${field.label} is required.`;
      continue;
    }

    if (field.type === FIELD_TYPE_SELECT) {
      const allowed = field.options.some((option) => option.value === trimmed);
      if (!allowed) {
        errors[field.name] = `${field.label} has an unsupported value.`;
      }
      continue;
    }

    if (!NUMERIC_PATTERN.test(trimmed)) {
      errors[field.name] = `${field.label} must be a whole number.`;
      continue;
    }

    const value = Number(trimmed);
    if (field.min !== undefined && value < field.min) {
      errors[field.name] = `${field.label} must be at least ${field.min}.`;
      continue;
    }
    numbers[field.name] = value;
  }

  // Cross-field rules (only checked when both operands parsed cleanly).
  if (
    numbers.maxFloor !== undefined &&
    numbers.minFloor !== undefined &&
    numbers.maxFloor <= numbers.minFloor
  ) {
    errors.maxFloor = `Maximum floor must be greater than minimum floor (${numbers.minFloor}).`;
  }

  if (
    numbers.homeFloor !== undefined &&
    numbers.minFloor !== undefined &&
    numbers.maxFloor !== undefined &&
    (numbers.homeFloor < numbers.minFloor || numbers.homeFloor > numbers.maxFloor)
  ) {
    errors.homeFloor =
      `Home floor must be within the floor range (${numbers.minFloor} to ${numbers.maxFloor}).`;
  }

  if (
    numbers.doorReopenWindowTicks !== undefined &&
    numbers.doorTransitionTicks !== undefined &&
    numbers.doorReopenWindowTicks > numbers.doorTransitionTicks
  ) {
    errors.doorReopenWindowTicks =
      `Door reopen window ticks must not exceed door transition ticks (${numbers.doorTransitionTicks}).`;
  }

  return errors;
}

/**
 * Convenience helper indicating whether the form passes client-side validation.
 *
 * @param {Record<string, string>} formData - Form state keyed by field name.
 * @returns {boolean} True when there are no validation errors.
 */
export function isVersionFormValid(formData) {
  return Object.keys(validateVersionFormData(formData)).length === 0;
}
