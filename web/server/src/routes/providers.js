import express from 'express';
import crypto from 'node:crypto';
import { providerSpec, providerSpecs } from '../providers/registry.js';
import { testConnection } from '../chat/engine.js';
import { ProviderError } from '../chat/engine.js';

const SAFE_PROTOCOLS = new Set(['http:', 'https:']);

/**
 * Provider configuration API.
 *
 * Plaintext API keys never appear in a response. The database stores only AES-256-GCM
 * ciphertext (`api_key_enc`); the browser receives `hasApiKey: true` at most.
 */
export function createProvidersRouter({ db, secretBox }) {
  const router = express.Router();

  const findOwned = (userId, id) =>
    db.prepare('SELECT * FROM provider_configs WHERE id = ? AND user_id = ?').get(id, userId);

  function publicConfig(row) {
    let extraHeaders = {};
    try {
      extraHeaders = JSON.parse(row.extra_headers || '{}');
    } catch {
      extraHeaders = {};
    }
    return {
      id: row.id,
      providerId: row.provider_id,
      nickname: row.nickname,
      baseUrl: row.base_url,
      chatPath: row.chat_path,
      testPath: row.test_path,
      model: row.model,
      authType: row.auth_type,
      apiKeyHeader: row.api_key_header,
      apiKeyQueryParam: row.api_key_query_param,
      extraHeaders,
      streaming: Boolean(row.streaming),
      temperature: row.temperature,
      maxTokens: row.max_tokens,
      requestStyle: row.request_style,
      responseTextPath: row.response_text_path,
      isActive: Boolean(row.is_active),
      hasApiKey: Boolean(row.api_key_enc),
      createdAt: row.created_at,
      updatedAt: row.updated_at,
    };
  }

  /** Validates and normalises a submitted configuration. */
  function validate(body, spec) {
    const text = (key) => String(body?.[key] ?? '').trim();
    const baseUrl = text('baseUrl') || spec.defaultBaseUrl;
    let parsed;
    try {
      parsed = new URL(baseUrl);
    } catch {
      throw new ValidationError('Enter a valid API base URL, for example https://api.example.com/v1');
    }
    if (!SAFE_PROTOCOLS.has(parsed.protocol)) {
      throw new ValidationError('Only http:// and https:// endpoints are allowed');
    }
    const chatPath = text('chatPath') || spec.defaultChatPath;
    const testPath = text('testPath') || spec.defaultTestPath;
    for (const [label, value] of [['Endpoint path', chatPath], ['Test path', testPath]]) {
      if (!value.startsWith('/') && !/^https?:\/\//i.test(value)) {
        throw new ValidationError(`${label} must start with /`);
      }
    }
    const model = text('model') || spec.defaultModel;
    if (!model) throw new ValidationError('Enter a model ID');
    if (model.length > 160) throw new ValidationError('Model ID is too long');
    const nickname = text('nickname') || spec.label;
    if (nickname.length > 80) throw new ValidationError('Configuration name is too long');

    const extraHeaders = normaliseHeaders(body?.extraHeaders);
    const temperature =
      body?.temperature === null || body?.temperature === undefined || body?.temperature === ''
        ? null
        : clamp(Number(body.temperature), 0, 2);
    const maxTokens =
      body?.maxTokens === null || body?.maxTokens === undefined || body?.maxTokens === ''
        ? null
        : clamp(Math.round(Number(body.maxTokens)), 1, 128000);
    if (Number.isNaN(temperature === null ? 0 : temperature) || Number.isNaN(maxTokens === null ? 0 : maxTokens)) {
      throw new ValidationError('Temperature and token limit must be numbers');
    }

    return {
      providerId: spec.id,
      nickname,
      baseUrl: baseUrl.replace(/\/+$/, ''),
      chatPath,
      testPath,
      model,
      authType: spec.customizable ? text('authType') || spec.authType : spec.authType,
      apiKeyHeader: spec.customizable ? text('apiKeyHeader') || spec.apiKeyHeader : spec.apiKeyHeader,
      apiKeyQueryParam: spec.customizable
        ? text('apiKeyQueryParam') || spec.apiKeyQueryParam || 'key'
        : spec.apiKeyQueryParam,
      extraHeaders,
      streaming: body?.streaming === undefined ? true : Boolean(body.streaming),
      temperature,
      maxTokens,
      requestStyle: spec.customizable ? text('requestStyle') || spec.requestStyle : spec.requestStyle,
      responseTextPath: spec.customizable ? text('responseTextPath') || null : null,
    };
  }

  function normaliseHeaders(input) {
    if (!input || typeof input !== 'object' || Array.isArray(input)) return {};
    const output = {};
    for (const [name, value] of Object.entries(input)) {
      if (!/^[A-Za-z0-9-]{1,64}$/.test(name)) continue;
      if (typeof value !== 'string' || value.length > 300) continue;
      if (/[\r\n]/.test(value)) continue;
      output[name] = value;
    }
    return output;
  }

  router.get('/specs', (_req, res) => {
    res.json({ providers: providerSpecs() });
  });

  router.get('/', (req, res) => {
    const rows = db
      .prepare('SELECT * FROM provider_configs WHERE user_id = ? ORDER BY created_at DESC')
      .all(req.user.id);
    res.json({ configs: rows.map(publicConfig) });
  });

  router.post('/', (req, res) => {
    const spec = providerSpec(String(req.body?.providerId || ''));
    if (!spec) return res.status(400).json({ error: 'Choose a supported provider' });
    let config;
    try {
      config = validate(req.body, spec);
    } catch (error) {
      return res.status(400).json({ error: error.message });
    }
    const apiKey = String(req.body?.apiKey || '').trim();
    if (config.authType !== 'none' && !apiKey) {
      return res.status(400).json({ error: 'Enter your API key' });
    }
    const id = crypto.randomUUID();
    const timestamp = Date.now();
    db.prepare(
      `INSERT INTO provider_configs
        (id, user_id, provider_id, nickname, base_url, chat_path, test_path, model, auth_type,
         api_key_header, api_key_query_param, extra_headers, streaming, temperature, max_tokens,
         is_active, request_style, response_text_path, api_key_enc, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).run(
      id, req.user.id, config.providerId, config.nickname, config.baseUrl, config.chatPath,
      config.testPath, config.model, config.authType, config.apiKeyHeader,
      config.apiKeyQueryParam, JSON.stringify(config.extraHeaders), config.streaming ? 1 : 0,
      config.temperature, config.maxTokens, 0, config.requestStyle, config.responseTextPath,
      apiKey ? secretBox.encrypt(apiKey) : null, timestamp, timestamp
    );
    const count = db
      .prepare('SELECT COUNT(*) AS count FROM provider_configs WHERE user_id = ?')
      .get(req.user.id).count;
    if (count === 1) {
      db.prepare('UPDATE provider_configs SET is_active = 1 WHERE id = ?').run(id);
    }
    res.status(201).json({ config: publicConfig(findOwned(req.user.id, id)) });
  });

  router.put('/:id', (req, res) => {
    const existing = findOwned(req.user.id, req.params.id);
    if (!existing) return res.status(404).json({ error: 'Configuration not found' });
    const spec = providerSpec(String(req.body?.providerId || existing.provider_id));
    if (!spec) return res.status(400).json({ error: 'Choose a supported provider' });
    let config;
    try {
      config = validate(req.body, spec);
    } catch (error) {
      return res.status(400).json({ error: error.message });
    }
    const apiKey = String(req.body?.apiKey || '').trim();
    // An empty key means "keep the stored key". A key can only ever be replaced, never read.
    const encrypted = apiKey ? secretBox.encrypt(apiKey) : existing.api_key_enc;
    db.prepare(
      `UPDATE provider_configs SET provider_id = ?, nickname = ?, base_url = ?, chat_path = ?,
        test_path = ?, model = ?, auth_type = ?, api_key_header = ?, api_key_query_param = ?,
        extra_headers = ?, streaming = ?, temperature = ?, max_tokens = ?, request_style = ?,
        response_text_path = ?, api_key_enc = ?, updated_at = ?
       WHERE id = ? AND user_id = ?`
    ).run(
      config.providerId, config.nickname, config.baseUrl, config.chatPath, config.testPath,
      config.model, config.authType, config.apiKeyHeader, config.apiKeyQueryParam,
      JSON.stringify(config.extraHeaders), config.streaming ? 1 : 0, config.temperature,
      config.maxTokens, config.requestStyle, config.responseTextPath, encrypted, Date.now(),
      existing.id, req.user.id
    );
    res.json({ config: publicConfig(findOwned(req.user.id, existing.id)) });
  });

  router.delete('/:id', (req, res) => {
    const result = db
      .prepare('DELETE FROM provider_configs WHERE id = ? AND user_id = ?')
      .run(req.params.id, req.user.id);
    if (!result.changes) return res.status(404).json({ error: 'Configuration not found' });
    res.json({ message: 'Configuration deleted' });
  });

  router.post('/:id/activate', (req, res) => {
    const existing = findOwned(req.user.id, req.params.id);
    if (!existing) return res.status(404).json({ error: 'Configuration not found' });
    db.prepare('UPDATE provider_configs SET is_active = 0 WHERE user_id = ?').run(req.user.id);
    db.prepare('UPDATE provider_configs SET is_active = 1 WHERE id = ? AND user_id = ?')
      .run(existing.id, req.user.id);
    res.json({ config: publicConfig(findOwned(req.user.id, existing.id)) });
  });

  /** Tests a saved configuration, or a draft that has not been saved yet. */
  router.post('/test', async (req, res) => {
    const body = req.body || {};
    let config;
    let apiKey;

    if (body.configId) {
      const row = findOwned(req.user.id, String(body.configId));
      if (!row) return res.status(404).json({ error: 'Configuration not found' });
      config = runtimeConfig(publicConfig(row));
      apiKey = secretBox.decrypt(row.api_key_enc) || '';
    } else {
      const spec = providerSpec(String(body.providerId || ''));
      if (!spec) return res.status(400).json({ error: 'Choose a supported provider' });
      try {
        config = validate(body, spec);
      } catch (error) {
        return res.status(400).json({ error: error.message });
      }
      apiKey = String(body.apiKey || '').trim();
    }
    if (config.authType !== 'none' && !apiKey) {
      return res.status(400).json({ error: 'Enter your API key to test the connection' });
    }
    try {
      const reply = await testConnection({ config, apiKey });
      res.json({ ok: true, providerId: config.providerId, model: config.model, reply: reply.text });
    } catch (error) {
      res.status(error instanceof ProviderError ? error.status || 502 : 502).json({
        ok: false,
        error: normaliseProviderError(error),
      });
    }
  });

  return router;
}

/** Convert the public shape into the shape the engine/adapters expect. */
export function runtimeConfig(publicShape) {
  return {
    id: publicShape.id,
    providerId: publicShape.providerId,
    nickname: publicShape.nickname,
    baseUrl: publicShape.baseUrl,
    chatPath: publicShape.chatPath,
    testPath: publicShape.testPath,
    model: publicShape.model,
    authType: publicShape.authType,
    apiKeyHeader: publicShape.apiKeyHeader,
    apiKeyQueryParam: publicShape.apiKeyQueryParam,
    extraHeaders: publicShape.extraHeaders || {},
    streaming: publicShape.streaming,
    temperature: publicShape.temperature,
    maxTokens: publicShape.maxTokens,
    requestStyle: publicShape.requestStyle,
    responseTextPath: publicShape.responseTextPath,
  };
}

export function normaliseProviderError(error) {
  if (error instanceof ProviderError) {
    return { code: error.code, message: error.message, retryable: error.retryable };
  }
  return { code: 'unexpected_error', message: error?.message || 'Unexpected provider error', retryable: false };
}

export class ValidationError extends Error {}

function clamp(value, min, max) {
  if (!Number.isFinite(value)) return Number.NaN;
  return Math.min(max, Math.max(min, value));
}
