import { createAdapter, applyAuth } from '../providers/adapters.js';
import { providerSpec } from '../providers/registry.js';

/** Raised when a provider call fails; carries the normalised error. */
export class ProviderError extends Error {
  constructor(error) {
    super(error.message);
    this.name = 'ProviderError';
    this.status = error.status;
    this.code = error.code;
    this.retryable = error.retryable;
  }
}

function adapterFor(config) {
  const spec = providerSpec(config.providerId);
  if (!spec) {
    throw new Error(`Unknown provider '${config.providerId}'`);
  }
  return createAdapter(spec, config);
}

/**
 * Executes a chat completion against the configured provider.
 * The API key is only ever used server side — it is never returned to the browser.
 */
export async function chat({ config, apiKey, messages, options = {}, fetchImpl = fetch }) {
  const adapter = adapterFor(config);
  const request = applyAuth(config, apiKey, adapter.buildChatRequest(config, messages, options));

  let response;
  try {
    response = await fetchImpl(request.url, {
      method: request.method,
      headers: request.headers,
      body: request.body,
    });
  } catch (error) {
    throw new ProviderError({
      status: 0,
      code: 'network_error',
      message: `Could not reach ${config.providerId}: ${error.message}`,
      retryable: true,
    });
  }

  const text = await response.text();
  if (!response.ok) {
    let parsed = null;
    try {
      parsed = JSON.parse(text);
    } catch {
      parsed = null;
    }
    throw new ProviderError(adapter.parseError(response.status, parsed || { message: text.slice(0, 300) }));
  }

  let json;
  try {
    json = JSON.parse(text);
  } catch {
    throw new ProviderError({
      status: response.status,
      code: 'invalid_response',
      message: 'The provider returned a response that is not valid JSON',
      retryable: false,
    });
  }
  return adapter.parseReply(json, config);
}

/**
 * Streams a completion. `onDelta` is called for every text fragment.
 * Returns the assembled answer.
 */
export async function streamChat({
  config,
  apiKey,
  messages,
  options = {},
  fetchImpl = fetch,
  onDelta = () => {},
}) {
  const adapter = adapterFor(config);
  const request = applyAuth(
    config,
    apiKey,
    adapter.buildChatRequest(config, messages, { ...options, stream: true })
  );

  let response;
  try {
    response = await fetchImpl(request.url, {
      method: request.method,
      headers: request.headers,
      body: request.body,
    });
  } catch (error) {
    throw new ProviderError({
      status: 0,
      code: 'network_error',
      message: `Could not reach ${config.providerId}: ${error.message}`,
      retryable: true,
    });
  }

  if (!response.ok) {
    const text = await response.text();
    let parsed = null;
    try {
      parsed = JSON.parse(text);
    } catch {
      parsed = null;
    }
    throw new ProviderError(adapter.parseError(response.status, parsed || { message: text.slice(0, 300) }));
  }

  const reader = response.body?.getReader();
  if (!reader) {
    const reply = adapter.parseReply(JSON.parse(await response.text()), config);
    if (reply.text) onDelta(reply.text);
    return reply.text;
  }

  const decoder = new TextDecoder();
  let buffer = '';
  let answer = '';

  const handle = (payload) => {
    if (!payload || payload === '[DONE]') return;
    let json;
    try {
      json = JSON.parse(payload);
    } catch {
      return;
    }
    const event = adapter.parseStreamEvent(json, config);
    if (event.type === 'delta') {
      answer += event.text;
      onDelta(event.text);
    } else if (event.type === 'error') {
      throw new ProviderError({ status: 0, code: event.error.code, message: event.error.message, retryable: false });
    }
  };

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    let boundary = buffer.indexOf('\n\n');
    while (boundary !== -1) {
      const frame = buffer.slice(0, boundary);
      buffer = buffer.slice(boundary + 2);
      const data = frame
        .split('\n')
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.slice(5).trim())
        .join('\n');
      if (data) handle(data);
      boundary = buffer.indexOf('\n\n');
    }
  }
  if (buffer.trim()) {
    const data = buffer
      .split('\n')
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.slice(5).trim())
      .join('\n');
    if (data) handle(data);
  }
  return answer;
}

/** Runs a minimal completion to verify that a configuration really works. */
export async function testConnection({ config, apiKey, fetchImpl = fetch }) {
  return chat({
    config,
    apiKey,
    messages: [{ role: 'user', content: 'Reply with the single word: OK' }],
    options: { temperature: 0, maxTokens: 16 },
    fetchImpl,
  });
}
