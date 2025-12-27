import { Injectable, Logger, OnModuleInit } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Role, mergeRoles } from '../../common/roles';
import { Organization } from '../organizations/schemas/organization.schema';
import { OrganizationsService } from '../organizations/organizations.service';
import { OrgTaxonomyService } from '../org-taxonomy/org-taxonomy.service';
import { User } from '../users/schemas/user.schema';

@Injectable()
export class DevSeedService implements OnModuleInit {
  private readonly logger = new Logger(DevSeedService.name);

  constructor(
    @InjectModel('Organization') private readonly orgModel: Model<Organization>,
    @InjectModel('User') private readonly userModel: Model<User>,
    private readonly orgs: OrganizationsService,
    private readonly taxonomy: OrgTaxonomyService
  ) {}

  async onModuleInit() {
    if (process.env.NODE_ENV === 'production') return;
    if (process.env.DISABLE_DEV_SEED === '1') return;

    try {
      const targetOrgName = (process.env.DEV_SEED_ORG_NAME || 'Myte Group Inc.').trim();
      if (!targetOrgName) return;

      let org = await this.orgModel.findOne({ name: targetOrgName, archivedAt: null });
      if (!org) {
        const created = await this.orgs.create({ name: targetOrgName });
        const createdId = created?.id || (created as any)?._id;
        org = createdId ? await this.orgModel.findById(createdId) : null;
      }
      if (!org) return;
      const orgId = org.id || (org as any)?._id;
      if (!orgId) return;

      const missingScope = await this.userModel.find({
        $and: [
          { $or: [{ role: Role.SuperAdmin }, { roles: Role.SuperAdmin }] },
          { $or: [{ orgId: { $exists: false } }, { orgId: null }, { orgId: '' }] },
        ],
      });
      if (missingScope.length) {
        for (const user of missingScope) {
          const roles = mergeRoles(user.role as Role, user.roles as Role[]);
          user.orgId = String(orgId);
          user.roles = Array.from(new Set([...roles, Role.SuperAdmin, Role.PlatformAdmin]));
          user.role = Role.SuperAdmin;
          await user.save();
        }

        if (!org.ownerUserId && missingScope[0]) {
          await this.orgs.setOwner(String(orgId), missingScope[0].id || (missingScope[0] as any)._id);
        }
      }

      const defaultCompanyTypeKeys = [
        'supplier',
        'supplier_mill',
        'supplier_mill_plate',
        'supplier_mill_beams',
        'supplier_mill_special',
        'supplier_mill_mixed',
        'supplier_hardware',
        'supplier_hardware_bolts',
        'supplier_hardware_welding_rods',
        'supplier_hardware_anchors',
        'supplier_hardware_rail',
        'supplier_hardware_grinding_disks',
        'supplier_hardware_cutting_disks',
        'supplier_hardware_sawzall_blades',
        'supplier_hardware_other',
        'supplier_paint',
        'supplier_ppe',
        'subcontractor',
        'subcontractor_erector',
        'subcontractor_transporter',
        'subcontractor_detailer',
        'subcontractor_engineer',
        'subcontractor_fabricator',
        'subcontractor_galvanizer',
        'subcontractor_painter',
        'subcontractor_field_qaqc',
        'subcontractor_shop_qaqc',
        'subcontractor_project_manager',
        'client',
      ];

      await this.taxonomy.ensureKeysActive(
        { userId: missingScope[0]?.id || (missingScope[0] as any)?._id, orgId: String(orgId), role: Role.SuperAdmin },
        String(orgId),
        'company_type',
        defaultCompanyTypeKeys
      );

      if (missingScope.length) {
        this.logger.log(`Assigned org scope (${String(orgId)}) to ${missingScope.length} superadmin user(s).`);
      }
    } catch (err) {
      this.logger.warn(`Dev seed skipped: ${(err as Error)?.message || err}`);
    }
  }
}
