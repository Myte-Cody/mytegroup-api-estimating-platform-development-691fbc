# Spring Entity Migration Plan

## Overview

Create Spring Boot JPA entities that mirror all 23 NestJS Mongoose schemas, organized by domain. Convert MongoDB string ID references to proper JPA relationships with `@ManyToOne`, `@OneToMany`, and `@OneToOne` annotations.

## Domain Organization

### 1. Core/Identity Domain

- **User** - User accounts and authentication
- **Organization** - Tenant organizations
- **Invite** - User invitations
- **WaitlistEntry** - Waitlist management

### 2. People Management Domain

- **Person** - Modern person entity with emails/phones arrays
- **Contact** - Legacy contact entity

### 3. Organizational Structure Domain

- **Office** - Organization locations/offices
- **OrgTaxonomy** - Organization-specific taxonomy values
- **GraphEdge** - Generic graph relationships between entities

### 4. Projects & Estimates Domain

- **Project** - Construction projects
- **Estimate** - Project estimates
- **Seat** - Seat assignments for projects

### 5. Companies & Locations Domain

- **Company** - External companies
- **CompanyLocation** - Company locations

### 6. Cost Management Domain

- **CostCode** - Cost codes for projects
- **CostCodeImportJob** - Cost code import jobs

### 7. Communication Domain

- **EmailTemplate** - Email templates
- **ContactInquiry** - Contact form inquiries
- **Notification** - User notifications

### 8. Legal & Compliance Domain

- **LegalDoc** - Legal documents (terms, privacy)
- **LegalAcceptance** - User legal document acceptances

### 9. System/Infrastructure Domain

- **EventLog** - System event logging
- **TenantMigration** - Database migration tracking
- **Audit** - Already exists in Spring Boot

## Entity Relationships

### Key Relationships to Implement:

1. **Organization** → **User** (ownerUserId, createdByUserId)
2. **User** → **Organization** (orgId)
3. **Person** → **Organization** (orgId)
4. **Person** → **Office** (orgLocationId)
5. **Person** → **Person** (reportsToPersonId - self-reference)
6. **Person** → **Company** (companyId)
7. **Person** → **CompanyLocation** (companyLocationId)
8. **Person** → **User** (userId)
9. **Project** → **Organization** (orgId)
10. **Project** → **Office** (officeId)
11. **Project** → **Person** (staffing fields)
12. **Project** → **CostCode** (costCodeBudgets)
13. **Project** → **Seat** (seatAssignments)
14. **Estimate** → **Project** (projectId)
15. **Estimate** → **User** (createdByUserId)
16. **Seat** → **User** (userId)
17. **Seat** → **Project** (projectId)
18. **CompanyLocation** → **Company** (companyId)
19. **CostCode** → **CostCodeImportJob** (importJobId)
20. **Invite** → **Organization** (orgId)
21. **Invite** → **Person** (personId)
22. **Invite** → **User** (createdByUserId, invitedUserId)
23. **GraphEdge** → Various entities (polymorphic via nodeType/nodeId)

## Implementation Details

### Base Entity Structure

All entities will extend `BaseEntity` which provides:

- `id` (Long, auto-generated)
- `createdAt` (LocalDateTime)
- `updatedAt` (LocalDateTime)

### Common Fields Across Entities

- `archivedAt` (LocalDateTime, nullable)
- `piiStripped` (Boolean, default false)
- `legalHold` (Boolean, default false)
- `orgId` (Long, foreign key to Organization)

### Embedded Objects

Convert Mongoose embedded schemas to JPA `@Embeddable` classes:

- `PersonEmail` → `@Embeddable` class
- `PersonPhone` → `@Embeddable` class
- `PersonCertification` → `@Embeddable` class
- `ProjectBudget` → `@Embeddable` class
- `ProjectQuantities` → `@Embeddable` class
- `ProjectStaffing` → `@Embeddable` class
- `CostCodeBudget` → `@Embeddable` class
- `SeatAssignment` → `@Embeddable` class
- `OrgTaxonomyValue` → `@Embeddable` class
- `EstimateLineItem` → `@Embeddable` class
- `CollectionProgress` → `@Embeddable` class

### Collection Fields

Convert Mongoose arrays to JPA collections:

- Use `@ElementCollection` for simple arrays (String, Integer)
- Use `@OneToMany` with `@Embeddable` for complex embedded objects
- Use `@ManyToMany` for bidirectional relationships

### Enum Types

Convert TypeScript string enums and union types to Java enums, organized by domain. Create enum classes in domain-specific packages under `com.mytegroup.api.entity.enums`:

#### 1. Core/Identity Domain Enums

**Package**: `com.mytegroup.api.entity.enums.core`

##### Role (common/roles.ts)

**Package**: `com.mytegroup.api.common.enums` (shared across modules)

**Used in**: User, Invite, and other entities

**Values**:

- `SUPER_ADMIN` ("superadmin")
- `PLATFORM_ADMIN` ("platform_admin")
- `ORG_OWNER` ("org_owner")
- `ORG_ADMIN` ("org_admin")
- `MANAGER` ("manager")
- `VIEWER` ("viewer")
- `ADMIN` ("admin")
- `COMPLIANCE_OFFICER` ("compliance_officer")
- `SECURITY_OFFICER` ("security_officer")
- `PM` ("pm")
- `ESTIMATOR` ("estimator")
- `ENGINEER` ("engineer")
- `DETAILER` ("detailer")
- `TRANSPORTER` ("transporter")
- `FOREMAN` ("foreman")
- `SUPERINTENDENT` ("superintendent")
- `QAQC` ("qaqc")
- `HS` ("hs")
- `PURCHASING` ("purchasing")
- `COMPLIANCE` ("compliance")
- `SECURITY` ("security")
- `FINANCE` ("finance")
- `USER` ("user")

##### InviteStatus (invites/schemas/invite.schema.ts)

**Used in**: Invite entity

**Values**:

- `PENDING` ("pending")
- `ACCEPTED` ("accepted")
- `EXPIRED` ("expired")

**Default**: `PENDING`

##### WaitlistStatus (waitlist/waitlist.schema.ts)

**Used in**: WaitlistEntry entity

**Values**:

- `PENDING_COHORT` ("pending-cohort")
- `INVITED` ("invited")
- `ACTIVATED` ("activated")

**Default**: `PENDING_COHORT`

##### WaitlistVerifyStatus (waitlist/waitlist.schema.ts)

**Used in**: WaitlistEntry entity (verifyStatus, phoneVerifyStatus fields)

**Values**:

- `UNVERIFIED` ("unverified")
- `VERIFIED` ("verified")
- `BLOCKED` ("blocked")

**Default**: `UNVERIFIED`

#### 2. People Management Domain Enums

**Package**: `com.mytegroup.api.entity.enums.people`

##### PersonType (persons/schemas/person.schema.ts)

**Used in**: Person entity

**Values**:

- `INTERNAL_STAFF` ("internal_staff")
- `INTERNAL_UNION` ("internal_union")
- `EXTERNAL_PERSON` ("external_person")

##### ContactPersonType (contacts/schemas/contact.schema.ts)

**Note**: Different from PersonType - separate enum

**Used in**: Contact entity

**Values**:

- `STAFF` ("staff")
- `IRONWORKER` ("ironworker")
- `EXTERNAL` ("external")

**Default**: `EXTERNAL`

##### ContactKind (contacts/schemas/contact.schema.ts)

**Used in**: Contact entity

**Values**:

- `INDIVIDUAL` ("individual")
- `BUSINESS` ("business")

**Default**: `INDIVIDUAL`

#### 3. Organizational Structure Domain Enums

**Package**: `com.mytegroup.api.entity.enums.organization`

##### GraphNodeType (graph-edges/schemas/graph-edge.schema.ts)

**Used in**: GraphEdge entity (fromNodeType, toNodeType fields)

**Values**:

- `PERSON` ("person")
- `ORG_LOCATION` ("org_location")
- `COMPANY` ("company")
- `COMPANY_LOCATION` ("company_location")

##### DatastoreType (organizations/schemas/organization.schema.ts)

**Used in**: Organization entity

**Values**:

- `SHARED` ("shared")
- `DEDICATED` ("dedicated")

**Default**: `SHARED`

##### DataResidency (organizations/schemas/organization.schema.ts)

**Used in**: Organization entity

**Values**:

- `SHARED` ("shared")
- `DEDICATED` ("dedicated")

**Default**: `SHARED`

#### 4. Projects & Estimates Domain Enums

**Package**: `com.mytegroup.api.entity.enums.projects`

##### EstimateStatus (estimates/schemas/estimate.schema.ts)

**Used in**: Estimate entity

**Values**:

- `DRAFT` ("draft")
- `FINAL` ("final")
- `ARCHIVED` ("archived")

**Default**: `DRAFT`

##### SeatStatus (seats/schemas/seat.schema.ts)

**Used in**: Seat entity

**Values**:

- `VACANT` ("vacant")
- `ACTIVE` ("active")

**Default**: `VACANT`

#### 5. Cost Management Domain Enums

**Package**: `com.mytegroup.api.entity.enums.cost`

##### CostCodeImportStatus (cost-codes/schemas/cost-code-import-job.schema.ts)

**Used in**: CostCodeImportJob entity

**Values**:

- `QUEUED` ("queued")
- `PROCESSING` ("processing")
- `PREVIEW` ("preview")
- `DONE` ("done")
- `FAILED` ("failed")

**Default**: `QUEUED`

#### 6. Communication Domain Enums

**Package**: `com.mytegroup.api.entity.enums.communication`

##### ContactInquiryStatus (contact-inquiries/schemas/contact-inquiry.schema.ts)

**Used in**: ContactInquiry entity

**Values**:

- `NEW` ("new")
- `IN_PROGRESS` ("in-progress")
- `CLOSED` ("closed")

**Default**: `NEW`

#### 7. Legal & Compliance Domain Enums

**Package**: `com.mytegroup.api.entity.enums.legal`

##### LegalDocType (legal/legal.types.ts)

**Used in**: LegalDoc, LegalAcceptance entities

**Values**:

- `PRIVACY_POLICY` ("privacy_policy")
- `TERMS` ("terms")

#### 8. System/Infrastructure Domain Enums

**Package**: `com.mytegroup.api.entity.enums.system`

##### MigrationDirection (migrations/schemas/tenant-migration.schema.ts)

**Used in**: TenantMigration entity

**Values**:

- `SHARED_TO_DEDICATED` ("shared_to_dedicated")
- `DEDICATED_TO_SHARED` ("dedicated_to_shared")

##### MigrationStatus (migrations/schemas/tenant-migration.schema.ts)

**Used in**: TenantMigration entity

**Values**:

- `PENDING` ("pending")
- `IN_PROGRESS` ("in_progress")
- `READY_FOR_CUTOVER` ("ready_for_cutover")
- `COMPLETED` ("completed")
- `FAILED` ("failed")
- `ABORTED` ("aborted")

**Default**: `PENDING`

### Enum Implementation Notes

- Use `@Enumerated(EnumType.STRING)` for all enum fields to store string values in database
- Use `@Column(nullable = false)` for required enum fields
- Provide default values in entity constructors or use `@ColumnDefault` annotation
- Create `Role` enum in `common.enums` package since it's shared across multiple domains
- Organize enums by domain in sub-packages: `core`, `people`, `organization`, `projects`, `cost`, `communication`, `legal`, `system`
- Note: `PersonType` exists in two forms - one for Person entity and one for Contact entity (named `ContactPersonType` in Java)

## File Structure

```
spring-boot-api/src/main/java/com/mytegroup/api/
├── common/
│   └── enums/
│       └── Role.java
├── entity/
│   ├── BaseEntity.java (exists)
│   ├── Audit.java (exists)
│   ├── enums/
│   │   ├── core/
│   │   │   ├── InviteStatus.java
│   │   │   ├── WaitlistStatus.java
│   │   │   └── WaitlistVerifyStatus.java
│   │   ├── people/
│   │   │   ├── PersonType.java
│   │   │   ├── ContactPersonType.java
│   │   │   └── ContactKind.java
│   │   ├── organization/
│   │   │   ├── GraphNodeType.java
│   │   │   ├── DatastoreType.java
│   │   │   └── DataResidency.java
│   │   ├── projects/
│   │   │   ├── EstimateStatus.java
│   │   │   └── SeatStatus.java
│   │   ├── cost/
│   │   │   └── CostCodeImportStatus.java
│   │   ├── communication/
│   │   │   └── ContactInquiryStatus.java
│   │   ├── legal/
│   │   │   └── LegalDocType.java
│   │   └── system/
│   │       ├── MigrationDirection.java
│   │       └── MigrationStatus.java
│   ├── core/
│   │   ├── User.java
│   │   ├── Organization.java
│   │   ├── Invite.java
│   │   └── WaitlistEntry.java
│   ├── people/
│   │   ├── Person.java
│   │   ├── Contact.java
│   │   └── embeddable/
│   │       ├── PersonEmail.java
│   │       ├── PersonPhone.java
│   │       └── PersonCertification.java
│   ├── organization/
│   │   ├── Office.java
│   │   ├── OrgTaxonomy.java
│   │   └── GraphEdge.java
│   ├── projects/
│   │   ├── Project.java
│   │   ├── Estimate.java
│   │   ├── Seat.java
│   │   └── embeddable/
│   │       ├── ProjectBudget.java
│   │       ├── ProjectQuantities.java
│   │       ├── ProjectStaffing.java
│   │       ├── CostCodeBudget.java
│   │       ├── SeatAssignment.java
│   │       └── EstimateLineItem.java
│   ├── companies/
│   │   ├── Company.java
│   │   └── CompanyLocation.java
│   ├── cost/
│   │   ├── CostCode.java
│   │   └── CostCodeImportJob.java
│   ├── communication/
│   │   ├── EmailTemplate.java
│   │   ├── ContactInquiry.java
│   │   └── Notification.java
│   ├── legal/
│   │   ├── LegalDoc.java
│   │   └── LegalAcceptance.java
│   └── system/
│       ├── EventLog.java
│       └── TenantMigration.java
```

## Mandatory Fields by Entity

### Core/Identity Domain

#### User

- `username` (String, unique, @Column(nullable = false))
- `email` (String, unique, @Column(nullable = false))
- `passwordHash` (String, @Column(nullable = false))
- `role` (String, default: 'user', @Column(nullable = false))
- `roles` (List<String>, default: ['user'], @ElementCollection)
- `isEmailVerified` (Boolean, default: false, @Column(nullable = false))
- `isOrgOwner` (Boolean, default: false, @Column(nullable = false))
- `piiStripped` (Boolean, default: false, @Column(nullable = false))
- `legalHold` (Boolean, default: false, @Column(nullable = false))

#### Organization

- `name` (String, unique, @Column(nullable = false))
- `useDedicatedDb` (Boolean, default: false, @Column(nullable = false))
- `datastoreType` (enum, default: 'shared', @Column(nullable = false))
- `dataResidency` (enum, default: 'shared', @Column(nullable = false))
- `piiStripped` (Boolean, default: false, @Column(nullable = false))
- `legalHold` (Boolean, default: false, @Column(nullable = false))

#### Invite

- `orgId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `email` (String, @Column(nullable = false))
- `role` (String, @Column(nullable = false))
- `tokenHash` (String, @Column(nullable = false))
- `tokenExpires` (LocalDateTime, @Column(nullable = false))
- `status` (enum, default: 'pending', @Column(nullable = false))
- `createdByUserId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `piiStripped` (Boolean, default: false, @Column(nullable = false))
- `legalHold` (Boolean, default: false, @Column(nullable = false))

#### WaitlistEntry

- `email` (String, unique, @Column(nullable = false))
- `name` (String, @Column(nullable = false))
- `phone` (String, @Column(nullable = false))
- `role` (String, @Column(nullable = false))
- `status` (enum, default: 'pending-cohort', @Column(nullable = false))
- `verifyStatus` (enum, default: 'unverified', @Column(nullable = false))
- `verifyAttempts` (Integer, default: 0, @Column(nullable = false))
- `verifyAttemptTotal` (Integer, default: 0, @Column(nullable = false))
- `verifyResends` (Integer, default: 0, @Column(nullable = false))
- `phoneVerifyStatus` (enum, default: 'unverified', @Column(nullable = false))
- `phoneVerifyAttempts` (Integer, default: 0, @Column(nullable = false))
- `phoneVerifyAttemptTotal` (Integer, default: 0, @Column(nullable = false))
- `phoneVerifyResends` (Integer, default: 0, @Column(nullable = false))
- `preCreateAccount` (Boolean, default: false, @Column(nullable = false))
- `marketingConsent` (Boolean, default: false, @Column(nullable = false))
- `piiStripped` (Boolean, default: false, @Column(nullable = false))
- `legalHold` (Boolean, default: false, @Column(nullable = false))

### People Management Domain

#### Person

- `orgId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `personType` (enum, @Column(nullable = false))
- `displayName` (String, @Column(nullable = false))
- `emails` (List<PersonEmail>, default: [], @ElementCollection)
- `phones` (List<PersonPhone>, default: [], @ElementCollection)
- `tagKeys` (List<String>, default: [], @ElementCollection)
- `skillKeys` (List<String>, default: [], @ElementCollection)
- `skillFreeText` (List<String>, default: [], @ElementCollection)
- `certifications` (List<PersonCertification>, default: [], @ElementCollection)
- `piiStripped` (Boolean, default: false, @Column(nullable = false))
- `legalHold` (Boolean, default: false, @Column(nullable = false))

**PersonEmail (@Embeddable):**

- `value` (String, @Column(nullable = false))
- `normalized` (String, @Column(nullable = false))

**PersonPhone (@Embeddable):**

- `value` (String, @Column(nullable = false))
- `e164` (String, @Column(nullable = false))

**PersonCertification (@Embeddable):**

- `name` (String, @Column(nullable = false))

#### Contact

- `orgId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `name` (String, @Column(nullable = false))
- `personType` (enum, default: 'external', @Column(nullable = false))
- `contactKind` (enum, default: 'individual', @Column(nullable = false))
- `promotedToForeman` (Boolean, default: false, @Column(nullable = false))
- `roles` (List<String>, default: [], @ElementCollection)
- `tags` (List<String>, default: [], @ElementCollection)
- `skills` (List<String>, default: [], @ElementCollection)
- `certifications` (List<ContactCertification>, default: [], @ElementCollection)
- `piiStripped` (Boolean, default: false, @Column(nullable = false))
- `legalHold` (Boolean, default: false, @Column(nullable = false))

**ContactCertification (@Embeddable):**

- `name` (String, @Column(nullable = false))

### Organizational Structure Domain

#### Office

- `name` (String, @Column(nullable = false))
- `normalizedName` (String, @Column(nullable = false))
- `orgId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `tagKeys` (List<String>, default: [], @ElementCollection)
- `piiStripped` (Boolean, default: false, @Column(nullable = false))
- `legalHold` (Boolean, default: false, @Column(nullable = false))

#### OrgTaxonomy

- `orgId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `namespace` (String, @Column(nullable = false))
- `values` (List<OrgTaxonomyValue>, default: [], @ElementCollection)

**OrgTaxonomyValue (@Embeddable):**

- `key` (String, @Column(nullable = false))
- `label` (String, @Column(nullable = false))

#### GraphEdge

- `orgId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `fromNodeType` (enum, @Column(nullable = false))
- `fromNodeId` (Long, @Column(nullable = false))
- `toNodeType` (enum, @Column(nullable = false))
- `toNodeId` (Long, @Column(nullable = false))
- `edgeTypeKey` (String, @Column(nullable = false))
- `piiStripped` (Boolean, default: false, @Column(nullable = false))
- `legalHold` (Boolean, default: false, @Column(nullable = false))

### Projects & Estimates Domain

#### Project

- `name` (String, @Column(nullable = false))
- `orgId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `costCodeBudgets` (List<CostCodeBudget>, default: [], @ElementCollection)
- `seatAssignments` (List<SeatAssignment>, default: [], @ElementCollection)
- `piiStripped` (Boolean, default: false, @Column(nullable = false))
- `legalHold` (Boolean, default: false, @Column(nullable = false))

**CostCodeBudget (@Embeddable):**

- `costCodeId` (Long, @Column(nullable = false))

**SeatAssignment (@Embeddable):**

- `seatId` (Long, @Column(nullable = false))

#### Estimate

- `projectId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `orgId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `createdByUserId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `name` (String, @Column(nullable = false))
- `status` (enum, default: 'draft', @Column(nullable = false))
- `revision` (Integer, default: 1, @Column(nullable = false))
- `lineItems` (List<EstimateLineItem>, default: [], @ElementCollection)
- `piiStripped` (Boolean, default: false, @Column(nullable = false))
- `legalHold` (Boolean, default: false, @Column(nullable = false))

#### Seat

- `orgId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `seatNumber` (Integer, @Column(nullable = false))
- `status` (enum, default: 'vacant', @Column(nullable = false))
- `history` (List<SeatHistoryEntry>, default: [], @ElementCollection)

### Companies & Locations Domain

#### Company

- `orgId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `name` (String, @Column(nullable = false))
- `normalizedName` (String, @Column(nullable = false))
- `companyTypeKeys` (List<String>, default: [], @ElementCollection)
- `tagKeys` (List<String>, default: [], @ElementCollection)
- `piiStripped` (Boolean, default: false, @Column(nullable = false))
- `legalHold` (Boolean, default: false, @Column(nullable = false))

#### CompanyLocation

- `orgId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `companyId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `name` (String, @Column(nullable = false))
- `normalizedName` (String, @Column(nullable = false))
- `tagKeys` (List<String>, default: [], @ElementCollection)
- `piiStripped` (Boolean, default: false, @Column(nullable = false))
- `legalHold` (Boolean, default: false, @Column(nullable = false))

### Cost Management Domain

#### CostCode

- `orgId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `category` (String, @Column(nullable = false))
- `code` (String, @Column(nullable = false))
- `description` (String, @Column(nullable = false))
- `active` (Boolean, default: false, @Column(nullable = false))
- `isUsed` (Boolean, default: false, @Column(nullable = false))

#### CostCodeImportJob

- `orgId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `status` (enum, default: 'queued', @Column(nullable = false))
- `preview` (List<CostCodeImportPreview>, default: [], @ElementCollection)
- `dryRun` (Boolean, default: false, @Column(nullable = false))
- `resumeRequested` (Boolean, default: true, @Column(nullable = false))
- `allowLegalHoldOverride` (Boolean, default: false, @Column(nullable = false))
- `chunkSize` (Integer, default: 100, @Column(nullable = false))
- `progress` (Map<String, CollectionProgress>, default: {}, @ElementCollection)
- `startedAt` (LocalDateTime, default: now, @Column(nullable = false))

### Communication Domain

#### EmailTemplate

- `orgId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `name` (String, @Column(nullable = false))
- `locale` (String, default: EMAIL_TEMPLATE_DEFAULT_LOCALE, @Column(nullable = false))
- `subject` (String, @Column(nullable = false))
- `html` (String, @Column(nullable = false))
- `text` (String, @Column(nullable = false))
- `requiredVariables` (List<String>, default: [], @ElementCollection)
- `optionalVariables` (List<String>, default: [], @ElementCollection)
- `piiStripped` (Boolean, default: false, @Column(nullable = false))
- `legalHold` (Boolean, default: false, @Column(nullable = false))

#### ContactInquiry

- `name` (String, @Column(nullable = false))
- `email` (String, @Column(nullable = false))
- `message` (String, @Column(nullable = false))
- `status` (enum, default: 'new', @Column(nullable = false))
- `piiStripped` (Boolean, default: false, @Column(nullable = false))
- `legalHold` (Boolean, default: false, @Column(nullable = false))

#### Notification

- `orgId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `userId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `type` (String, @Column(nullable = false))
- `payload` (Map<String, Object>, default: {}, @ElementCollection)
- `read` (Boolean, default: false, @Column(nullable = false))

### Legal & Compliance Domain

#### LegalDoc

- `type` (enum, @Column(nullable = false))
- `version` (String, @Column(nullable = false))
- `content` (String, @Column(nullable = false))
- `effectiveAt` (LocalDateTime, default: now, @Column(nullable = false))

#### LegalAcceptance

- `userId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `docType` (enum, @Column(nullable = false))
- `version` (String, @Column(nullable = false))
- `acceptedAt` (LocalDateTime, default: now, @Column(nullable = false))
- `piiStripped` (Boolean, default: false, @Column(nullable = false))
- `legalHold` (Boolean, default: false, @Column(nullable = false))

### System/Infrastructure Domain

#### EventLog

- `eventType` (String, @Column(nullable = false))
- `piiStripped` (Boolean, default: false, @Column(nullable = false))
- `legalHold` (Boolean, default: false, @Column(nullable = false))
- `createdAt` (LocalDateTime, auto-generated, @Column(nullable = false))

#### TenantMigration

- `orgId` (Long, @ManyToOne, @JoinColumn(nullable = false))
- `direction` (enum, @Column(nullable = false))
- `status` (enum, default: 'pending', @Column(nullable = false))
- `dryRun` (Boolean, default: false, @Column(nullable = false))
- `resumeRequested` (Boolean, default: true, @Column(nullable = false))
- `allowLegalHoldOverride` (Boolean, default: false, @Column(nullable = false))
- `chunkSize` (Integer, default: 100, @Column(nullable = false))
- `progress` (Map<String, CollectionProgress>, default: {}, @ElementCollection)
- `startedAt` (LocalDateTime, default: now, @Column(nullable = false))

## Self-Referencing Relationships

1. **Person** → **Person** (`reportsToPersonId`)

   - `@ManyToOne @JoinColumn(name = "reports_to_person_id") private Person reportsTo;`
   - `@OneToMany(mappedBy = "reportsTo") private List<Person> reports;`
   - Validation: Cannot reference self

2. **Contact** → **Contact** (`reportsToContactId`)

   - `@ManyToOne @JoinColumn(name = "reports_to_contact_id") private Contact reportsTo;`
   - `@OneToMany(mappedBy = "reportsTo") private List<Contact> reports;`

3. **Office** → **Office** (`parentOrgLocationId`)

   - `@ManyToOne @JoinColumn(name = "parent_org_location_id") private Office parent;`
   - `@OneToMany(mappedBy = "parent") private List<Office> children;`
   - Validation: Cannot reference self

## Indexes

Replicate all Mongoose indexes as JPA `@Table` indexes:

- Unique constraints for business keys
- Composite indexes for common queries
- Partial indexes will be handled via application logic (archivedAt filtering)

## Flyway Migration Scripts

Create Flyway migration scripts to create all database tables, organized by domain. Migration files should be placed in `spring-boot-api/src/main/resources/db/migration/` following the naming convention `V{version}__{description}.sql`.

### Migration Script Organization

#### V2__Create_Core_Domain_Tables.sql

**Tables**: `organizations`, `users`, `invites`, `waitlist_entries`

**Order**: Create `organizations` first (no dependencies), then `users` (depends on organizations), then `invites` and `waitlist_entries` (depend on organizations/users).

**Key Features**:

- `organizations`: Unique constraint on `name`, unique partial index on `primary_domain` (where not null)
- `users`: Unique constraint on `email`, composite index on `org_id, archived_at`
- `invites`: Composite indexes on `org_id, email, status` and `org_id, person_id, status`, index on `token_hash`, TTL index on `token_expires`
- `waitlist_entries`: Unique constraint on `email`, indexes on `status, created_at` and `verify_status, status, created_at`

#### V3__Create_People_Domain_Tables.sql

**Tables**: `persons`, `contacts`

**Order**: Create `persons` and `contacts` (both depend on organizations).

**Key Features**:

- `persons`: 
  - Unique partial indexes on `org_id, primary_email` (where archived_at is null and primary_email is not null)
  - Unique partial indexes on `org_id, external_id`, `org_id, primary_phone_e164`, `org_id, ironworker_number`
  - Composite indexes on `org_id, archived_at`, `org_id, emails_normalized`, `org_id, phones_e164`
  - Self-referencing FK: `reports_to_person_id` → `persons(id)`
  - Embedded collections: `person_emails`, `person_phones`, `person_certifications` (element collections)
- `contacts`:
  - Composite indexes on `org_id, email`, `org_id, person_type, archived_at`, `org_id, ironworker_number`, `org_id, archived_at`
  - Self-referencing FK: `reports_to_contact_id` → `contacts(id)`
  - Embedded collections: `contact_certifications` (element collection)

#### V4__Create_Organization_Domain_Tables.sql

**Tables**: `offices`, `org_taxonomies`, `graph_edges`

**Order**: Create `offices` first (depends on organizations, self-referencing), then `org_taxonomies` and `graph_edges` (depend on organizations).

**Key Features**:

- `offices`:
  - Unique partial index on `org_id, normalized_name` (where archived_at is null)
  - Composite indexes on `org_id, archived_at`, `org_id, parent_org_location_id`
  - Self-referencing FK: `parent_org_location_id` → `offices(id)`
  - Embedded collection: `office_tag_keys` (element collection)
- `org_taxonomies`:
  - Unique constraint on `org_id, namespace`
  - Embedded collection: `org_taxonomy_values` (JSONB or separate table for complex structure)
- `graph_edges`:
  - Unique partial index on `org_id, edge_type_key, from_node_type, from_node_id, to_node_type, to_node_id` (where archived_at is null)
  - Composite indexes on `org_id, from_node_type, from_node_id`, `org_id, to_node_type, to_node_id`, `org_id, archived_at`

#### V5__Create_Projects_Domain_Tables.sql

**Tables**: `projects`, `estimates`, `seats`

**Order**: Create `projects` first (depends on organizations, offices), then `estimates` (depends on projects), then `seats` (depends on organizations, projects, users).

**Key Features**:

- `projects`:
  - Unique partial indexes on `org_id, name` and `org_id, project_code` (where archived_at is null)
  - Composite index on `org_id, archived_at`
  - Embedded collections: `project_cost_code_budgets`, `project_seat_assignments` (element collections)
  - Embedded objects: `project_budget` (JSONB), `project_quantities` (JSONB), `project_staffing` (JSONB)
- `estimates`:
  - Unique partial index on `org_id, project_id, name` (where archived_at is null)
  - Composite index on `org_id, project_id, archived_at`
  - Embedded collection: `estimate_line_items` (element collection)
- `seats`:
  - Unique constraint on `org_id, seat_number`
  - Unique partial index on `org_id, user_id` (where user_id is not null)
  - Composite indexes on `org_id, status`, `org_id, role, status`, `org_id, project_id`
  - Embedded collection: `seat_history` (element collection)

#### V6__Create_Companies_Domain_Tables.sql

**Tables**: `companies`, `company_locations`

**Order**: Create `companies` first (depends on organizations), then `company_locations` (depends on companies).

**Key Features**:

- `companies`:
  - Unique partial index on `org_id, normalized_name` (where archived_at is null)
  - Unique partial index on `org_id, external_id` (where archived_at is null and external_id is not null)
  - Composite indexes on `org_id, archived_at`, `org_id, company_type_keys`, `org_id, tag_keys`
  - Embedded collections: `company_type_keys`, `company_tag_keys` (element collections)
- `company_locations`:
  - Unique partial index on `org_id, company_id, normalized_name` (where archived_at is null)
  - Unique partial index on `org_id, company_id, external_id` (where archived_at is null and external_id is not null)
  - Composite indexes on `org_id, company_id`, `org_id, tag_keys`, `org_id, company_id, tag_keys`, `org_id, archived_at`
  - Embedded collection: `company_location_tag_keys` (element collection)

#### V7__Create_Cost_Domain_Tables.sql

**Tables**: `cost_codes`, `cost_code_import_jobs`

**Order**: Create `cost_code_import_jobs` first (depends on organizations), then `cost_codes` (depends on organizations and import jobs).

**Key Features**:

- `cost_codes`:
  - Unique constraint on `org_id, code`
  - Composite index on `org_id, category`, index on `active`, index on `import_job_id`
- `cost_code_import_jobs`:
  - Composite index on `org_id`
  - Embedded collection: `cost_code_import_previews` (element collection)

#### V8__Create_Communication_Domain_Tables.sql

**Tables**: `email_templates`, `contact_inquiries`, `notifications`

**Order**: All depend on organizations, create in any order.

**Key Features**:

- `email_templates`:
  - Unique constraint on `org_id, name, locale`
  - Embedded collections: `email_template_required_variables`, `email_template_optional_variables` (element collections)
- `contact_inquiries`:
  - Indexes on `created_at DESC`, `status, created_at DESC`
- `notifications`:
  - Composite index on `org_id, user_id, read, created_at DESC`

#### V9__Create_Legal_Domain_Tables.sql

**Tables**: `legal_docs`, `legal_acceptances`

**Order**: Create `legal_docs` first (no dependencies), then `legal_acceptances` (depends on users, legal_docs).

**Key Features**:

- `legal_docs`:
  - Unique constraint on `type, version`
  - Composite index on `type, effective_at DESC, created_at DESC`
- `legal_acceptances`:
  - Unique constraint on `user_id, doc_type, version`
  - Composite index on `org_id, doc_type, version`

#### V10__Create_System_Domain_Tables.sql

**Tables**: `event_logs`, `tenant_migrations`

**Order**: Both depend on organizations, create in any order.

**Key Features**:

- `event_logs`:
  - Composite indexes on `org_id, created_at DESC`, `org_id, entity_id, created_at DESC`, `org_id, action, created_at DESC`, `org_id, event_type, created_at DESC`
  - TTL index on `created_at` (10 years retention, skip when legal_hold is true)
- `tenant_migrations`:
  - Composite index on `org_id`
  - Embedded collection: `tenant_migration_progress` (JSONB or Map structure)

### Migration Script Guidelines

1. **Table Creation Order**: Respect foreign key dependencies - create referenced tables before referencing tables
2. **Enum Handling**: Use VARCHAR columns with CHECK constraints or ENUM types (PostgreSQL supports both)
3. **Default Values**: Set defaults for NOT NULL columns with default values
4. **Indexes**: Create all indexes defined in Mongoose schemas, including:

   - Unique constraints
   - Composite indexes
   - Partial indexes (using WHERE clauses where possible)

5. **Foreign Keys**: Add foreign key constraints with appropriate ON DELETE behavior:

   - `ON DELETE RESTRICT` for critical relationships
   - `ON DELETE SET NULL` for optional relationships
   - `ON DELETE CASCADE` for dependent data

6. **Element Collections**: For `@ElementCollection` fields, create separate tables:

   - `{entity}_{collection_name}` tables
   - Foreign key to parent entity
   - Appropriate indexes

7. **Embedded Objects**: For complex embedded objects (JSONB), use JSONB columns in PostgreSQL
8. **Timestamps**: Use `TIMESTAMP` type, set defaults for `created_at` and `updated_at`
9. **Boolean Defaults**: Set `DEFAULT false` for boolean fields with defaults
10. **Nullable Fields**: Mark optional fields as `NULL`, required fields as `NOT NULL`

### Element Collection Tables

Create separate tables for `@ElementCollection` fields:

- `person_emails` (person_id, value, normalized, label, is_primary, verified_at)
- `person_phones` (person_id, value, e164, label, is_primary)
- `person_certifications` (person_id, name, issued_at, expires_at, document_url, notes)
- `contact_certifications` (contact_id, name, issued_at, expires_at, document_url, notes)
- `project_cost_code_budgets` (project_id, cost_code_id, budgeted_hours, cost_budget)
- `project_seat_assignments` (project_id, seat_id, person_id, role, assigned_at, removed_at)
- `seat_history` (seat_id, user_id, project_id, role, assigned_at, removed_at)
- `office_tag_keys` (office_id, tag_key)
- `company_type_keys` (company_id, type_key)
- `company_tag_keys` (company_id, tag_key)
- `company_location_tag_keys` (company_location_id, tag_key)
- `person_tag_keys` (person_id, tag_key)
- `person_skill_keys` (person_id, skill_key)
- `person_skill_free_text` (person_id, skill_text)
- `contact_roles` (contact_id, role)
- `contact_tags` (contact_id, tag)
- `contact_skills` (contact_id, skill)
- `email_template_required_variables` (email_template_id, variable)
- `email_template_optional_variables` (email_template_id, variable)
- `estimate_line_items` (estimate_id, code, description, quantity, unit, unit_cost, total)
- `cost_code_import_previews` (cost_code_import_job_id, category, code, description)
- `org_taxonomy_values` (org_taxonomy_id, key, label, sort_order, color, metadata JSONB, archived_at)

### Indexes to Create

Replicate all Mongoose indexes:

- Unique indexes for business keys
- Composite indexes for common query patterns
- Partial unique indexes (using WHERE clauses) for soft-delete scenarios
- TTL indexes (using application logic or PostgreSQL features)

### Foreign Key Constraints

Define foreign keys with appropriate cascade behavior:

- `users.org_id` → `organizations.id` (RESTRICT)
- `persons.org_id` → `organizations.id` (RESTRICT)
- `persons.reports_to_person_id` → `persons.id` (SET NULL)
- `persons.org_location_id` → `offices.id` (SET NULL)
- `persons.company_id` → `companies.id` (SET NULL)
- `persons.company_location_id` → `company_locations.id` (SET NULL)
- `persons.user_id` → `users.id` (SET NULL)
- `projects.org_id` → `organizations.id` (RESTRICT)
- `projects.office_id` → `offices.id` (SET NULL)
- `estimates.project_id` → `projects.id` (RESTRICT)
- `estimates.created_by_user_id` → `users.id` (RESTRICT)
- `seats.org_id` → `organizations.id` (RESTRICT)
- `seats.user_id` → `users.id` (SET NULL)
- `seats.project_id` → `projects.id` (SET NULL)
- `company_locations.company_id` → `companies.id` (RESTRICT)
- `cost_codes.import_job_id` → `cost_code_import_jobs.id` (SET NULL)
- `invites.org_id` → `organizations.id` (RESTRICT)
- `invites.person_id` → `persons.id` (SET NULL)
- `invites.created_by_user_id` → `users.id` (RESTRICT)
- `invites.invited_user_id` → `users.id` (SET NULL)
- `legal_acceptances.user_id` → `users.id` (RESTRICT)
- `legal_acceptances.org_id` → `organizations.id` (SET NULL)
- `notifications.org_id` → `organizations.id` (RESTRICT)
- `notifications.user_id` → `users.id` (RESTRICT)
- `email_templates.org_id` → `organizations.id` (RESTRICT)
- `contacts.org_id` → `organizations.id` (RESTRICT)
- `contacts.office_id` → `offices.id` (SET NULL)
- `contacts.reports_to_contact_id` → `contacts.id` (SET NULL)
- `contacts.invited_user_id` → `users.id` (SET NULL)
- `contacts.foreman_user_id` → `users.id` (SET NULL)
- `offices.org_id` → `organizations.id` (RESTRICT)
- `offices.parent_org_location_id` → `offices.id` (SET NULL)
- `org_taxonomies.org_id` → `organizations.id` (RESTRICT)
- `graph_edges.org_id` → `organizations.id` (RESTRICT)
- `event_logs.org_id` → `organizations.id` (SET NULL)
- `tenant_migrations.org_id` → `organizations.id` (RESTRICT)

## Repository Layer

Create Spring Data JPA repository interfaces for all entities, organized by domain. Repositories should extend `JpaRepository` and include custom query methods based on NestJS service patterns.

### Repository Structure

```
spring-boot-api/src/main/java/com/mytegroup/api/repository/
├── core/
│   ├── UserRepository.java
│   ├── OrganizationRepository.java
│   ├── InviteRepository.java
│   └── WaitlistEntryRepository.java
├── people/
│   ├── PersonRepository.java
│   └── ContactRepository.java
├── organization/
│   ├── OfficeRepository.java
│   ├── OrgTaxonomyRepository.java
│   └── GraphEdgeRepository.java
├── projects/
│   ├── ProjectRepository.java
│   ├── EstimateRepository.java
│   └── SeatRepository.java
├── companies/
│   ├── CompanyRepository.java
│   └── CompanyLocationRepository.java
├── cost/
│   ├── CostCodeRepository.java
│   └── CostCodeImportJobRepository.java
├── communication/
│   ├── EmailTemplateRepository.java
│   ├── ContactInquiryRepository.java
│   └── NotificationRepository.java
├── legal/
│   ├── LegalDocRepository.java
│   └── LegalAcceptanceRepository.java
└── system/
    ├── EventLogRepository.java
    └── TenantMigrationRepository.java
```

### Common Repository Patterns

#### Base Repository Methods

All repositories should include:

1. **Soft Delete Support**:

   - `findByOrgIdAndArchivedAtIsNull(Long orgId)` - Find active entities
   - `findByOrgId(Long orgId)` - Find all (including archived)
   - `existsByOrgIdAndArchivedAtIsNull(Long orgId)` - Check if active exists

2. **Organization Scoping**:

   - All queries should be scoped by `orgId` for multi-tenant isolation
   - Use `@Query` annotations for complex queries

3. **Pagination Support**:

   - Use `Pageable` parameter for pagination
   - Return `Page<T>` or custom pagination DTOs

### Domain-Specific Repository Methods

#### Core Domain Repositories

**UserRepository**:

- `findByEmail(String email)` - Find by email (unique)
- `findByOrgIdAndArchivedAtIsNull(Long orgId)` - List active users
- `findByOrgIdAndEmail(Long orgId, String email)` - Find by org and email
- `existsByEmail(String email)` - Check email exists
- `findByOrgIdAndRolesContaining(Long orgId, String role)` - Find by role

**OrganizationRepository**:

- `findByName(String name)` - Find by name (unique)
- `findByPrimaryDomain(String domain)` - Find by primary domain
- `findByDatastoreType(DatastoreType type)` - Find by datastore type
- `existsByArchivedAtIsNull()` - Check if any active org exists

**InviteRepository**:

- `findByOrgIdAndEmailAndStatus(Long orgId, String email, InviteStatus status)`
- `findByOrgIdAndPersonIdAndStatus(Long orgId, Long personId, InviteStatus status)`
- `findByTokenHash(String tokenHash)`
- `findByTokenExpiresBefore(LocalDateTime date)` - Find expired invites

**WaitlistEntryRepository**:

- `findByEmail(String email)` - Find by email (unique)
- `findByStatusAndCreatedAtDesc(WaitlistStatus status, Pageable pageable)`
- `findByVerifyStatusAndStatusAndCreatedAtAsc(WaitlistVerifyStatus verifyStatus, WaitlistStatus status, Pageable pageable)`
- `findByEmailContainingIgnoreCase(String email)` - Search by email

#### People Domain Repositories

**PersonRepository**:

- `findByOrgIdAndArchivedAtIsNull(Long orgId, Pageable pageable)` - List with pagination
- `findByOrgIdAndPersonType(Long orgId, PersonType personType)` - Filter by type
- `findByOrgIdAndPrimaryEmail(Long orgId, String email)` - Find by primary email
- `findByOrgIdAndPrimaryPhoneE164(Long orgId, String phone)` - Find by phone
- `findByOrgIdAndIronworkerNumber(Long orgId, String number)` - Find by ironworker number
- `findByOrgIdAndCompanyId(Long orgId, Long companyId)` - Find by company
- `findByOrgIdAndCompanyLocationId(Long orgId, Long locationId)` - Find by location
- `findByOrgIdAndOrgLocationId(Long orgId, Long officeId)` - Find by office
- `findByOrgIdAndReportsToPersonId(Long orgId, Long managerId)` - Find direct reports
- `findByOrgIdAndDisplayNameContainingIgnoreCase(Long orgId, String search)` - Search by name
- `@Query` methods for complex searches (email, phone, name combinations)
- `findByOrgIdAndTagKeysContaining(Long orgId, String tagKey)` - Find by tag
- `findByOrgIdAndSkillKeysContaining(Long orgId, String skillKey)` - Find by skill

**ContactRepository**:

- `findByOrgIdAndArchivedAtIsNull(Long orgId, Pageable pageable)`
- `findByOrgIdAndPersonTypeAndArchivedAtIsNull(Long orgId, ContactPersonType personType)`
- `findByOrgIdAndEmail(Long orgId, String email)`
- `findByOrgIdAndIronworkerNumber(Long orgId, String number)`
- `findByOrgIdAndOfficeId(Long orgId, Long officeId)`
- `findByOrgIdAndReportsToContactId(Long orgId, Long managerId)` - Find direct reports

#### Organization Domain Repositories

**OfficeRepository**:

- `findByOrgIdAndArchivedAtIsNull(Long orgId)` - List active offices
- `findByOrgIdAndNormalizedName(Long orgId, String normalizedName)` - Find by normalized name
- `findByOrgIdAndParentOrgLocationId(Long orgId, Long parentId)` - Find children
- `findByOrgIdAndParentOrgLocationIdIsNull(Long orgId)` - Find root offices
- `findByOrgIdAndTagKeysContaining(Long orgId, String tagKey)` - Find by tag

**OrgTaxonomyRepository**:

- `findByOrgIdAndNamespace(Long orgId, String namespace)` - Find by namespace
- `findByOrgId(Long orgId)` - Find all taxonomies for org

**GraphEdgeRepository**:

- `findByOrgIdAndFromNodeTypeAndFromNodeId(Long orgId, GraphNodeType nodeType, Long nodeId)`
- `findByOrgIdAndToNodeTypeAndToNodeId(Long orgId, GraphNodeType nodeType, Long nodeId)`
- `findByOrgIdAndEdgeTypeKey(Long orgId, String edgeTypeKey)`
- `findByOrgIdAndArchivedAtIsNull(Long orgId)` - List active edges

#### Projects Domain Repositories

**ProjectRepository**:

- `findByOrgIdAndArchivedAtIsNull(Long orgId, Pageable pageable)`
- `findByOrgIdAndName(Long orgId, String name)` - Find by name (unique when not archived)
- `findByOrgIdAndProjectCode(Long orgId, String projectCode)` - Find by code (unique when not archived)
- `findByOrgIdAndOfficeId(Long orgId, Long officeId)` - Find by office
- `findByOrgIdAndNameContainingIgnoreCase(Long orgId, String search)` - Search by name

**EstimateRepository**:

- `findByOrgIdAndProjectIdAndArchivedAtIsNull(Long orgId, Long projectId)`
- `findByOrgIdAndProjectIdAndName(Long orgId, Long projectId, String name)` - Find by name (unique when not archived)
- `findByProjectId(Long projectId)` - Find all estimates for project
- `findByCreatedByUserId(Long userId)` - Find by creator

**SeatRepository**:

- `findByOrgIdAndSeatNumber(Long orgId, Integer seatNumber)` - Find by seat number (unique)
- `findByOrgIdAndStatus(Long orgId, SeatStatus status)` - Find by status
- `findByOrgIdAndRoleAndStatus(Long orgId, String role, SeatStatus status)`
- `findByOrgIdAndProjectId(Long orgId, Long projectId)` - Find by project
- `findByOrgIdAndUserId(Long orgId, Long userId)` - Find by user (unique when not null)
- `findByOrgIdAndStatusOrderBySeatNumber(Long orgId, SeatStatus status)` - List ordered

#### Companies Domain Repositories

**CompanyRepository**:

- `findByOrgIdAndArchivedAtIsNull(Long orgId, Pageable pageable)`
- `findByOrgIdAndNormalizedName(Long orgId, String normalizedName)` - Find by name (unique when not archived)
- `findByOrgIdAndExternalId(Long orgId, String externalId)` - Find by external ID (unique when not archived)
- `findByOrgIdAndNameContainingIgnoreCase(Long orgId, String search)` - Search by name
- `findByOrgIdAndCompanyTypeKeysContaining(Long orgId, String typeKey)` - Find by type
- `findByOrgIdAndTagKeysContaining(Long orgId, String tagKey)` - Find by tag

**CompanyLocationRepository**:

- `findByOrgIdAndCompanyIdAndArchivedAtIsNull(Long orgId, Long companyId)`
- `findByOrgIdAndCompanyIdAndNormalizedName(Long orgId, Long companyId, String normalizedName)` - Find by name (unique when not archived)
- `findByOrgIdAndCompanyIdAndExternalId(Long orgId, Long companyId, String externalId)` - Find by external ID
- `findByCompanyId(Long companyId)` - Find all locations for company
- `findByOrgIdAndTagKeysContaining(Long orgId, String tagKey)` - Find by tag

#### Cost Domain Repositories

**CostCodeRepository**:

- `findByOrgIdAndCode(Long orgId, String code)` - Find by code (unique)
- `findByOrgIdAndCategory(Long orgId, String category)` - Find by category
- `findByOrgIdAndActive(Long orgId, Boolean active)` - Find by active status
- `findByOrgIdAndIsUsed(Long orgId, Boolean isUsed)` - Find by usage
- `findByImportJobId(Long importJobId)` - Find by import job

**CostCodeImportJobRepository**:

- `findByOrgId(Long orgId)` - Find all jobs for org
- `findByOrgIdAndStatus(Long orgId, CostCodeImportStatus status)` - Find by status

#### Communication Domain Repositories

**EmailTemplateRepository**:

- `findByOrgIdAndNameAndLocale(Long orgId, String name, String locale)` - Find by name and locale (unique)
- `findByOrgIdAndName(Long orgId, String name)` - Find all locales for template

**ContactInquiryRepository**:

- `findByStatusOrderByCreatedAtDesc(ContactInquiryStatus status, Pageable pageable)`
- `findAllByOrderByCreatedAtDesc(Pageable pageable)` - List all ordered

**NotificationRepository**:

- `findByOrgIdAndUserIdAndReadOrderByCreatedAtDesc(Long orgId, Long userId, Boolean read, Pageable pageable)`
- `findByOrgIdAndUserIdOrderByCreatedAtDesc(Long orgId, Long userId, Pageable pageable)` - All for user
- `countByOrgIdAndUserIdAndReadFalse(Long orgId, Long userId)` - Count unread

#### Legal Domain Repositories

**LegalDocRepository**:

- `findByTypeAndVersion(LegalDocType type, String version)` - Find by type and version (unique)
- `findByTypeOrderByEffectiveAtDescCreatedAtDesc(LegalDocType type)` - Find latest by type

**LegalAcceptanceRepository**:

- `findByUserIdAndDocTypeAndVersion(Long userId, LegalDocType docType, String version)` - Find acceptance (unique)
- `findByUserIdAndDocType(Long userId, LegalDocType docType)` - Find all acceptances for doc type
- `findByOrgIdAndDocTypeAndVersion(Long orgId, LegalDocType docType, String version)` - Find by org

#### System Domain Repositories

**EventLogRepository**:

- `findByOrgIdOrderByCreatedAtDesc(Long orgId, Pageable pageable)` - List events
- `findByOrgIdAndEntityIdOrderByCreatedAtDesc(Long orgId, String entityId, Pageable pageable)` - Find by entity
- `findByOrgIdAndActionOrderByCreatedAtDesc(Long orgId, String action, Pageable pageable)` - Find by action
- `findByOrgIdAndEventTypeOrderByCreatedAtDesc(Long orgId, String eventType, Pageable pageable)` - Find by event type
- `findByOrgIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(Long orgId, String entityType, String entityId, Pageable pageable)`
- `findByCreatedAtBetween(LocalDateTime start, LocalDateTime end)` - Find by date range

**TenantMigrationRepository**:

- `findByOrgId(Long orgId)` - Find migrations for org
- `findByOrgIdAndStatus(Long orgId, MigrationStatus status)` - Find by status
- `findByStatus(MigrationStatus status)` - Find all by status (admin)

### Custom Query Methods

For complex queries that can't be expressed with method names, use `@Query` annotations:

```java
@Query("SELECT p FROM Person p WHERE p.orgId = :orgId AND " +
       "(LOWER(p.displayName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
       "LOWER(p.primaryEmail) LIKE LOWER(CONCAT('%', :search, '%')))")
List<Person> searchPersons(@Param("orgId") Long orgId, @Param("search") String search);
```

### Element Collection Queries

For `@ElementCollection` fields, create separate repository methods or use joins:

```java
@Query("SELECT p FROM Person p JOIN p.emails e WHERE p.orgId = :orgId AND e.normalized = :email")
Optional<Person> findByOrgIdAndEmailNormalized(@Param("orgId") Long orgId, @Param("email") String email);
```

### Repository Implementation Notes

1. **Soft Delete Pattern**: Always filter by `archivedAt IS NULL` unless explicitly including archived
2. **Organization Scoping**: All queries should include `orgId` parameter for multi-tenant isolation
3. **Pagination**: Use `Pageable` and return `Page<T>` for consistent pagination
4. **Unique Constraints**: Use `Optional<T>` return type for unique lookups
5. **Indexes**: Leverage database indexes defined in Flyway migrations for performance
6. **Custom Queries**: Use `@Query` with JPQL or native SQL for complex searches
7. **Specifications**: Consider using JPA Specifications for dynamic query building
8. **Projections**: Use DTO projections for read-only queries to improve performance

## Next Steps

1. Create domain package structure
2. Create embeddable classes for complex nested objects
3. Create enum classes for all TypeScript enums (organized by domain)
4. Create entity classes with proper JPA annotations
5. Add relationship mappings (@ManyToOne, @OneToMany)
6. Add indexes and constraints
7. Create Flyway migration scripts (V2-V10) for all tables, element collections, indexes, and foreign keys
8. Create Spring Data JPA repository interfaces for all entities with common and domain-specific query methods

