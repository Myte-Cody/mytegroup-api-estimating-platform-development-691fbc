import { strict as assert } from 'assert'
import { describe, it, after, afterEach, before } from 'node:test'
import mongoose, { Model } from 'mongoose'
import { MongoMemoryServer } from 'mongodb-memory-server'
import { ConflictException } from '@nestjs/common'
import { Organization, OrganizationSchema } from '../src/features/organizations/schemas/organization.schema'
import { OrganizationsService } from '../src/features/organizations/organizations.service'
import { AuditLogService } from '../src/common/services/audit-log.service'
import { TenantConnectionService } from '../src/common/tenancy/tenant-connection.service'
import { normalizeDomainFromEmail } from '../src/common/utils/domain.util'
import { SeatsService } from '../src/features/seats/seats.service'

class AuditStub implements Partial<AuditLogService> {
  async log() {
    return
  }
}

class TenantStub implements Partial<TenantConnectionService> {
  async testConnection() {
    return
  }
  async resetConnectionForOrg() {
    return
  }
}

class SeatsStub implements Partial<SeatsService> {
  async ensureOrgSeats() {
    return
  }
}

describe('Domain gating', () => {
  let mongod: MongoMemoryServer
  let orgModel: Model<Organization>
  let orgs: OrganizationsService

  before(async () => {
    mongod = await MongoMemoryServer.create()
    const uri = mongod.getUri()
    await mongoose.connect(uri)
    orgModel = mongoose.model<Organization>('Organization', OrganizationSchema)
    orgs = new OrganizationsService(orgModel, new AuditStub() as any, new TenantStub() as any, new SeatsStub() as any)
  })

  after(async () => {
    await mongoose.disconnect()
    await mongod.stop()
  })

  afterEach(async () => {
    await orgModel.deleteMany({})
  })

  it('normalizes domains from emails', () => {
    assert.equal(normalizeDomainFromEmail('User@Example.com'), 'example.com')
    assert.equal(normalizeDomainFromEmail('bad'), null)
  })

  it('persists normalized primaryDomain and enforces uniqueness', async () => {
    const org = await orgs.create({ name: 'Org1', primaryDomain: 'Example.com' })
    assert.equal(org.primaryDomain, 'example.com')

    let threw = false
    try {
      await orgs.create({ name: 'Org2', primaryDomain: 'example.com' })
    } catch (err) {
      threw = err instanceof ConflictException
    }
    assert.equal(threw, true)
  })
})
