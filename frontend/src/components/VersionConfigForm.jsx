// @ts-check
import { VERSION_CONFIG_FIELDS, FIELD_TYPE_SELECT } from '../utils/versionConfigSchema';

/**
 * Guided form for defining a lift system version configuration.
 *
 * Renders a structured input for each configuration field with inline help and
 * validation messages. The component is controlled: the parent owns the form
 * state and validation errors and receives updates through {@code onChange}.
 *
 * @param {Object} props - Component props.
 * @param {Record<string, string>} props.value - Current form state keyed by field name.
 * @param {Record<string, string>} [props.errors] - Map of field name to error message.
 * @param {(name: string, value: string) => void} props.onChange - Field change handler.
 * @returns {JSX.Element} The guided configuration form.
 */
function VersionConfigForm({ value, errors = {}, onChange }) {
  return (
    <div className="version-config-form">
      <div className="version-config-grid">
        {VERSION_CONFIG_FIELDS.map((field) => {
          const fieldId = `vcf-${field.name}`;
          const helpId = `${fieldId}-help`;
          const errorId = `${fieldId}-error`;
          const fieldError = errors[field.name];
          const describedBy = fieldError ? `${helpId} ${errorId}` : helpId;
          const handleChange = (e) => onChange(field.name, e.target.value);

          return (
            <div className="version-config-field" key={field.name}>
              <label htmlFor={fieldId}>
                {field.label} <span className="required">*</span>
              </label>

              {field.type === FIELD_TYPE_SELECT ? (
                <select
                  id={fieldId}
                  value={value[field.name] ?? ''}
                  onChange={handleChange}
                  className={fieldError ? 'error' : ''}
                  aria-describedby={describedBy}
                  aria-invalid={fieldError ? 'true' : undefined}
                >
                  <option value="" disabled>
                    Select…
                  </option>
                  {field.options.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              ) : (
                <input
                  id={fieldId}
                  type="number"
                  step="1"
                  value={value[field.name] ?? ''}
                  onChange={handleChange}
                  className={fieldError ? 'error' : ''}
                  aria-describedby={describedBy}
                  aria-invalid={fieldError ? 'true' : undefined}
                  {...(field.min !== undefined ? { min: field.min } : {})}
                />
              )}

              <p className="version-config-help" id={helpId}>
                {field.help}
              </p>
              {fieldError && (
                <span className="error-message" id={errorId} role="alert">
                  {fieldError}
                </span>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default VersionConfigForm;
