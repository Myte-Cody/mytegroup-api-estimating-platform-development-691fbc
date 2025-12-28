import { Logger } from '@nestjs/common';
import ExcelJS from 'exceljs';
import { Connection, createConnection } from 'mongoose';
import { AiService } from '../common/services/ai.service';
import { mongoConfig } from '../config/app.config';
import { CostCodeImportJobSchema } from '../features/cost-codes/schemas/cost-code-import-job.schema';
import { OrganizationSchema } from '../features/organizations/schemas/organization.schema';
import { createQueue, createQueueScheduler, createWorker } from './queue.factory';

type CostCodeImportPayload = {
  orgId: string;
  jobId: string;
};

const logger = new Logger('CostCodeImportWorker');
const queueName = 'cost-code-import';
const ai = new AiService();
const MAX_IMPORT_ROWS = 1000;
const MAX_IMPORT_CODES = 5000;

export const costCodeImportQueue = createQueue(queueName);
export const costCodeImportScheduler = createQueueScheduler(queueName);

let defaultConnectionPromise: Promise<Connection> | null = null;

const dedicatedConnectionPromises = new Map<string, Promise<Connection>>();
const dedicatedConnections = new Map<string, Connection>();

const sanitizeText = (value?: string | null) => {
  if (value === undefined || value === null) return '';
  return String(value).trim();
};

const normalizeCostCodes = (inputs: Array<{ category?: string; code?: string; description?: string }>) => {
  const seen = new Set<string>();
  const normalized: Array<{ category: string; code: string; description: string }> = [];

  for (const raw of inputs || []) {
    const category = sanitizeText(raw.category) || 'General';
    const code = sanitizeText(raw.code);
    const description = sanitizeText(raw.description);
    if (!category || !code || !description) continue;
    if (seen.has(code)) continue;
    seen.add(code);
    normalized.push({ category, code, description });
    if (normalized.length >= MAX_IMPORT_CODES) break;
  }
  return normalized;
};

const getModel = <T>(connection: Connection, name: string, schema: any) => {
  if (connection.models[name]) {
    return connection.models[name] as any;
  }
  return connection.model<T>(name, schema);
};

const getDefaultConnection = async () => {
  if (!defaultConnectionPromise) {
    defaultConnectionPromise = createConnection(mongoConfig.uri, {
      dbName: mongoConfig.dbName,
      maxPoolSize: 5,
    }).asPromise();
  }
  return defaultConnectionPromise;
};

const getOrgRecord = async (orgId: string) => {
  const conn = await getDefaultConnection();
  const Org = getModel(conn, 'Organization', OrganizationSchema);
  return Org.findById(orgId).lean();
};

const getConnectionForOrg = async (orgId: string, org: any) => {
  if (!org?.useDedicatedDb || !org?.databaseUri) {
    return getDefaultConnection();
  }
  if (dedicatedConnections.has(orgId)) {
    return dedicatedConnections.get(orgId) as Connection;
  }
  if (dedicatedConnectionPromises.has(orgId)) {
    return dedicatedConnectionPromises.get(orgId) as Promise<Connection>;
  }

  const promise = createConnection(org.databaseUri, {
    dbName: org.databaseName || undefined,
    maxPoolSize: 5,
  })
    .asPromise()
    .then((conn) => {
      dedicatedConnections.set(orgId, conn);
      dedicatedConnectionPromises.delete(orgId);
      return conn;
    })
    .catch((err) => {
      dedicatedConnectionPromises.delete(orgId);
      throw err;
    });
  dedicatedConnectionPromises.set(orgId, promise);
  return promise;
};

const buildWorkbookJson = async (buffer: Buffer) => {
  const workbook = new ExcelJS.Workbook();
  await workbook.xlsx.load(buffer);
  const sheets = workbook.worksheets.map((sheet) => {
    const rows: string[][] = [];
    const maxRows = Math.min(sheet.rowCount || 0, MAX_IMPORT_ROWS);
    for (let i = 1; i <= maxRows; i += 1) {
      const row = sheet.getRow(i);
      const values = row.values as any[];
      rows.push(values.slice(1).map((val) => sanitizeText(val)));
    }
    return { name: sheet.name, rows };
  });
  return { sheets };
};

export const enqueueCostCodeImport = async (payload: CostCodeImportPayload) => {
  await costCodeImportQueue.add('import', payload, {
    removeOnComplete: true,
    attempts: 2,
    backoff: { type: 'exponential', delay: 2000 },
  });
};

export const startCostCodeImportWorker = () =>
  createWorker<CostCodeImportPayload>(queueName, async (job) => {
    const { orgId, jobId } = job.data || {};
    if (!orgId || !jobId) {
      throw new Error('Missing orgId or jobId for cost code import.');
    }

    const org = await getOrgRecord(orgId);
    if (!org) {
      logger.warn(`Org ${orgId} not found for import job ${jobId}`);
      return;
    }

    const connection = await getConnectionForOrg(orgId, org);
    const ImportJob = getModel(connection, 'CostCodeImportJob', CostCodeImportJobSchema);
    const importJob = await ImportJob.findOne({ _id: jobId, orgId });
    if (!importJob) {
      logger.warn(`Import job ${jobId} missing for org ${orgId}`);
      return;
    }

    if (['preview', 'done'].includes((importJob as any).status)) {
      return;
    }

    (importJob as any).status = 'processing';
    await importJob.save();

    try {
      if (!ai.isEnabled()) {
        throw new Error('AI is not enabled.');
      }
      const base64 = (importJob as any).fileBase64;
      if (!base64) {
        throw new Error('Import file missing.');
      }

      const buffer = Buffer.from(base64, 'base64');
      const workbook = await buildWorkbookJson(buffer);
      const extracted = await ai.extractCostCodesFromWorkbook({ orgId, workbook });
      const normalized = normalizeCostCodes(extracted || []);

      if (!normalized.length) {
        throw new Error('No cost codes detected in the import file.');
      }

      (importJob as any).preview = normalized;
      (importJob as any).status = 'preview';
      (importJob as any).errorMessage = null;
      (importJob as any).fileBase64 = null;
      await importJob.save();
    } catch (err: any) {
      (importJob as any).status = 'failed';
      (importJob as any).errorMessage = err?.message || 'Import failed';
      (importJob as any).fileBase64 = null;
      await importJob.save();
      throw err;
    }
  });
