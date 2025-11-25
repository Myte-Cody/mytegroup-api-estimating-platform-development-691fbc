import { ConflictException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { AuditLogService } from '../../common/services/audit-log.service';
import { CreateOrganizationDto } from './dto/create-organization.dto';
import { UpdateOrganizationDatastoreDto } from './dto/update-organization-datastore.dto';
import { Organization } from './schemas/organization.schema';

@Injectable()
export class OrganizationsService {
  constructor(
    @InjectModel('Organization') private readonly orgModel: Model<Organization>,
    private readonly audit: AuditLogService
  ) {}

  async create(dto: CreateOrganizationDto) {
    const existing = await this.orgModel.findOne({ name: dto.name });
    if (existing) throw new ConflictException('Organization name already in use');

    const org = await this.orgModel.create({
      name: dto.name,
      useDedicatedDb: dto.useDedicatedDb ?? false,
      databaseUri: dto.databaseUri || null,
      databaseName: dto.databaseName || null,
      dataResidency: dto.dataResidency || 'shared',
    });
    await this.audit.log({
      eventType: 'organization.created',
      orgId: org.id,
      entity: 'Organization',
      entityId: org.id,
    });
    return org.toObject();
  }

  async setOwner(orgId: string, userId: string) {
    const org = await this.orgModel.findById(orgId);
    if (!org) throw new NotFoundException('Organization not found');
    org.ownerUserId = userId;
    org.createdByUserId = org.createdByUserId || userId;
    await org.save();
    await this.audit.log({
      eventType: 'organization.owner_assigned',
      orgId: org.id,
      entity: 'Organization',
      entityId: org.id,
      metadata: { ownerUserId: userId },
    });
    return org.toObject();
  }

  async updateDatastore(id: string, dto: UpdateOrganizationDatastoreDto) {
    const org = await this.orgModel.findById(id);
    if (!org) throw new NotFoundException('Organization not found');
    org.useDedicatedDb = dto.useDedicatedDb ?? org.useDedicatedDb;
    org.databaseUri = dto.databaseUri ?? org.databaseUri;
    org.databaseName = dto.databaseName ?? org.databaseName;
    org.dataResidency = dto.dataResidency ?? org.dataResidency;
    await org.save();
    await this.audit.log({
      eventType: 'organization.datastore_updated',
      orgId: org.id,
      entity: 'Organization',
      entityId: org.id,
      metadata: {
        useDedicatedDb: org.useDedicatedDb,
        databaseUriSet: !!org.databaseUri,
        databaseName: org.databaseName,
        dataResidency: org.dataResidency,
      },
    });
    return org.toObject();
  }

  async findById(id: string) {
    const org = await this.orgModel.findById(id);
    if (!org) throw new NotFoundException('Organization not found');
    return org.toObject();
  }

  async findAll() {
    return this.orgModel.find({ archivedAt: null });
  }

  async archive(id: string) {
    const org = await this.orgModel.findById(id);
    if (!org) throw new NotFoundException('Organization not found');
    if (org.archivedAt) return org.toObject();
    org.archivedAt = new Date();
    await org.save();
    await this.audit.log({
      eventType: 'organization.archived',
      orgId: org.id,
      entity: 'Organization',
      entityId: org.id,
    });
    return org.toObject();
  }

  async unarchive(id: string) {
    const org = await this.orgModel.findById(id);
    if (!org) throw new NotFoundException('Organization not found');
    if (!org.archivedAt) return org.toObject();
    org.archivedAt = null;
    await org.save();
    await this.audit.log({
      eventType: 'organization.unarchived',
      orgId: org.id,
      entity: 'Organization',
      entityId: org.id,
    });
    return org.toObject();
  }
}
