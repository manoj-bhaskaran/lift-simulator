export type VersionStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export interface LiftSystem {
  id: string;
  systemKey: string;
  displayName: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
  versionCount?: number;
  versions?: Version[];
}

export interface Version {
  id: string;
  versionNumber: number;
  status: VersionStatus;
  config: string;
  createdAt: string;
  publishedAt?: string;
  archivedAt?: string;
}

export interface ValidationResult {
  valid: boolean;
  errors?: ValidationError[];
  warnings?: ValidationWarning[];
}

export interface ValidationError {
  field: string;
  message: string;
}

export interface ValidationWarning {
  field: string;
  message: string;
}

export type EventType = 'HALL_CALL' | 'CAR_CALL' | 'CANCEL';
export type Direction = 'UP' | 'DOWN' | 'IDLE';
export type ControllerStrategy = 'NAIVE' | 'SIMPLE' | 'DIRECTIONAL_SCAN';
export type IdleParkingMode = 'STAY_PUT' | 'RETURN_HOME' | 'RETURN_TO_LOBBY';

export interface ScenarioEvent {
  id?: number;
  tick: number;
  eventType: EventType;
  description?: string;
  originFloor?: number;
  destinationFloor?: number;
  direction?: Direction;
  eventOrder?: number;
  createdAt?: string;
}

export interface Scenario {
  id?: number;
  name: string;
  description?: string;
  totalTicks: number;
  minFloor: number;
  maxFloor: number;
  initialFloor?: number;
  homeFloor?: number;
  travelTicksPerFloor: number;
  doorTransitionTicks: number;
  doorDwellTicks: number;
  controllerStrategy: ControllerStrategy;
  idleParkingMode: IdleParkingMode;
  seed?: number;
  createdAt?: string;
  updatedAt?: string;
  events?: ScenarioEvent[];
}

export interface ValidationIssue {
  field: string;
  message: string;
  code: string;
}

export interface ScenarioValidationResult {
  valid: boolean;
  errors: ValidationIssue[];
  warnings: ValidationIssue[];
}
