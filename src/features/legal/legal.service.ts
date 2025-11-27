import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { FilterQuery, Model } from 'mongoose';
import { AuditLogService } from '../../common/services/audit-log.service';
import { LegalDocType } from './legal.types';
import { CreateLegalDocDto } from './dto/create-legal-doc.dto';
import { AcceptLegalDocDto } from './dto/accept-legal-doc.dto';
import { LegalDoc } from './schemas/legal-doc.schema';
import { LegalAcceptance } from './schemas/legal-acceptance.schema';

type Actor = { id?: string; orgId?: string };

@Injectable()
export class LegalService {
  constructor(
    @InjectModel('LegalDoc') private readonly docModel: Model<LegalDoc>,
    @InjectModel('LegalAcceptance') private readonly acceptanceModel: Model<LegalAcceptance>,
    private readonly audit: AuditLogService
  ) {}

  async createDoc(dto: CreateLegalDocDto, actor: Actor) {
    const exists = await this.docModel.findOne({ type: dto.type, version: dto.version });
    if (exists) {
      throw new BadRequestException('Version already exists for this legal document type');
    }
    const doc = await this.docModel.create({
      type: dto.type,
      version: dto.version,
      content: dto.content,
      effectiveAt: dto.effectiveAt || new Date(),
      archivedAt: null,
    });
    await this.audit.log({
      eventType: 'legal.updated',
      entity: 'LegalDoc',
      entityId: doc.id,
      orgId: actor.orgId,
      userId: actor.id,
      metadata: { type: doc.type, version: doc.version },
    });
    return doc.toObject ? doc.toObject() : doc;
  }

  async latestDoc(type: LegalDocType) {
    const doc = await this.docModel
      .findOne({ type, archivedAt: null } as FilterQuery<LegalDoc>)
      .sort({ effectiveAt: -1, createdAt: -1 })
      .lean();
    if (!doc) {
      throw new NotFoundException('Legal document not found');
    }
    return doc;
  }

  async history(type: LegalDocType, limit = 10) {
    const docs = await this.docModel
      .find({ type } as FilterQuery<LegalDoc>)
      .sort({ effectiveAt: -1, createdAt: -1 })
      .limit(limit)
      .lean();
    return docs;
  }

  async acceptanceStatus(user: Actor) {
    const types = Object.values(LegalDocType);
    const latest = await Promise.all(
      types.map(async (type) => {
        try {
          const doc = await this.latestDoc(type as LegalDocType);
          return doc;
        } catch {
          return null;
        }
      })
    );
    const accepted = await this.acceptanceModel
      .find({ userId: user.id } as FilterQuery<LegalAcceptance>)
      .lean();
    const acceptedMap = new Map<string, string>();
    accepted.forEach((acc) => acceptedMap.set(`${acc.docType}`, acc.version));

    const required: { type: LegalDocType; version: string }[] = [];
    latest.forEach((doc) => {
      if (!doc) return;
      const key = `${doc.type}`;
      if (acceptedMap.get(key) !== doc.version) {
        required.push({ type: doc.type as LegalDocType, version: doc.version });
      }
    });
    return {
      required,
      acceptedVersions: Object.fromEntries(acceptedMap),
      latest: latest.filter(Boolean),
    };
  }

  async accept(dto: AcceptLegalDocDto, actor: Actor, meta?: { ipAddress?: string; userAgent?: string }) {
    if (!actor.id) {
      throw new BadRequestException('User required to accept legal documents');
    }
    const doc =
      dto.version && dto.docType
        ? await this.docModel.findOne({ type: dto.docType, version: dto.version })
        : await this.docModel
            .findOne({ type: dto.docType, archivedAt: null } as FilterQuery<LegalDoc>)
            .sort({ effectiveAt: -1, createdAt: -1 });
    if (!doc) {
      throw new NotFoundException('Legal document not found');
    }

    const existing = await this.acceptanceModel.findOne({
      userId: actor.id,
      docType: doc.type,
      version: doc.version,
    });
    if (existing) {
      return existing.toObject ? existing.toObject() : existing;
    }

    const acceptance = await this.acceptanceModel.create({
      userId: actor.id,
      orgId: actor.orgId,
      docType: doc.type,
      version: doc.version,
      ipAddress: meta?.ipAddress,
      userAgent: meta?.userAgent,
      archivedAt: null,
      piiStripped: false,
      legalHold: false,
    });

    await this.audit.log({
      eventType: 'legal.accepted',
      entity: 'LegalAcceptance',
      entityId: acceptance.id,
      orgId: actor.orgId,
      userId: actor.id,
      metadata: { docType: doc.type, version: doc.version },
      payload: { ip: meta?.ipAddress, userAgent: meta?.userAgent },
    });

    return acceptance.toObject ? acceptance.toObject() : acceptance;
  }
}
