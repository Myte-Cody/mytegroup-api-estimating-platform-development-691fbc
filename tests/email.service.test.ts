const assert = require('node:assert/strict');
const { describe, it } = require('node:test');
const { BadRequestException } = require('@nestjs/common');
const { EmailService } = require('../src/features/email/email.service.ts');

describe('EmailService', () => {
  it('uses stub transport in test mode and logs attempts', async () => {
    process.env.SMTP_TEST_MODE = 'true';
    process.env.EMAIL = 'noreply@example.com';
    const auditLog: any[] = [];
    const audit = { log: async (event: any) => auditLog.push(event) };
    const templates = { render: async () => ({ rendered: { subject: 'Sub', text: 'Text', html: '<p>Text</p>' } }) };
    const service = new EmailService(audit as any, templates as any);

    const result = await service.sendMail({
      email: 'test@example.com',
      subject: 'Hi',
      text: 'Hello',
      mode: 'test',
      orgId: 'org-1',
    } as any);

    assert.equal(result.status, 'sent');
    assert.equal(service.sentMessages.length, 1);
    const eventTypes = auditLog.map((evt) => evt.eventType);
    assert(eventTypes.includes('email.send_attempt'));
    assert(eventTypes.includes('email.send'));
    assert.equal(auditLog.find((evt) => evt.eventType === 'email.send')?.metadata?.status, 'sent');
  });

  it('rejects messages without text or html content', async () => {
    process.env.SMTP_TEST_MODE = 'true';
    process.env.EMAIL = 'noreply@example.com';
    const audit = { log: async () => {} };
    const templates = { render: async () => ({ rendered: {} }) };
    const service = new EmailService(audit as any, templates as any);

    await assert.rejects(
      () => service.sendMail({ email: 'user@example.com', subject: 'Missing content' } as any),
      (err: any) => err instanceof BadRequestException
    );
  });
});
