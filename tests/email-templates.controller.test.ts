const assert = require('node:assert/strict');
const { describe, it, beforeEach } = require('node:test');
const { ForbiddenException } = require('@nestjs/common');
const { Role } = require('../src/common/roles.ts');

process.env.TEST_EMAIL_ALLOWLIST = 'allowed@example.com';
process.env.SMTP_TEST_MODE = 'true';

const { EmailTemplatesController } = require('../src/features/email-templates/email-templates.controller.ts');

describe('EmailTemplatesController', () => {
  let auditLog: any[];
  let renders: any[];
  let sends: any[];
  let updates: any[];
  let controller: any;

  beforeEach(() => {
    auditLog = [];
    renders = [];
    sends = [];
    updates = [];
    const templates = {
      list: async (orgId: string, locale: string) => [{ name: 'invite', locale, orgId }],
      get: async (orgId: string, name: string, locale: string) => ({
        name,
        locale,
        subject: 's',
        html: '<p>h</p>',
        text: 't',
        requiredVariables: [],
        optionalVariables: [],
        source: 'custom',
      }),
      update: async (orgId: string, userId: string, name: string, dto: any) => {
        updates.push({ orgId, userId, name, dto });
        return { name, orgId, locale: dto.locale || 'en' };
      },
      render: async (orgId: string, name: string, variables: any, locale: string) => {
        renders.push({ orgId, name, variables, locale });
        return {
          template: { name, locale, requiredVariables: [], optionalVariables: [], subject: 'sub', html: '<p>hi</p>', text: 'text' },
          rendered: { subject: 'sub', html: '<p>hi</p>', text: 'text' },
        };
      },
    };
    const email = {
      sendMail: async (payload: any) => {
        sends.push(payload);
        return { status: 'sent' };
      },
    };
    const audit = { log: async (event: any) => auditLog.push(event) };
    controller = new EmailTemplatesController(templates as any, email as any, audit as any);
  });

  it('prevents accessing another org for non-superadmins', async () => {
    const req: any = { session: { user: { orgId: 'org-1', role: Role.Admin, id: 'actor-1' } }, query: { orgId: 'org-2' } };
    assert.throws(
      () => controller.update('invite' as any, { subject: 's', html: '{{inviteLink}}', text: '{{inviteLink}}' } as any, req),
      (err: any) => err instanceof ForbiddenException
    );
    assert.equal(updates.length, 0);
  });

  it('renders previews and logs audit events', async () => {
    const req: any = { session: { user: { orgId: 'org-1', role: Role.Admin, id: 'actor-1' } }, query: {} };
    const result = await controller.preview(
      'invite' as any,
      { variables: { inviteLink: 'https://x' }, locale: 'en' } as any,
      req
    );
    assert.equal(result.rendered.text, 'text');
    assert.equal(renders[0]?.orgId, 'org-1');
    assert.equal(auditLog[auditLog.length - 1]?.eventType, 'email_template.preview');
    assert.equal(auditLog[auditLog.length - 1]?.orgId, 'org-1');
  });

  it('enforces allowlist and marks test sends', async () => {
    const req: any = { session: { user: { orgId: 'org-1', role: Role.Admin, id: 'actor-2' } }, query: {} };
    await assert.rejects(
      () =>
        controller.testSend(
          'invite' as any,
          { to: 'blocked@example.com', variables: { inviteLink: 'https://x' }, locale: 'en' } as any,
          req
        ),
      (err: any) => err instanceof ForbiddenException && /allow/.test(err.message)
    );

    await controller.testSend(
      'invite' as any,
      { to: 'allowed@example.com', variables: { inviteLink: 'https://x' }, locale: 'en' } as any,
      req
    );
    assert.equal(sends[0]?.mode, 'test');
    assert.equal(sends[0]?.templateName, 'invite');
    assert.equal(sends[0]?.orgId, 'org-1');
    assert.equal(auditLog[auditLog.length - 1]?.eventType, 'email_template.test_send');
  });
});
