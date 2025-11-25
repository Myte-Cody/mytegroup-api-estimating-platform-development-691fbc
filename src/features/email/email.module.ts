import { Module } from '@nestjs/common';
import { CommonModule } from '../../common/common.module';
import { EmailController } from './email.controller';
import { EmailService } from './email.service';
@Module({
  imports: [CommonModule],
  controllers: [EmailController],
  providers: [EmailService],
  exports: [EmailService],
})
export class EmailModule {}
