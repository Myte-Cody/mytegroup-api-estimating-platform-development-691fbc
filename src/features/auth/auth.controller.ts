import { Controller, Post, Body, Req, Get, UseGuards, Optional } from '@nestjs/common';
import { Request } from 'express';
import { AuthService } from './auth.service';
import { LoginDto } from './dto/login.dto';
import { SessionGuard } from '../../common/guards/session.guard';
import { RegisterDto } from './dto/register.dto';
import { Roles } from '../../common/decorators/roles.decorator';
import { RolesGuard } from '../../common/guards/roles.guard';
import { VerifyEmailDto } from './dto/verify-email.dto';
import { ForgotPasswordDto } from './dto/forgot-password.dto';
import { ResetPasswordDto } from './dto/reset-password.dto';
import { Role } from '../../common/roles';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { PasswordStrengthDto } from './dto/password-strength.dto';
import { LegalService } from '../legal/legal.service';
import { SessionsService } from '../sessions/sessions.service';
@Controller('auth')
export class AuthController {
  constructor(private auth: AuthService, private legal: LegalService, @Optional() private sessions?: SessionsService) {}

  private async regenerateSession(req: Request) {
    return new Promise<void>((resolve, reject) => {
      req.session.regenerate((err) => {
        if (err) return reject(err);
        resolve();
      });
    });
  }

  @Post('register')
  async register(@Body() dto: RegisterDto, @Req() req: Request) {
    const user = await this.auth.register(dto);
    await this.regenerateSession(req);
    (req as any).session.user = {
      id: (user as any)._id || (user as any).id,
      role: user.role,
      roles: (user as any).roles || [user.role],
      orgId: user.organizationId,
      isOrgOwner: (user as any).isOrgOwner,
    };
    await this.sessions?.registerSession(req.sessionID, (user as any)._id || (user as any).id);
    const status = await this.legal.acceptanceStatus({ id: (user as any).id, orgId: user.organizationId });
    return { user, legalRequired: status.required.length > 0, legalRequiredDocs: status.required };
  }

  @Post('login')
  async login(@Body() body: LoginDto, @Req() req: Request) {
    const user = await this.auth.login(body.email, body.password);
    await this.regenerateSession(req);
    const normalizedUser: any = {
      ...user,
      id: (user as any)._id || (user as any).id,
    };
    // session-based auth for dev; cookie set by express-session middleware
    (req as any).session.user = {
      id: normalizedUser.id,
      role: normalizedUser.role,
      roles: normalizedUser.roles || [normalizedUser.role],
      orgId: normalizedUser.organizationId,
      isOrgOwner: normalizedUser.isOrgOwner,
    };
    await this.sessions?.registerSession(req.sessionID, normalizedUser.id);
    const status = await this.legal.acceptanceStatus({ id: normalizedUser.id, orgId: normalizedUser.organizationId });
    return { user: normalizedUser, legalRequired: status.required.length > 0, legalRequiredDocs: status.required };
  }

  @UseGuards(SessionGuard)
  @Post('logout')
  async logout(@Req() req: Request) {
    return new Promise((resolve) => {
      const userId = req.session.user?.id;
      const sessionId = req.sessionID;
      req.session.destroy(async () => {
        if (userId && sessionId) {
          await this.sessions?.removeSession(sessionId, userId);
        }
        resolve({ status: 'ok' });
      });
    });
  }

  @UseGuards(SessionGuard)
  @Get('me')
  me(@Req() req: Request) {
    const user = req.session.user;
    return { user, legalRequired: (req as any).legalRequired || [], legalStatus: (req as any).legalStatus };
  }

  @Post('verify-email')
  async verifyEmail(@Body() dto: VerifyEmailDto) {
    return this.auth.verifyEmail(dto);
  }

  @Post('password-strength')
  async passwordStrength(@Body() dto: PasswordStrengthDto) {
    return this.auth.passwordStrength(dto.password);
  }

  @Post('forgot-password')
  async forgotPassword(@Body() dto: ForgotPasswordDto) {
    return this.auth.forgotPassword(dto);
  }

  @Post('reset-password')
  async resetPassword(@Body() dto: ResetPasswordDto) {
    return this.auth.resetPassword(dto);
  }

  // Roles: admin, org owner, superadmin (tenant scoped)
  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.OrgAdmin, Role.SuperAdmin, Role.OrgOwner, Role.PlatformAdmin)
  @Get('users')
  async listUsers(@Req() req: Request) {
    const orgId = req.session.user?.orgId;
    return this.auth.listUsers(orgId, req.session?.user);
  }
}
