const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const { STRONG_PASSWORD_REGEX } = require('../src/features/auth/dto/password-rules.ts');

describe('STRONG_PASSWORD_REGEX', () => {
  it('rejects weak passwords', () => {
    const regex = STRONG_PASSWORD_REGEX;
    const weak = ['short', 'password', 'NoNumber!', 'nonumberlowerUPPER!', '1234567890Aa'];
    weak.forEach((p) => assert(!regex.test(p), `expected to reject ${p}`));
  });

  it('accepts strong passwords', () => {
    const regex = STRONG_PASSWORD_REGEX;
    const strong = ['Str0ng!Passw0rd', 'Another$Pass123', 'Compl3x#Value!OK'];
    strong.forEach((p) => assert(regex.test(p), `expected to accept ${p}`));
  });
});
