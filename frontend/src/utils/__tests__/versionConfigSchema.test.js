import { describe, it, expect } from 'vitest';
import {
  VERSION_CONFIG_FIELDS,
  getDefaultVersionFormData,
  configToFormData,
  formDataToConfig,
  formDataToJson,
  validateVersionFormData,
  isVersionFormValid
} from '../versionConfigSchema';
import { CONFIG_EXAMPLE, CONFIG_REQUIRED_FIELDS } from '../configSchemaHelp';

describe('versionConfigSchema', () => {
  it('defines a field for every required configuration key', () => {
    const fieldNames = VERSION_CONFIG_FIELDS.map((f) => f.name).sort();
    expect(fieldNames).toEqual([...CONFIG_REQUIRED_FIELDS].sort());
  });

  describe('getDefaultVersionFormData', () => {
    it('pre-fills the canonical example as string values', () => {
      const data = getDefaultVersionFormData();
      expect(data.minFloor).toBe(String(CONFIG_EXAMPLE.minFloor));
      expect(data.controllerStrategy).toBe(CONFIG_EXAMPLE.controllerStrategy);
      expect(data.idleParkingMode).toBe(CONFIG_EXAMPLE.idleParkingMode);
    });

    it('produces a form that passes client validation', () => {
      expect(isVersionFormValid(getDefaultVersionFormData())).toBe(true);
    });
  });

  describe('configToFormData', () => {
    it('stringifies present values and blanks missing ones', () => {
      const data = configToFormData({ minFloor: 0, lifts: 3 });
      expect(data.minFloor).toBe('0');
      expect(data.lifts).toBe('3');
      expect(data.maxFloor).toBe('');
    });

    it('tolerates null and undefined input', () => {
      expect(configToFormData(null).minFloor).toBe('');
      expect(configToFormData(undefined).maxFloor).toBe('');
    });
  });

  describe('formDataToConfig / formDataToJson', () => {
    it('parses numeric fields to numbers and keeps selects as strings', () => {
      const config = formDataToConfig(getDefaultVersionFormData());
      expect(config.minFloor).toBe(CONFIG_EXAMPLE.minFloor);
      expect(typeof config.lifts).toBe('number');
      expect(config.controllerStrategy).toBe(CONFIG_EXAMPLE.controllerStrategy);
    });

    it('maps blank values to null', () => {
      const config = formDataToConfig({ ...getDefaultVersionFormData(), maxFloor: '' });
      expect(config.maxFloor).toBeNull();
    });

    it('round-trips through JSON back into matching form data', () => {
      const original = getDefaultVersionFormData();
      const json = formDataToJson(original);
      const roundTripped = configToFormData(JSON.parse(json));
      expect(roundTripped).toEqual(original);
    });
  });

  describe('validateVersionFormData', () => {
    it('returns no errors for the default form', () => {
      expect(validateVersionFormData(getDefaultVersionFormData())).toEqual({});
    });

    it('flags required fields that are blank', () => {
      const data = { ...getDefaultVersionFormData(), lifts: '' };
      expect(validateVersionFormData(data).lifts).toMatch(/required/i);
    });

    it('enforces minimum values', () => {
      const data = { ...getDefaultVersionFormData(), lifts: '0' };
      expect(validateVersionFormData(data).lifts).toMatch(/at least 1/i);
    });

    it('rejects non-integer numeric input', () => {
      const data = { ...getDefaultVersionFormData(), lifts: '1.5' };
      expect(validateVersionFormData(data).lifts).toMatch(/whole number/i);
    });

    it('requires maxFloor greater than minFloor', () => {
      const data = { ...getDefaultVersionFormData(), minFloor: '5', maxFloor: '5' };
      expect(validateVersionFormData(data).maxFloor).toMatch(/greater than minimum/i);
    });

    it('requires homeFloor within the floor range', () => {
      const data = { ...getDefaultVersionFormData(), minFloor: '0', maxFloor: '9', homeFloor: '20' };
      expect(validateVersionFormData(data).homeFloor).toMatch(/within the floor range/i);
    });

    it('requires doorReopenWindowTicks not to exceed doorTransitionTicks', () => {
      const data = {
        ...getDefaultVersionFormData(),
        doorTransitionTicks: '2',
        doorReopenWindowTicks: '5'
      };
      expect(validateVersionFormData(data).doorReopenWindowTicks).toMatch(/must not exceed/i);
    });

    it('rejects unsupported select values', () => {
      const data = { ...getDefaultVersionFormData(), controllerStrategy: 'BOGUS' };
      expect(validateVersionFormData(data).controllerStrategy).toMatch(/unsupported/i);
    });
  });
});
