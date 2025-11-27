import { Injectable } from '@nestjs/common'
import { createRedisClient } from '../../common/redis/redis.client'
import { AuditLogService } from '../../common/services/audit-log.service'

const SESSION_PREFIX = 'sess:'
const USER_SESSION_SET_PREFIX = 'user:sessions:'

type SessionRecord = {
  cookie?: any
  user?: { id?: string }
  meta?: {
    ip?: string
    userAgent?: string
    createdAt?: string
    lastSeen?: string
  }
}

@Injectable()
export class SessionsService {
  private readonly client = createRedisClient('sessions')

  constructor(private readonly audit: AuditLogService) {}

  private async ensureConnected() {
    if ((this.client as any).status === 'wait' || (this.client as any).status === 'close') {
      await this.client.connect()
    }
  }

  private keyForSession(sessionId: string) {
    return `${SESSION_PREFIX}${sessionId}`
  }

  private keyForUserSet(userId: string) {
    return `${USER_SESSION_SET_PREFIX}${userId}`
  }

  async registerSession(sessionId: string, userId: string) {
    if (!sessionId || !userId) return
    await this.ensureConnected()
    await this.client.sadd(this.keyForUserSet(userId), sessionId)
  }

  async removeSession(sessionId: string, userId?: string) {
    if (!sessionId) return
    await this.ensureConnected()
    const pipeline = this.client.multi()
    pipeline.del(this.keyForSession(sessionId))
    if (userId) {
      pipeline.srem(this.keyForUserSet(userId), sessionId)
    }
    await pipeline.exec()
    await this.audit.log({
      eventType: 'session.revoked',
      userId,
      metadata: { sessionId },
    })
  }

  async listSessions(userId: string, currentSessionId?: string) {
    if (!userId) return []
    await this.ensureConnected()
    const sessionIds = await this.client.smembers(this.keyForUserSet(userId))
    const records: Array<{ id: string; isCurrent: boolean; meta?: any; createdAt?: string; lastSeen?: string }> = []
    for (const sid of sessionIds) {
      const raw = await this.client.get(this.keyForSession(sid))
      if (!raw) {
        continue
      }
      let parsed: SessionRecord | null = null
      try {
        parsed = JSON.parse(raw)
      } catch {
        parsed = null
      }
      records.push({
        id: sid,
        isCurrent: sid === currentSessionId,
        meta: parsed?.meta,
        createdAt: parsed?.meta?.createdAt,
        lastSeen: parsed?.meta?.lastSeen,
      })
    }
    return records
  }

  async revokeSession(sessionId: string, userId: string) {
    await this.removeSession(sessionId, userId)
    return { status: 'revoked' }
  }

  async revokeAll(userId: string) {
    if (!userId) return { status: 'ok' }
    await this.ensureConnected()
    const key = this.keyForUserSet(userId)
    const sessionIds = await this.client.smembers(key)
    const pipeline = this.client.multi()
    for (const sid of sessionIds) {
      pipeline.del(this.keyForSession(sid))
      pipeline.srem(key, sid)
    }
    await pipeline.exec()
    await this.audit.log({
      eventType: 'session.revoked_all',
      userId,
      metadata: { count: sessionIds.length },
    })
    return { status: 'revoked' }
  }
}
