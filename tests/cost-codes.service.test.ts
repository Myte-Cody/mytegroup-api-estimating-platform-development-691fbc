process.env.REDIS_DISABLED = '1'

const assert = require('node:assert/strict')
const { describe, it } = require('node:test')
const { BadRequestException } = require('@nestjs/common')
const { Role } = require('../src/common/roles')
const { CostCodesService } = require('../src/features/cost-codes/cost-codes.service.ts')
const { STEEL_FIELD_PACK } = require('../src/features/cost-codes/seed/steel-field-pack')

const createCostCodeModel = (initial = []) => {
  const records = initial.map((rec) => ({ ...rec }))
  return {
    records,
    async countDocuments(filter) {
      return records.filter((rec) => {
        return Object.entries(filter).every(([key, value]) => rec[key] === value)
      }).length
    },
    async deleteMany(filter) {
      for (let i = records.length - 1; i >= 0; i -= 1) {
        if (Object.entries(filter).every(([key, value]) => records[i][key] === value)) {
          records.splice(i, 1)
        }
      }
      return { deletedCount: 0 }
    },
    async insertMany(docs) {
      docs.forEach((doc) => records.push({ ...doc }))
      return docs
    },
  }
}

const createService = ({ codes = [], org = {} } = {}) => {
  const costCodeModel = createCostCodeModel(codes)
  const importJobModel = {}
  const orgModel = {
    async findOne(filter) {
      if (filter._id !== 'org-1') return null
      if (filter.archivedAt !== null) return null
      return {
        _id: 'org-1',
        archivedAt: null,
        legalHold: false,
        ...org,
      }
    },
  }
  const tenants = {
    async getModelForOrg() {
      return costCodeModel
    },
  }
  const auditLog = []
  const audit = {
    async logMutation(entry) {
      auditLog.push(entry)
    },
  }
  const ai = {
    isEnabled() {
      return true
    },
  }

  const service = new CostCodesService(audit, tenants, ai, costCodeModel, importJobModel, orgModel)
  return { service, costCodeModel, auditLog }
}

describe('CostCodesService seed pack', () => {
  it('seeds the steel field pack by replacing existing codes', async () => {
    const { service, costCodeModel } = createService({
      codes: [{ orgId: 'org-1', code: 'OLD', description: 'Old', category: 'Legacy', isUsed: false }],
    })

    const result = await service.seedPack({ orgId: 'org-1', role: Role.OrgAdmin }, {})

    assert.equal(result.inserted, STEEL_FIELD_PACK.length)
    assert.equal(costCodeModel.records.length, STEEL_FIELD_PACK.length)
  })

  it('blocks seeding when used codes exist', async () => {
    const { service } = createService({
      codes: [{ orgId: 'org-1', code: 'USED', description: 'Used', category: 'Legacy', isUsed: true }],
    })

    await assert.rejects(
      () => service.seedPack({ orgId: 'org-1', role: Role.OrgAdmin }, {}),
      (err) => err instanceof BadRequestException
    )
  })

  it('generates a template workbook', async () => {
    const { service } = createService()
    const buffer = await service.buildTemplate({ orgId: 'org-1', role: Role.OrgAdmin })
    assert.ok(Buffer.isBuffer(buffer))
    assert.ok(buffer.length > 0)
  })
})
