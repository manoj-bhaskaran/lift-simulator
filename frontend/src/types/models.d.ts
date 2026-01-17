export type VersionStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export interface LiftSystem {
  id: string;
  systemKey: string;
  displayName: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
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
