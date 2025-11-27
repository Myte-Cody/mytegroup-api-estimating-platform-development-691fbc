import { Module } from '@nestjs/common'
import { SessionsService } from './sessions.service'
import { SessionsController } from './sessions.controller'
import { CommonModule } from '../../common/common.module'

@Module({
  imports: [CommonModule],
  providers: [SessionsService],
  controllers: [SessionsController],
  exports: [SessionsService],
})
export class SessionsModule {}
