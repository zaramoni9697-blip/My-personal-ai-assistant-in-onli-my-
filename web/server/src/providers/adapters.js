import { joinUrl } from './registry.js';

/**
 * Provider request/response adapters.
 *
 * Every provider has its own adapter: the request shape, the authentication style and the
 * response parsing are provider specific — nothing is assumed to be identical everywhere.
 */

const JSON_HEADERS = { 'Content-Type': 'application/json' };

function imageParts(message) {
  if (!message || !Array.isArray(message.images)) return [];
  return message.images
    .filter((image) => image && typeof image.data === 'string' && typeof image.mimeType === 'string')
    .slice(0, 4);
}

/** Returns the OpenAI-style content value for a message (string or multimodal array). */
function openAiContent(message) {
  const text = messageText(message);
  const images = imageParts(message);
  if (images.length === 0) return text;
  return [
    ...(text ? [{ type: 'text', text }] : []),
    ...images.map((image) => ({
      type: 'image_url',
      image_url: { url: `data:${image.mimeType};base64,${image.data}` },
    })),
  ];
}


function authHeaders(config, apiKey) {
  if (!apiKey || config.authType === 'none') return {};
  switch (config.authType) {
    case 'bearer':
      return { [config.apiKeyHeader || 'Authorization']: `Bearer ${apiKey}` };
    case 'x-api-key':
    case 'header':
      return { [config.apiKeyHeader || 'x-api-key']: apiKey };
    case 'query':
      return {};
    default:
      return { [config.apiKeyHeader || 'Authorization']: `Bearer ${apiKey}` };
  }
}

function applyAuth(config, apiKey, request) {
  if (!apiKey || config.authType === 'none') return request;
  if (config.authType === 'query') {
    const param = config.apiKeyQueryParam || 'key';
    const separator = request.url.includes('?') ? '&' : '?';
    return { ...request, url: `${request.url}${separator}${param}=${encodeURIComponent(apiKey)}` };
  }
  return { ...request, headers: { ...request.headers, ...authHeaders(config, apiKey) } };
}

function messageText(message) {
  return typeof message === 'string' ? message : message?.content || '';
}

function messageRole(message) {
  return message?.role || 'user';
}

/* ------------------------------------------------------------------ OpenAI family */

function openAiCompatibleAdapter(spec) {
  return {
    spec,
    buildChatRequest(config, messages, options = {}) {
      const body = {
        model: config.model,
        stream: Boolean(options.stream),
        messages: messages.map((message) => ({
          role: messageRole(message),
          content: openAiContent(message),
        })),
      };
      if (options.temperature !== undefined && options.temperature !== null) {
        body.temperature = options.temperature;
      }
      if (options.maxTokens) body.max_tokens = options.maxTokens;
      if (options.stream) body.stream_options = { include_usage: true };
      return {
        url: joinUrl(config.baseUrl, config.chatPath),
        method: 'POST',
        headers: { ...JSON_HEADERS, ...(spec.extraHeaders || {}), ...(config.extraHeaders || {}) },
        body: JSON.stringify(body),
      };
    },
    buildTestRequest(config) {
      return {
        url: joinUrl(config.baseUrl, config.testPath),
        method: 'GET',
        headers: { ...(spec.extraHeaders || {}), ...(config.extraHeaders || {}) },
      };
    },
    parseReply(json, config = {}) {
      const choice = json?.choices?.[0];
      const text = choice?.message?.content ?? choice?.delta?.content ?? choice?.text ?? '';
      return {
        text: typeof text === 'string' ? text : '',
        stopReason: choice?.finish_reason || null,
        promptTokens: json?.usage?.prompt_tokens ?? null,
        completionTokens: json?.usage?.completion_tokens ?? null,
      };
    },
    parseStreamEvent(json, config = {}) {
      if (json?.error) {
        return { type: 'error', error: { code: json.error.type || 'stream_error', message: json.error.message || 'Provider error' } };
      }
      const choice = json?.choices?.[0];
      const delta = choice?.delta?.content;
      if (typeof delta === 'string' && delta.length > 0) return { type: 'delta', text: delta };
      if (choice?.finish_reason) return { type: 'completed', stopReason: choice.finish_reason };
      return { type: 'ignored' };
    },
    parseError(status, body) {
      const message = body?.error?.message || body?.message || `Request failed (HTTP ${status})`;
      return {
        status,
        code: body?.error?.code || body?.error?.type || `http_${status}`,
        message: String(message).slice(0, 400),
        retryable: status === 429 || status >= 500,
      };
    },
  };
}

/** DeepSeek drops the temperature for its reasoning model and returns a separate trace. */
function deepSeekAdapter(spec) {
  const base = openAiCompatibleAdapter(spec);
  return {
    ...base,
    spec,
    buildChatRequest(config, messages, options = {}) {
      const effective = /reasoner/i.test(config.model)
        ? { ...options, temperature: undefined }
        : options;
      return base.buildChatRequest(config, messages, effective);
    },
    parseReply(json, config = {}) {
      const reply = base.parseReply(json);
      reply.reasoning = json?.choices?.[0]?.message?.reasoning_content || null;
      return reply;
    },
  };
}

/** Mistral expects an explicit max_tokens on most models. */
function mistralAdapter(spec) {
  const base = openAiCompatibleAdapter(spec);
  return {
    ...base,
    spec,
    buildChatRequest(config, messages, options = {}) {
      const effective = options.maxTokens ? options : { ...options, maxTokens: 1024 };
      return base.buildChatRequest(config, messages, effective);
    },
  };
}

/** OpenRouter proxies many vendors and wants attribution headers. */
function openRouterAdapter(spec) {
  const base = openAiCompatibleAdapter(spec);
  return {
    ...base,
    spec,
    parseReply(json, config = {}) {
      const reply = base.parseReply(json);
      reply.reasoning = json?.choices?.[0]?.message?.reasoning || null;
      return reply;
    },
  };
}

/* ------------------------------------------------------------------ Anthropic */

function anthropicAdapter(spec) {
  return {
    spec,
    buildChatRequest(config, messages, options = {}) {
      const system = messages
        .filter((message) => messageRole(message) === 'system')
        .map(messageText)
        .join('\n\n');
      const body = {
        model: config.model,
        max_tokens: options.maxTokens || 2048,
        stream: Boolean(options.stream),
        messages: messages
          .filter((message) => messageRole(message) !== 'system')
          .map((message) => ({
            role: messageRole(message) === 'assistant' ? 'assistant' : 'user',
            content: [
              ...(messageText(message) ? [{ type: 'text', text: messageText(message) }] : []),
              ...imageParts(message).map((image) => ({
                type: 'image',
                source: { type: 'base64', media_type: image.mimeType, data: image.data },
              })),
            ],
          })),
      };
      if (system) body.system = system;
      if (options.temperature !== undefined && options.temperature !== null) {
        body.temperature = options.temperature;
      }
      return {
        url: joinUrl(config.baseUrl, config.chatPath),
        method: 'POST',
        headers: {
          ...JSON_HEADERS,
          'anthropic-version': '2023-06-01',
          ...(spec.extraHeaders || {}),
          ...(config.extraHeaders || {}),
        },
        body: JSON.stringify(body),
      };
    },
    buildTestRequest(config) {
      return {
        url: joinUrl(config.baseUrl, config.testPath),
        method: 'GET',
        headers: { ...(spec.extraHeaders || {}), ...(config.extraHeaders || {}) },
      };
    },
    parseReply(json, config = {}) {
      const blocks = Array.isArray(json?.content) ? json.content : [];
      const text = blocks
        .filter((block) => block?.type === 'text')
        .map((block) => block.text || '')
        .join('');
      return {
        text,
        stopReason: json?.stop_reason || null,
        promptTokens: json?.usage?.input_tokens ?? null,
        completionTokens: json?.usage?.output_tokens ?? null,
      };
    },
    parseStreamEvent(json, config = {}) {
      switch (json?.type) {
        case 'content_block_delta': {
          const text = json?.delta?.text;
          return typeof text === 'string' && text.length > 0
            ? { type: 'delta', text }
            : { type: 'ignored' };
        }
        case 'message_stop':
          return { type: 'completed', stopReason: json?.stop_reason || null };
        case 'message_delta': {
          const stop = json?.delta?.stop_reason;
          return stop ? { type: 'completed', stopReason: stop } : { type: 'ignored' };
        }
        case 'error':
          return {
            type: 'error',
            error: {
              code: json?.error?.type || 'anthropic_error',
              message: json?.error?.message || 'Anthropic returned an error',
            },
          };
        default:
          return { type: 'ignored' };
      }
    },
    parseError(status, body) {
      const message = body?.error?.message || body?.message || `Request failed (HTTP ${status})`;
      return {
        status,
        code: body?.error?.type || `http_${status}`,
        message: String(message).slice(0, 400),
        retryable: status === 429 || status >= 500,
      };
    },
  };
}

/* ------------------------------------------------------------------ Gemini */

function geminiAdapter(spec) {
  return {
    spec,
    buildChatRequest(config, messages, options = {}) {
      const system = messages
        .filter((message) => messageRole(message) === 'system')
        .map(messageText)
        .join('\n\n');
      const body = {
        contents: messages
          .filter((message) => messageRole(message) !== 'system')
          .map((message) => ({
            role: messageRole(message) === 'assistant' ? 'model' : 'user',
            parts: [
              ...(messageText(message) ? [{ text: messageText(message) }] : []),
              ...imageParts(message).map((image) => ({
                inline_data: { mime_type: image.mimeType, data: image.data },
              })),
            ],
          })),
      };
      if (system) body.systemInstruction = { parts: [{ text: system }] };
      const generationConfig = {};
      if (options.temperature !== undefined && options.temperature !== null) {
        generationConfig.temperature = options.temperature;
      }
      if (options.maxTokens) generationConfig.maxOutputTokens = options.maxTokens;
      if (Object.keys(generationConfig).length > 0) body.generationConfig = generationConfig;

      let path = config.chatPath.replace('{model}', config.model);
      if (options.stream) path = path.replace(':generateContent', ':streamGenerateContent');
      return {
        url: joinUrl(config.baseUrl, path) + (options.stream ? '?alt=sse' : ''),
        method: 'POST',
        headers: { ...JSON_HEADERS, ...(spec.extraHeaders || {}), ...(config.extraHeaders || {}) },
        body: JSON.stringify(body),
      };
    },
    buildTestRequest(config) {
      return {
        url: joinUrl(config.baseUrl, config.testPath.replace('{model}', config.model)),
        method: 'GET',
        headers: { ...(spec.extraHeaders || {}), ...(config.extraHeaders || {}) },
      };
    },
    parseReply(json, config = {}) {
      const candidate = json?.candidates?.[0];
      const parts = candidate?.content?.parts || [];
      const text = parts.map((part) => part?.text || '').join('');
      return {
        text,
        stopReason: candidate?.finishReason || null,
        promptTokens: json?.usageMetadata?.promptTokenCount ?? null,
        completionTokens: json?.usageMetadata?.candidatesTokenCount ?? null,
      };
    },
    parseStreamEvent(json, config = {}) {
      if (json?.error) {
        return {
          type: 'error',
          error: {
            code: json.error.status || 'gemini_error',
            message: json.error.message || 'Gemini returned an error',
          },
        };
      }
      const candidate = json?.candidates?.[0];
      const text = (candidate?.content?.parts || []).map((part) => part?.text || '').join('');
      if (text) return { type: 'delta', text };
      if (candidate?.finishReason) return { type: 'completed', stopReason: candidate.finishReason };
      return { type: 'ignored' };
    },
    parseError(status, body) {
      const message = body?.error?.message || body?.message || `Request failed (HTTP ${status})`;
      return {
        status,
        code: body?.error?.status || `http_${status}`,
        message: String(message).slice(0, 400),
        retryable: status === 429 || status >= 500,
      };
    },
  };
}

/* ------------------------------------------------------------------ Custom / plain */

const PLAIN_TEXT_PATHS = [
  'choices.0.message.content',
  'message.content',
  'response',
  'output',
  'text',
  'content',
  'reply',
  'result',
];

function readPath(object, dottedPath) {
  return dottedPath.split('.').reduce((current, segment) => {
    if (current === null || current === undefined) return undefined;
    return current[segment];
  }, object);
}

function plainAdapter(spec) {
  return {
    spec,
    buildChatRequest(config, messages, options = {}) {
      const body = {
        model: config.model,
        stream: Boolean(options.stream),
        messages: messages.map((message) => ({
          role: messageRole(message),
          content: openAiContent(message),
        })),
      };
      if (options.temperature !== undefined && options.temperature !== null) {
        body.temperature = options.temperature;
      }
      if (options.maxTokens) body.max_tokens = options.maxTokens;
      return {
        url: joinUrl(config.baseUrl, config.chatPath),
        method: 'POST',
        headers: { ...JSON_HEADERS, ...(spec.extraHeaders || {}), ...(config.extraHeaders || {}) },
        body: JSON.stringify(body),
      };
    },
    buildTestRequest(config) {
      return {
        url: joinUrl(config.baseUrl, config.testPath),
        method: 'GET',
        headers: { ...(spec.extraHeaders || {}), ...(config.extraHeaders || {}) },
      };
    },
    parseReply(json, config = {}) {
      const custom = config.responseTextPath;
      const text = custom ? readPath(json, custom) : null;
      if (typeof text === 'string' && text.length > 0) return { text };
      for (const candidate of PLAIN_TEXT_PATHS) {
        const value = readPath(json, candidate);
        if (typeof value === 'string' && value.length > 0) return { text: value };
      }
      return { text: '' };
    },
    parseStreamEvent(json, config = {}) {
      const reply = this.parseReply(json, config);
      return reply.text ? { type: 'delta', text: reply.text } : { type: 'ignored' };
    },
    parseError(status, body) {
      const message = body?.message || body?.error?.message || `Request failed (HTTP ${status})`;
      return {
        status,
        code: `http_${status}`,
        message: String(message).slice(0, 400),
        retryable: status === 429 || status >= 500,
      };
    },
  };
}

/* ------------------------------------------------------------------ factory */

export function createAdapter(spec, config = {}) {
  switch (spec.id) {
    case 'gemini':
      return geminiAdapter(spec);
    case 'anthropic':
      return anthropicAdapter(spec);
    case 'openai':
      return openAiCompatibleAdapter(spec);
    case 'deepseek':
      return deepSeekAdapter(spec);
    case 'groq':
      return openAiCompatibleAdapter(spec);
    case 'openrouter':
      return openRouterAdapter(spec);
    case 'mistral':
      return mistralAdapter(spec);
    case 'custom': {
      const style = (config.requestStyle || spec.requestStyle || 'openai').toLowerCase();
      if (style === 'anthropic') return anthropicAdapter(spec);
      if (style === 'gemini') return geminiAdapter(spec);
      if (style === 'plain') return plainAdapter(spec);
      return openAiCompatibleAdapter(spec);
    }
    default:
      return openAiCompatibleAdapter(spec);
  }
}

export { applyAuth, authHeaders, openAiCompatibleAdapter };
