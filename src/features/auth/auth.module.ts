import { Module } from '@nestjs/common';
import { CommonModule } from '../../common/common.module';
import { UsersModule } from '../users/users.module';
import { OrganizationsModule } from '../organizations/organizations.module';
import { EmailModule } from '../email/email.module';
import { LegalModule } from '../legal/legal.module';
import { WaitlistModule } from '../waitlist/waitlist.module';
import { AuthService } from './auth.service';
import { AuthController } from './auth.controller';
import { SessionsModule } from '../sessions/sessions.module';

@Module({
  imports: [CommonModule, UsersModule, OrganizationsModule, EmailModule, LegalModule, WaitlistModule, SessionsModule],
  providers: [AuthService],
  controllers: [AuthController],
})
export class AuthModule {}
