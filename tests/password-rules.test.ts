const { describe, it } = require('node:test');
const assert = require('node:assert');
// ts-node/register handles TS import resolution
// eslint-disable-next-line @typescript-eslint/no-var-requires
const path = require('path');
const { pathToFileURL } = require('url');

async function loadRegex() {
  const target = pathToFileURL(path.join(__dirname, '../src/features/auth/dto/password-rules.ts')).href;
  const mod = await import(target);
  return mod.STRONG_PASSWORD_REGEX;
}

describe('STRONG_PASSWORD_REGEX', () => {
  it('rejects weak passwords', async () => {
    const regex = await loadRegex();
    const weak = ['short', 'password', 'NoNumber!', 'nonumberlowerUPPER!', '1234567890Aa'];
    weak.forEach((p) => assert(!regex.test(p), `expected to reject ${p}`));
  });

  it('accepts strong passwords', async () => {
    const regex = await loadRegex();
    const strong = ['Str0ng!Passw0rd', 'Another$Pass123', 'Compl3x#Value!OK'];
    strong.forEach((p) => assert(regex.test(p), `expected to accept ${p}`));
  });
});
