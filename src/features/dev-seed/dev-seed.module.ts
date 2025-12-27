import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { OrganizationSchema } from '../organizations/schemas/organization.schema';
import { OrganizationsModule } from '../organizations/organizations.module';
import { OrgTaxonomyModule } from '../org-taxonomy/org-taxonomy.module';
import { UserSchema } from '../users/schemas/user.schema';
import { DevSeedService } from './dev-seed.service';

@Module({
  imports: [
    OrganizationsModule,
    OrgTaxonomyModule,
    MongooseModule.forFeature([
      { name: 'Organization', schema: OrganizationSchema },
      { name: 'User', schema: UserSchema },
    ]),
  ],
  providers: [DevSeedService],
})
export class DevSeedModule {}
