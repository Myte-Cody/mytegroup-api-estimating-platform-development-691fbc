import { Body, Controller, Post, UseGuards } from '@nestjs/common';
import { EmailService } from './email.service';
import { SendEmailDto } from './dto/send-email.dto';
import { SessionGuard } from '../../common/guards/session.guard';
import { RolesGuard } from '../../common/guards/roles.guard';
import { Roles } from '../../common/decorators/roles.decorator';
import { Role } from '../../common/roles';

@Controller('email')
export class EmailController {
  constructor(private readonly emailService: EmailService) {}

  @UseGuards(SessionGuard, RolesGuard)
  @Roles(Role.Admin, Role.SuperAdmin)
  @Post('send')
  send(@Body() dto: SendEmailDto) {
    return this.emailService.sendMail(dto);
  }
}
