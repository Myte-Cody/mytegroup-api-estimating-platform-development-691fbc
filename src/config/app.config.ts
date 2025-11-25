const toBool = (value?: string) => value === 'true' || value === '1';
const normalizeOrigin = (val?: string) => {
  if (!val) return undefined;
  if (val.startsWith('http://') || val.startsWith('https://')) return val;
  return `https://${val}`;
};
const parseHostname = (val?: string) => {
  const origin = normalizeOrigin(val);
  if (!origin) return undefined;
  try {
    return new URL(origin).hostname;
  } catch {
    return undefined;
  }
};

export const isProduction = process.env.NODE_ENV === 'production';
const cookieDomain = process.env.SESSION_COOKIE_DOMAIN || parseHostname(process.env.CLIENT_ORIGIN);

export const sessionConfig = {
  secret: process.env.SESSION_SECRET || 'devsecret',
  resave: false,
  saveUninitialized: false,
  cookie: {
    sameSite: (process.env.SESSION_COOKIE_SAMESITE as any) || (isProduction ? 'none' : 'lax'),
    secure: toBool(process.env.SESSION_COOKIE_SECURE) || isProduction,
    httpOnly: true,
    domain: cookieDomain,
  },
};

export const mongoConfig = {
  uri: process.env.MONGO_URI || 'mongodb://localhost',
  dbName: process.env.MONGO_DB || 'MyteConstructionDev',
};

export const tenantConfig = {
  /**
   * shared: single Mongo instance/dbName for all orgs
   * dedicated: per-org URIs allowed (org-provided or template-based)
   */
  mode: (process.env.TENANT_MODE as 'shared' | 'dedicated') || 'shared',
  shared: {
    uri: process.env.MONGO_URI || 'mongodb://localhost',
    dbName: process.env.MONGO_DB || 'MyteConstructionDev',
  },
  dedicated: {
    /**
     * Optional URI template for dedicated org DBs, e.g.
     * mongodb+srv://user:pass@cluster/{orgSlug}
     */
    uriTemplate: process.env.TENANT_DEDICATED_URI_TEMPLATE,
    /**
     * Allow organizations to supply their own Mongo connection string (self-hosted).
     * This should be restricted to trusted roles and validated before use.
     */
    allowOrgProvidedUri: toBool(process.env.ALLOW_ORG_PROVIDED_URI) || false,
    /**
     * Default DB name prefix when creating dedicated DBs.
     */
    dbNamePrefix: process.env.TENANT_DEDICATED_DB_PREFIX || 'MyteTenant_',
  },
};

export const storageConfig = {
  provider: (process.env.STORAGE_PROVIDER as 'local' | 's3') || 'local',
  localPath: process.env.STORAGE_LOCAL_PATH || 'uploads',
  s3: {
    bucket: process.env.STORAGE_S3_BUCKET,
    region: process.env.STORAGE_S3_REGION,
    endpoint: process.env.STORAGE_S3_ENDPOINT,
  },
  maxUploadBytes: Number(process.env.MAX_UPLOAD_BYTES || 10 * 1024 * 1024), // 10 MB default
  allowMimeTypes: (process.env.ALLOW_MIME_TYPES || '')
    .split(',')
    .map((v) => v.trim())
    .filter(Boolean),
};

export const featureFlags = {
  enableUploads: toBool(process.env.FEATURE_ENABLE_UPLOADS) || false,
  enableTemplatesApi: toBool(process.env.FEATURE_ENABLE_TEMPLATES_API) || false,
  enableCrossOrgCollab: toBool(process.env.FEATURE_ENABLE_CROSS_ORG) || false,
};

export const clientOrigins = () => {
  const primary = normalizeOrigin(process.env.CLIENT_ORIGIN);
  const alt = normalizeOrigin(process.env.CLIENT_ORIGIN_ALT);
  const origins = [primary, alt, 'http://localhost:3000', 'http://localhost:3001'].filter(Boolean) as string[];
  return Array.from(new Set(origins));
};

export const clientBaseUrl = () => normalizeOrigin(process.env.CLIENT_ORIGIN) || 'http://localhost:3000';
export const apiBaseUrl = () => normalizeOrigin(process.env.API_BASE_URL) || clientBaseUrl();

export const buildClientUrl = (path: string) => {
  const base = clientBaseUrl();
  const trimmedPath = path.startsWith('/') ? path : `/${path}`;
  return `${base}${trimmedPath}`;
};

export const loggingConfig = {
  enableConsole: !isProduction,
  sanitizeMetadata: (meta?: Record<string, unknown>) => {
    if (!meta) return undefined;
    const redacted = { ...meta };
    if (redacted.email) redacted.email = '<redacted>';
    if (redacted.subject) redacted.subject = '<redacted>';
    if (redacted.body) redacted.body = '<redacted>';
    return redacted;
  },
};

export const smtpConfig = () => {
  const port = Number(process.env.SMTP_PORT || 587);
  const secureOverride = toBool(process.env.SMTP_SECURE);
  const secure = secureOverride || (isProduction && port === 465);
  const host = process.env.SMTP_SERVER || 'localhost';
  const authUser = process.env.SMTP_USER || process.env.EMAIL;
  const authPass = process.env.SMTP_PASS || process.env.EMAIL_PASSWORD;
  return {
    host,
    port,
    secure,
    auth: authUser && authPass ? { user: authUser, pass: authPass } : undefined,
    from: process.env.SMTP_FROM || process.env.EMAIL || authUser,
  };
};
