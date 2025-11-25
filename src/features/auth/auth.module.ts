import { Module } from '@nestjs/common';
import { CommonModule } from '../../common/common.module';
import { UsersModule } from '../users/users.module';
import { OrganizationsModule } from '../organizations/organizations.module';
import { EmailModule } from '../email/email.module';
import { AuthService } from './auth.service';
import { AuthController } from './auth.controller';

@Module({
  imports: [CommonModule, UsersModule, OrganizationsModule, EmailModule],
  providers: [AuthService],
  controllers: [AuthController],
})
export class AuthModule {}
