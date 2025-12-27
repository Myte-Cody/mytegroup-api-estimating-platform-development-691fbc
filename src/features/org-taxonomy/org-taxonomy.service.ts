import { BadRequestException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Role } from '../../common/roles';
import { AuditLogService } from '../../common/services/audit-log.service';
import { TenantConnectionService } from '../../common/tenancy/tenant-connection.service';
import { normalizeKey } from '../../common/utils/normalize.util';
import { Organization } from '../organizations/schemas/organization.schema';
import { OrgTaxonomy, OrgTaxonomySchema, OrgTaxonomyValue } from './schemas/org-taxonomy.schema';
import { PutOrgTaxonomyValueDto } from './dto/put-org-taxonomy.dto';

type ActorContext = { userId?: string; orgId?: string; role?: Role };

const RESERVED_EDGE_TYPE_KEYS = [
  'depends_on',
  'supports',
  'works_with',
  'primary_for',
  'assigned_to',
  'reports_to',
  'belongs_to',
];

@Injectable()
export class OrgTaxonomyService {
  constructor(
    private readonly audit: AuditLogService,
    private readonly tenants: TenantConnectionService,
    @InjectModel('OrgTaxonomy') private readonly taxonomyModel: Model<OrgTaxonomy>,
    @InjectModel('Organization') private readonly orgModel: Model<Organization>
  ) {}

  private async model(orgId: string) {
    return this.tenants.getModelForOrg<OrgTaxonomy>(orgId, 'OrgTaxonomy', OrgTaxonomySchema, this.taxonomyModel);
  }

  private normalizeNamespace(namespace: string) {
    const normalized = normalizeKey(namespace);
    if (!normalized) throw new BadRequestException('namespace is required');
    return normalized;
  }

  private reservedKeysForNamespace(namespace: string) {
    return namespace === 'edge_type' ? RESERVED_EDGE_TYPE_KEYS : [];
  }

  private defaultLabelFromKey(key: string) {
    return String(key || '')
      .split('_')
      .filter(Boolean)
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  private ensureOrgScope(orgId: string, actor: ActorContext) {
    const privileged = actor.role === Role.SuperAdmin || actor.role === Role.PlatformAdmin;
    if (privileged) return;
    if (!actor.orgId || actor.orgId !== orgId) {
      throw new ForbiddenException('Cannot access taxonomy outside your organization');
    }
  }

  private async validateOrg(orgId: string) {
    const org = await this.orgModel.findOne({ _id: orgId, archivedAt: null });
    if (!org) throw new NotFoundException('Organization not found or archived');
    if (org.legalHold) {
      throw new ForbiddenException('Organization is under legal hold');
    }
  }

  private ensureReservedEdgeTypes(values: OrgTaxonomyValue[], reservedKeys: string[]) {
    if (!reservedKeys.length) return values;
    const existing = new Map(values.map((v) => [v.key, v]));
    reservedKeys.forEach((key) => {
      if (!existing.has(key)) {
        values.push({
          key,
          label: this.defaultLabelFromKey(key),
          sortOrder: null,
          color: null,
          metadata: {},
          archivedAt: null,
        });
      }
    });
    return values;
  }

  async ensureKeysActive(actor: ActorContext, orgId: string, namespace: string, keys: string[]) {
    const normalizedNamespace = this.normalizeNamespace(namespace);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);

    const normalizedKeys = Array.from(
      new Set((keys || []).map((key) => normalizeKey(key)).filter((key) => !!key))
    );
    if (!normalizedKeys.length) return [];

    const model = await this.model(orgId);
    const doc = await model.findOne({ orgId, namespace: normalizedNamespace });

    const reservedKeys = this.reservedKeysForNamespace(normalizedNamespace);

    if (!doc) {
      await model.create({
        orgId,
        namespace: normalizedNamespace,
        values: this.ensureReservedEdgeTypes(
          normalizedKeys.map((key) => ({
            key,
            label: this.defaultLabelFromKey(key),
            sortOrder: null,
            color: null,
            metadata: {},
            archivedAt: null,
          })),
          reservedKeys
        ),
      });
      return normalizedKeys;
    }

    const values: OrgTaxonomyValue[] = Array.isArray((doc as any).values) ? (doc as any).values : [];
    const byKey = new Map<string, OrgTaxonomyValue>(values.map((value) => [value.key, value]));

    let changed = false;
    normalizedKeys.forEach((key) => {
      const existing = byKey.get(key);
      if (existing) {
        if (existing.archivedAt) {
          existing.archivedAt = null;
          changed = true;
        }
        if (!existing.label) {
          existing.label = this.defaultLabelFromKey(key);
          changed = true;
        }
        return;
      }
      values.push({
        key,
        label: this.defaultLabelFromKey(key),
        sortOrder: null,
        color: null,
        metadata: {},
        archivedAt: null,
      });
      changed = true;
    });

    const beforeLength = values.length;
    this.ensureReservedEdgeTypes(values, reservedKeys);
    if (values.length !== beforeLength) changed = true;

    if (changed) {
      (doc as any).values = values;
      await doc.save();
    }

    return normalizedKeys;
  }

  async get(actor: ActorContext, orgId: string, namespace: string) {
    const normalizedNamespace = this.normalizeNamespace(namespace);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);
    const model = await this.model(orgId);
    const existing = await model.findOne({ orgId, namespace: normalizedNamespace });

    const reservedKeys = this.reservedKeysForNamespace(normalizedNamespace);
    if (!existing) {
      const created = await model.create({
        orgId,
        namespace: normalizedNamespace,
        values: this.ensureReservedEdgeTypes([], reservedKeys),
      });
      return created.toObject ? created.toObject() : created;
    }

    if (reservedKeys.length) {
      const raw = existing.toObject ? existing.toObject() : (existing as any);
      const nextValues = this.ensureReservedEdgeTypes(raw.values || [], reservedKeys);
      if (nextValues.length !== (raw.values || []).length) {
        existing.set({ values: nextValues });
        await existing.save();
      }
    }

    return existing.toObject ? existing.toObject() : existing;
  }

  async put(actor: ActorContext, orgId: string, namespace: string, desired: PutOrgTaxonomyValueDto[]) {
    const normalizedNamespace = this.normalizeNamespace(namespace);
    this.ensureOrgScope(orgId, actor);
    await this.validateOrg(orgId);

    const model = await this.model(orgId);
    const existing = await model.findOne({ orgId, namespace: normalizedNamespace });
    const before = existing?.toObject ? existing.toObject() : existing;

    const reservedKeys = this.reservedKeysForNamespace(normalizedNamespace);

    const desiredByKey = new Map<string, PutOrgTaxonomyValueDto>();
    (desired || []).forEach((item) => {
      const key = normalizeKey(item.key);
      if (!key) return;
      if (desiredByKey.has(key)) {
        throw new BadRequestException(`Duplicate taxonomy key: ${key}`);
      }
      desiredByKey.set(key, { ...item, key });
    });

    const activeKeysInOrder = Array.from(desiredByKey.keys());
    const reservedMissing = reservedKeys.filter((key) => !desiredByKey.has(key));
    const existingByKey = new Map<string, OrgTaxonomyValue>(((before as any)?.values || []).map((v: any) => [v.key, v]));

    const nextValues: OrgTaxonomyValue[] = [];

    const upsertActive = (key: string) => {
      const input = desiredByKey.get(key);
      if (!input) return;
      const prev = existingByKey.get(key);
      nextValues.push({
        key,
        label: (input.label || '').trim() || prev?.label || this.defaultLabelFromKey(key),
        sortOrder: typeof input.sortOrder === 'number' ? input.sortOrder : prev?.sortOrder ?? null,
        color: input.color?.trim?.() || prev?.color || null,
        metadata: input.metadata ?? prev?.metadata ?? {},
        archivedAt: null,
      });
    };

    activeKeysInOrder.forEach(upsertActive);

    reservedMissing.forEach((key) => {
      const prev = existingByKey.get(key);
      nextValues.push({
        key,
        label: prev?.label || this.defaultLabelFromKey(key),
        sortOrder: prev?.sortOrder ?? null,
        color: prev?.color ?? null,
        metadata: prev?.metadata ?? {},
        archivedAt: null,
      });
    });

    const now = new Date();
    existingByKey.forEach((prev, key) => {
      if (desiredByKey.has(key)) return;
      if (reservedKeys.includes(key)) return;
      nextValues.push({
        key,
        label: prev.label,
        sortOrder: prev.sortOrder ?? null,
        color: prev.color ?? null,
        metadata: prev.metadata ?? {},
        archivedAt: prev.archivedAt || now,
      });
    });

    const doc = existing || new (model as any)({ orgId, namespace: normalizedNamespace, values: [] });
    doc.values = nextValues;
    await doc.save();

    await this.audit.log({
      eventType: 'org_taxonomy.updated',
      orgId,
      userId: actor.userId,
      entity: 'OrgTaxonomy',
      entityId: doc.id,
      metadata: {
        namespace: normalizedNamespace,
        activeKeys: activeKeysInOrder.length,
        archivedKeys: nextValues.filter((v) => !!v.archivedAt).length,
        reservedKeys: reservedKeys.length,
      },
    });

    return doc.toObject ? doc.toObject() : doc;
  }
}
