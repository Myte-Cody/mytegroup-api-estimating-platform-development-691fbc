import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { CommonModule } from '../../common/common.module';
import { ProjectsController } from './projects.controller';
import { ProjectsService } from './projects.service';
import { ProjectSchema } from './schemas/project.schema';
import { OrganizationSchema } from '../organizations/schemas/organization.schema';
import { OfficeSchema } from '../offices/schemas/office.schema';

@Module({
  imports: [
    CommonModule,
    MongooseModule.forFeature([
      { name: 'Project', schema: ProjectSchema },
      { name: 'Organization', schema: OrganizationSchema },
      { name: 'Office', schema: OfficeSchema },
    ]),
  ],
  controllers: [ProjectsController],
  providers: [ProjectsService],
  exports: [ProjectsService],
})
export class ProjectsModule {}
