import { BadRequestException } from '@nestjs/common';
import { EmailTemplateDefinition } from './email-template.constants';

export interface EmailTemplateInput {
  name: string;
  subject: string;
  html: string;
  text: string;
  requiredVariables: string[];
  locale?: string;
}

export interface RenderedEmailTemplate {
  subject: string;
  html: string;
  text: string;
}

export class EmailTemplateRenderer {
  private escapeHtml(value: string) {
    return value.replace(/[&<>"']/g, (char) => {
      switch (char) {
        case '&':
          return '&amp;';
        case '<':
          return '&lt;';
        case '>':
          return '&gt;';
        case '"':
          return '&quot;';
        case "'":
          return '&#39;';
        default:
          return char;
      }
    });
  }

  private toStringRecord(values: Record<string, unknown>) {
    return Object.entries(values || {}).reduce<Record<string, string>>((acc, [key, value]) => {
      acc[key] = value === undefined || value === null ? '' : String(value);
      return acc;
    }, {});
  }

  private replace(content: string, values: Record<string, string>) {
    return content.replace(/{{\s*([\w.]+)\s*}}/g, (_, key) => (key in values ? values[key] : ''));
  }

  render(template: EmailTemplateInput, variables: Record<string, unknown>): RenderedEmailTemplate {
    const required = template.requiredVariables || [];
    const missing = required.filter((key) => {
      const hasKey = variables && Object.prototype.hasOwnProperty.call(variables, key);
      const value = hasKey ? (variables as any)[key] : undefined;
      return value === undefined || value === null || value === '';
    });
    if (missing.length) {
      throw new BadRequestException(`Missing template variables: ${missing.join(', ')}`);
    }
    const asString = this.toStringRecord(variables || {});
    const htmlSafe = Object.entries(asString).reduce<Record<string, string>>((acc, [key, value]) => {
      acc[key] = this.escapeHtml(value);
      return acc;
    }, {});
    return {
      subject: this.replace(template.subject, asString),
      html: this.replace(template.html, htmlSafe),
      text: this.replace(template.text, asString),
    };
  }

  ensurePlaceholdersPresent(template: Pick<EmailTemplateDefinition, 'name' | 'html' | 'text' | 'requiredVariables'>) {
    const missing: string[] = [];
    template.requiredVariables.forEach((key) => {
      const pattern = new RegExp(`{{\\s*${key}\\s*}}`);
      const hasHtml = pattern.test(template.html);
      const hasText = pattern.test(template.text);
      if (!hasHtml || !hasText) {
        missing.push(key);
      }
    });
    if (missing.length) {
      throw new BadRequestException(
        `Template is missing required placeholders in html/text: ${missing.join(', ')}`
      );
    }
  }
}
