import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const REGISTRY_PATH = path.resolve(here, '../../../../shared/providers.json');

/**
 * The canonical provider registry.
 *
 * `shared/providers.json` is the single source of truth for the whole project — the Android
 * app ships the same file inside its assets.
 */
let cached = null;

export function loadRegistry() {
  if (cached) return cached;
  const raw = fs.readFileSync(REGISTRY_PATH, 'utf8');
  const parsed = JSON.parse(raw);
  cached = parsed.providers.map((provider) => ({
    ...provider,
    authType: (provider.authType || 'bearer').toLowerCase(),
    requestStyle: (provider.requestStyle || 'openai').toLowerCase(),
  }));
  return cached;
}

export function providerSpecs() {
  return loadRegistry();
}

export function providerSpec(id) {
  return loadRegistry().find((provider) => provider.id === id) || null;
}

export function joinUrl(base, suffix) {
  const b = String(base || '').trim().replace(/\/+$/, '');
  const p = String(suffix || '').trim();
  if (!p) return b;
  if (/^https?:\/\//i.test(p)) return p;
  return p.startsWith('/') ? `${b}${p}` : `${b}/${p}`;
}
