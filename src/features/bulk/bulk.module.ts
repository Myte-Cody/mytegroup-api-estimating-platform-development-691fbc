import { MiddlewareConsumer, Module, NestModule, RequestMethod } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import * as rateLimit from 'express-rate-limit';
import { CommonModule } from '../../common/common.module';
import { TenancyModule } from '../../common/tenancy/tenancy.module';
import { ContactSchema } from '../contacts/schemas/contact.schema';
import { OfficeSchema } from '../offices/schemas/office.schema';
import { ProjectSchema } from '../projects/schemas/project.schema';
import { UserSchema } from '../users/schemas/user.schema';
import { BulkController } from './bulk.controller';
import { BulkService } from './bulk.service';
import { getRateLimitExceededBody } from './bulk.rate-limit';

@Module({
  imports: [
    CommonModule,
    TenancyModule,
    MongooseModule.forFeature([
      { name: 'User', schema: UserSchema },
      { name: 'Contact', schema: ContactSchema },
      { name: 'Project', schema: ProjectSchema },
      { name: 'Office', schema: OfficeSchema },
    ]),
  ],
  controllers: [BulkController],
  providers: [BulkService],
  exports: [BulkService],
})
export class BulkModule implements NestModule {
  configure(consumer: MiddlewareConsumer) {
    const limiter = rateLimit({
      windowMs: 15 * 60 * 1000,
      max: 10,
      standardHeaders: true,
      legacyHeaders: false,
      message: getRateLimitExceededBody(),
    });
    consumer
      .apply(limiter)
      .forRoutes(
        { path: 'bulk-import/:entityType', method: RequestMethod.ALL },
        { path: 'bulk-export/:entityType', method: RequestMethod.ALL }
      );
  }
}
