const assert = require('node:assert/strict');
const { describe, it } = require('node:test');
const { BadRequestException } = require('@nestjs/common');
const { EmailTemplateRenderer } = require('../src/features/email-templates/email-template.renderer.ts');
const { BUILTIN_EMAIL_TEMPLATES } = require('../src/features/email-templates/email-template.constants.ts');

describe('EmailTemplateRenderer', () => {
  it('throws when required variables are missing or empty', () => {
    const renderer = new EmailTemplateRenderer();
    assert.throws(
      () =>
        renderer.render(
          {
            name: 'verification',
            subject: 'Verify',
            html: '<p>{{verifyLink}}</p>',
            text: 'Link: {{verifyLink}}',
            requiredVariables: ['verifyLink'],
          },
          {}
        ),
      (err) => err instanceof BadRequestException && /Missing template variables/.test((err as Error).message)
    );
  });

  it('escapes HTML values while keeping text intact', () => {
    const renderer = new EmailTemplateRenderer();
    const { html, text } = renderer.render(
      {
        name: 'verification',
        subject: 'Verify',
        html: '<p>{{verifyLink}}</p>',
        text: 'Link: {{verifyLink}}',
        requiredVariables: ['verifyLink'],
      },
      { verifyLink: '<script>alert(1)</script>' }
    );
    assert.ok(html.includes('&lt;script&gt;alert(1)&lt;/script&gt;'));
    assert.ok(!html.includes('<script>alert(1)</script>'));
    assert.ok(text.includes('<script>alert(1)</script>'));
  });

  it('guards that built-in templates keep required placeholders', () => {
    const renderer = new EmailTemplateRenderer();
    BUILTIN_EMAIL_TEMPLATES.forEach((tpl: any) => renderer.ensurePlaceholdersPresent(tpl));
  });
});
