import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';

/**
 * Token signing (JWT-like, HS256) and the server master key.
 *
 * The master key encrypts the stored API keys at rest. It comes from the environment
 * (`JARVIS_MASTER_KEY`) or is generated once and persisted with 0600 permissions.
 */
export function loadMasterKey(dataDir) {
  const fromEnv = process.env.JARVIS_MASTER_KEY;
  if (fromEnv && fromEnv.length >= 32) {
    return Buffer.from(fromEnv, 'utf8').slice(0, 32).toString('base64');
  }
  const keyFile = path.join(dataDir, 'master.key');
  if (fs.existsSync(keyFile)) {
    return fs.readFileSync(keyFile, 'utf8').trim();
  }
  const generated = crypto.randomBytes(32).toString('base64');
  fs.mkdirSync(dataDir, { recursive: true });
  fs.writeFileSync(keyFile, generated, { mode: 0o600 });
  return generated;
}

export function loadTokenSecret(dataDir) {
  const fromEnv = process.env.JARVIS_TOKEN_SECRET;
  if (fromEnv && fromEnv.length >= 32) return fromEnv;
  const secretFile = path.join(dataDir, 'token.secret');
  if (fs.existsSync(secretFile)) return fs.readFileSync(secretFile, 'utf8').trim();
  const generated = crypto.randomBytes(48).toString('base64');
  fs.mkdirSync(dataDir, { recursive: true });
  fs.writeFileSync(secretFile, generated, { mode: 0o600 });
  return generated;
}

function base64url(buffer) {
  return Buffer.from(buffer).toString('base64url');
}

/** Creates a signed token: `header.payload.signature`. */
export function signToken(payload, secret, expiresInSeconds = 60 * 60 * 24 * 7) {
  const header = { alg: 'HS256', typ: 'JWT' };
  const now = Math.floor(Date.now() / 1000);
  const body = { ...payload, iat: now, exp: now + expiresInSeconds };
  const encodedHeader = base64url(JSON.stringify(header));
  const encodedPayload = base64url(JSON.stringify(body));
  const signature = crypto
    .createHmac('sha256', secret)
    .update(`${encodedHeader}.${encodedPayload}`)
    .digest('base64url');
  return `${encodedHeader}.${encodedPayload}.${signature}`;
}

/** Verifies a token and returns its payload, or `null` when it is invalid/expired. */
export function verifyToken(token, secret) {
  if (typeof token !== 'string') return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  const [encodedHeader, encodedPayload, signature] = parts;
  const expected = crypto
    .createHmac('sha256', secret)
    .update(`${encodedHeader}.${encodedPayload}`)
    .digest('base64url');
  const given = Buffer.from(signature);
  const want = Buffer.from(expected);
  if (given.length !== want.length || !crypto.timingSafeEqual(given, want)) return null;
  let payload;
  try {
    payload = JSON.parse(Buffer.from(encodedPayload, 'base64url').toString('utf8'));
  } catch {
    return null;
  }
  if (payload.exp && payload.exp < Math.floor(Date.now() / 1000)) return null;
  return payload;
}

/** Extracts the bearer token from an Authorization header. */
export function bearerToken(req) {
  const header = req.headers.authorization || '';
  if (!header.toLowerCase().startsWith('bearer ')) return null;
  return header.slice(7).trim();
}
