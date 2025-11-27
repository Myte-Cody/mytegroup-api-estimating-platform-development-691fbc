import { Module } from '@nestjs/common'
import { MongooseModule } from '@nestjs/mongoose'
import { CommonModule } from '../../common/common.module'
import { EmailModule } from '../email/email.module'
import { WaitlistController } from './waitlist.controller'
import { WaitlistScheduler } from './waitlist.scheduler'
import { WaitlistService } from './waitlist.service'
import { WaitlistSchema } from './waitlist.schema'
import { UsersModule } from '../users/users.module'

@Module({
  imports: [CommonModule, EmailModule, UsersModule, MongooseModule.forFeature([{ name: 'Waitlist', schema: WaitlistSchema }])],
  controllers: [WaitlistController],
  providers: [WaitlistService, WaitlistScheduler],
  exports: [WaitlistService],
})
export class WaitlistModule {}
