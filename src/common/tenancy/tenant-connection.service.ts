import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectConnection, InjectModel } from '@nestjs/mongoose';
import { Connection, Model, Schema, createConnection } from 'mongoose';
import { Organization } from '../../features/organizations/schemas/organization.schema';

@Injectable()
export class TenantConnectionService {
  private readonly connectionPromises = new Map<string, Promise<Connection>>();
  private readonly connections = new Map<string, Connection>();

  constructor(
    @InjectConnection() private readonly defaultConnection: Connection,
    @InjectModel('Organization') private readonly orgModel: Model<Organization>
  ) {}

  private async resolveOrg(orgId: string) {
    const org = await this.orgModel.findById(orgId);
    if (!org) throw new NotFoundException('Organization not found');
    return org;
  }

  async getConnectionForOrg(orgId: string): Promise<Connection> {
    const org = await this.resolveOrg(orgId);
    if (!org.useDedicatedDb || !org.databaseUri) {
      return this.defaultConnection;
    }
    if (this.connections.has(orgId)) {
      return this.connections.get(orgId) as Connection;
    }
    if (this.connectionPromises.has(orgId)) {
      return this.connectionPromises.get(orgId) as Promise<Connection>;
    }
    const promise = createConnection(org.databaseUri, {
      dbName: org.databaseName || undefined,
      maxPoolSize: 5,
    })
      .asPromise()
      .then((conn) => {
        this.connections.set(orgId, conn);
        this.connectionPromises.delete(orgId);
        return conn;
      })
      .catch((err) => {
        this.connectionPromises.delete(orgId);
        throw err;
      });
    this.connectionPromises.set(orgId, promise);
    return promise;
  }

  async getModelForOrg<T>(
    orgId: string,
    modelName: string,
    schema: Schema<T>,
    defaultModel?: Model<T>
  ): Promise<Model<T>> {
    const connection = await this.getConnectionForOrg(orgId);
    if (connection === this.defaultConnection && defaultModel) {
      return defaultModel;
    }
    if (connection.models[modelName]) {
      return connection.models[modelName] as Model<T>;
    }
    return connection.model<T>(modelName, schema);
  }

  async resetConnectionForOrg(orgId: string) {
    if (this.connectionPromises.has(orgId)) {
      this.connectionPromises.delete(orgId);
    }
    const existing = this.connections.get(orgId);
    if (existing) {
      try {
        await existing.close();
      } catch {
        // ignore close errors; connection will be re-established on next request
      }
      this.connections.delete(orgId);
    }
  }

  async testConnection(uri: string, dbName?: string) {
    const conn = await createConnection(uri, { dbName, maxPoolSize: 1 }).asPromise();
    await conn.close();
  }
}
