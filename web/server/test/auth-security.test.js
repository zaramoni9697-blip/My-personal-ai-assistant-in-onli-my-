import { test } from 'node:test';
import assert from 'node:assert/strict';
import express from 'express';
import { mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { openDatabase } from '../src/db.js';
import { createAuthRouter } from '../src/routes/auth.js';

test('password reset is unavailable without a secure delivery mechanism', async () => {
  const dir = mkdtempSync(path.join(tmpdir(), 'jarvis-auth-'));
  const db = openDatabase(dir);
  const app = express();
  app.use(express.json());
  app.use('/api/auth', createAuthRouter({ db, tokenSecret: 'test-only-secret', onDeleteUser: () => {} }));
  const server = app.listen(0, '127.0.0.1');
  await new Promise(resolve => server.once('listening', resolve));
  const root = `http://127.0.0.1:${server.address().port}/api/auth`;
  const post = (route, body) => fetch(`${root}${route}`, {
    method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(body),
  });
  try {
    const signup = await post('/register', { email: 'test@example.org', password: 'a secure password', displayName: 'Test' });
    assert.equal(signup.status, 201);
    for (const email of ['test@example.org', 'unknown@example.org']) {
      const response = await post('/forgot-password', { email });
      assert.equal(response.status, 503);
      const body = await response.json();
      assert.equal(body.resetToken, undefined);
    }
    const reset = await post('/reset-password', { email: 'test@example.org', token: 'anything', newPassword: 'new password' });
    assert.equal(reset.status, 503);
    assert.equal((await post('/login', { email: 'test@example.org', password: 'a secure password' })).status, 200);
  } finally {
    await new Promise(resolve => server.close(resolve));
    db.close();
    rmSync(dir, { recursive: true, force: true });
  }
});
