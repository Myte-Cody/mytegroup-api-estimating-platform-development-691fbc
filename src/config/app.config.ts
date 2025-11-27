const toBool = (value?: string) => value === 'true' || value === '1';
const normalizeOrigin = (val?: string) => {
  if (!val) return undefined;
  if (val.startsWith('http://') || val.startsWith('https://')) return val;
  return `https://${val}`;
};
const stripProtocol = (val?: string) => (val || '').replace(/^https?:\/\//, '').split('/')[0];
const parseList = (val?: string) =>
  (val || '')
    .split(',')
    .map((v) => v.trim())
    .filter(Boolean);
const parseHostPort = (val?: string) => {
  const cleaned = stripProtocol(val);
  if (!cleaned) return { host: undefined as string | undefined, port: undefined as number | undefined };
  const [host, port] = cleaned.split(':');
  return { host, port: port ? Number(port) : undefined };
};

export const isProduction = process.env.NODE_ENV === 'production';
const protocol = isProduction ? 'https' : 'http';
const rootDomainRaw = isProduction
  ? process.env.ROOT_DOMAIN_PROD || process.env.ROOT_DOMAIN
  : process.env.ROOT_DOMAIN_DEV || process.env.ROOT_DOMAIN;
const { host: rootHost } = parseHostPort(rootDomainRaw);
const defaultClientPort = Number(process.env.CLIENT_PORT || (isProduction ? 443 : 6666));
const defaultApiPort = Number(process.env.API_PORT || process.env.PORT || (isProduction ? 443 : 7070));

const buildOrigin = (host: string | undefined, port?: number) => {
  const safeHost = host || 'localhost';
  const portToUse = typeof port === 'number' && !Number.isNaN(port) ? port : undefined;
  const needsPort = portToUse && ![80, 443].includes(portToUse);
  const portSegment = needsPort ? `:${portToUse}` : '';
  return `${protocol}://${safeHost}${portSegment}`;
};

export const clientOrigin = buildOrigin(rootHost, defaultClientPort);
export const apiOrigin = buildOrigin(rootHost, defaultApiPort);

const rawCookieDomain = process.env.SESSION_COOKIE_DOMAIN || rootHost;

// In dev, force localhost/lax/insecure cookies so that npm run dev
// (frontend + backend on localhost) always has a working session,
// regardless of prod-focused env overrides.
const devMode = !isProduction;
const cookieDomain = devMode ? 'localhost' : rawCookieDomain;
const cookieSameSite = devMode
  ? 'lax'
  : ((process.env.SESSION_COOKIE_SAMESITE as any) || 'none');
const cookieSecure = devMode
  ? false
  : toBool(process.env.SESSION_COOKIE_SECURE) || true;

export const sessionConfig = {
  secret: process.env.SESSION_SECRET || 'devsecret',
  resave: false,
  saveUninitialized: false,
  cookie: {
    sameSite: cookieSameSite,
    secure: cookieSecure,
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
  const origins = [
    normalizeOrigin(process.env.CLIENT_ORIGIN),
    normalizeOrigin(process.env.CLIENT_ORIGIN_ALT),
    normalizeOrigin(process.env.ROOT_DOMAIN_PROD),
    normalizeOrigin(process.env.ROOT_DOMAIN_DEV),
    clientOrigin,
    'http://localhost:3000',
    'http://localhost:3001',
    'http://localhost:6666',
    'http://localhost:4000',
    'http://localhost:4001',
  ].filter(Boolean) as string[];
  return Array.from(new Set(origins));
};

export const clientBaseUrl = () => clientOrigin;
export const apiBaseUrl = () => apiOrigin;

export const buildClientUrl = (path: string) => {
  const base = clientBaseUrl();
  const trimmedPath = path.startsWith('/') ? path : `/${path}`;
  return `${base}${trimmedPath}`;
};

export const apiPort = () => {
  const raw = Number(process.env.API_PORT || process.env.PORT);
  if (!Number.isNaN(raw)) return raw;
  return defaultApiPort;
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
  const stubTransport = toBool(process.env.SMTP_TEST_MODE) || process.env.NODE_ENV === 'test';
  return {
    host,
    port,
    secure,
    auth: authUser && authPass ? { user: authUser, pass: authPass } : undefined,
    from: process.env.SMTP_FROM || process.env.EMAIL || authUser,
    stubTransport,
  };
};

export const emailTestConfig = {
  allowedRecipients: parseList(process.env.TEST_EMAIL_ALLOWLIST || 'test@example.com'),
};
