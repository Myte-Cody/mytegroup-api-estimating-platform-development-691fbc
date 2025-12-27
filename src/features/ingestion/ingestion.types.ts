export enum IngestionContactsProfile {
  Staff = 'staff',
  Ironworkers = 'ironworkers',
  ExternalPeople = 'external_people',
  CompaniesLocations = 'companies_locations',
  Mixed = 'mixed',
}

export enum IngestionCandidateIssueCode {
  MissingDisplayName = 'missing_display_name',
  MissingIdentifier = 'missing_identifier',
  DuplicateIdentifier = 'duplicate_identifier',
}

export type IngestionContactCandidate = {
  splitIndex: number;
  extractedFrom: string[];
  method: 'deterministic' | 'ai';
  confidence: number;
  person: {
    displayName?: string;
    emails?: string[];
    phones?: string[];
  };
  issues: IngestionCandidateIssueCode[];
};

