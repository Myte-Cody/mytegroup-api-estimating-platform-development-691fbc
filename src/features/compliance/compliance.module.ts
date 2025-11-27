import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { ComplianceController } from './compliance.controller';
import { ComplianceService } from './compliance.service';
import { ContactSchema } from '../contacts/schemas/contact.schema';
import { InviteSchema } from '../invites/schemas/invite.schema';
import { ProjectSchema } from '../projects/schemas/project.schema';
import { UserSchema } from '../users/schemas/user.schema';

@Module({
  imports: [
    CommonModule,
    TenancyModule,
    MongooseModule.forFeature([
      { name: 'User', schema: UserSchema },
      { name: 'Contact', schema: ContactSchema },
      { name: 'Invite', schema: InviteSchema },
      { name: 'Project', schema: ProjectSchema },
    ]),
  ],
  controllers: [ComplianceController],
  providers: [ComplianceService],
  exports: [ComplianceService],
})
export class ComplianceModule {}
