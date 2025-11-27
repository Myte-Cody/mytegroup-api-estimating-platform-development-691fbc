import { Body, Controller, Post, Req, UseGuards } from '@nestjs/common';
import { EmailService } from './email.service';
import { SendEmailDto } from './dto/send-email.dto';
import { SessionGuard } from '../../common/guards/session.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { Role } from '../../common/roles';
import { OrgScopeGuard } from '../../common/guards/org-scope.guard';
import { Request } from 'express';
import { IsEmail } from 'class-validator';
import { plainToInstance } from 'class-transformer';
import { validateOrReject } from 'class-validator';

class PreviewEmailDto {
  @IsEmail()
  to: string;
}

@Controller('email')
export class EmailController {
  constructor(private readonly emailService: EmailService) {}

  @UseGuards(SessionGuard, OrgScopeGuard, RolesGuard)
  @Roles(Role.Admin, Role.SuperAdmin)
  @Post('send')
  send(@Body() dto: SendEmailDto, @Req() req: Request) {
    const orgId = req.session?.user?.orgId;
    return this.emailService.sendMail({ ...dto, orgId });
  }

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.SuperAdmin, Role.PlatformAdmin)
  @Post('preview')
  async preview(@Body() body: any) {
    const dto = plainToInstance(PreviewEmailDto, body);
    await validateOrReject(dto);
    return this.emailService.sendPreviewSet(dto.to);
  }
}
