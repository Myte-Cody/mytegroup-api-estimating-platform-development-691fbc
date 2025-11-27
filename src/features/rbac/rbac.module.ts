import { Module } from '@nestjs/common';
import { CommonModule } from '../../common/common.module';
import { UsersModule } from '../users/users.module';
import { RbacController } from './rbac.controller';
import { RbacService } from './rbac.service';

@Module({
  imports: [CommonModule, UsersModule],
  controllers: [RbacController],
  providers: [RbacService],
  exports: [RbacService],
})
export class RbacModule {}
