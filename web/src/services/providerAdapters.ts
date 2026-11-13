import { ProviderConfig, Message, JARVIS_SYSTEM_PROMPT } from '../types'

export interface ChatResult { ok: boolean; content: string; error?: string }

function toOpenAICompatibleMessages(messages: Message[]) {
  return [
    { role: 'system', content: JARVIS_SYSTEM_PROMPT },
    ...messages.filter(m => m.role !== 'system').map(m => ({ role: m.role, content: m.content }))
  ]
}

// Generic OpenAI-compatible (OpenAI, DeepSeek, Groq, OpenRouter, Mistral, Custom)
async function callOpenAICompatible(provider: ProviderConfig, messages: Message[]): Promise<ChatResult> {
  try {
    const body = {
      model: provider.modelId,
      messages: toOpenAICompatibleMessages(messages),
      temperature: 0.7,
      max_tokens: 2048
    }
    const headers: Record<string,string> = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${provider.apiKey}`
    }
    if (provider.type === 'openrouter') {
      headers['HTTP-Referer'] = window.location.origin
      headers['X-Title'] = 'JARVIS AI'
    }
    const base = provider.baseUrl || 'https://api.openai.com/v1/'
    const url = base.endsWith('/') ? base + 'chat/completions' : base + '/chat/completions'
    const res = await fetch(url, { method: 'POST', headers, body: JSON.stringify(body) })
    if (!res.ok) {
      const txt = await res.text()
      return { ok: false, content: '', error: `Provider error ${res.status}: ${txt.slice(0,300)}` }
    }
    const data = await res.json()
    const content = data.choices?.[0]?.message?.content ?? ''
    return { ok: true, content }
  } catch (e:any) { return { ok: false, content: '', error: e.message } }
}

async function callGemini(provider: ProviderConfig, messages: Message[]): Promise<ChatResult> {
  try {
    const base = provider.baseUrl || 'https://generativelanguage.googleapis.com/v1beta/'
    const b = base.endsWith('/') ? base : base + '/'
    const model = provider.modelId || 'gemini-1.5-flash'
    const url = `${b}models/${model}:generateContent?key=${provider.apiKey}`
    const contents = messages.filter(m => m.role !== 'system').map(m => ({
      role: m.role === 'assistant' ? 'model' : 'user',
      parts: [{ text: m.content }]
    }))
    const body = {
      contents,
      systemInstruction: { parts: [{ text: JARVIS_SYSTEM_PROMPT }] },
      generationConfig: { temperature: 0.7, maxOutputTokens: 2048 }
    }
    const res = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
    if (!res.ok) { const t = await res.text(); return { ok:false, content:'', error: `Gemini ${res.status}: ${t.slice(0,300)}` } }
    const data = await res.json()
    const text = data.candidates?.[0]?.content?.parts?.[0]?.text ?? ''
    return { ok: true, content: text }
  } catch (e:any) { return { ok:false, content:'', error:e.message } }
}

async function callClaude(provider: ProviderConfig, messages: Message[]): Promise<ChatResult> {
  try {
    // NOTE: Anthropic requires CORS proxy in browser; we show error and suggest using backend/custom endpoint
    const base = provider.baseUrl || 'https://api.anthropic.com/v1/'
    const b = base.endsWith('/') ? base : base + '/'
    const url = b + 'messages'
    const body = {
      model: provider.modelId,
      max_tokens: 2048,
      system: JARVIS_SYSTEM_PROMPT,
      messages: messages.filter(m=>m.role!=='system').map(m=>({ role: m.role, content: m.content }))
    }
    const res = await fetch(url, { method:'POST', headers:{ 'Content-Type':'application/json', 'x-api-key': provider.apiKey, 'anthropic-version':'2023-06-01' }, body: JSON.stringify(body) })
    if (!res.ok) { const t = await res.text(); return { ok:false, content:'', error: `Claude ${res.status}: ${t.slice(0,300)} — If CORS blocked, use OpenRouter or Custom proxy.` } }
    const data = await res.json()
    const text = data.content?.[0]?.text ?? ''
    return { ok:true, content:text }
  } catch (e:any) { return { ok:false, content:'', error:e.message + ' — Claude in browser may need CORS proxy; try OpenRouter.' } }
}

export async function chatWithProvider(provider: ProviderConfig, messages: Message[]): Promise<ChatResult> {
  // Handle creator identity locally if provider fails
  // Detect persona questions to ensure correct answer even offline
  const lastUser = [...messages].reverse().find(m=>m.role==='user')?.content?.toLowerCase() ?? ''
  const isCreatorQ = lastUser.includes('who created you') || lastUser.includes('who made you') || lastUser.includes('তোমাকে কে তৈরি করেছে') || lastUser.includes('তোমাকে কে বানিয়েছে')
  // Still call provider; if fails, fallback to local
  let result: ChatResult
  if (provider.type === 'gemini') result = await callGemini(provider, messages)
  else if (provider.type === 'claude') result = await callClaude(provider, messages)
  else result = await callOpenAICompatible(provider, messages)

  if (!result.ok && isCreatorQ) {
    const isBn = /[\u0980-\u09FF]/.test(lastUser)
    return { ok:true, content: isBn ? 'আমাকে তৈরি করেছেন Md Foysal।' : 'I was created by Md Foysal.' }
  }
  return result
}

export async function testProvider(provider: ProviderConfig): Promise<{ok:boolean; msg:string}> {
  const r = await chatWithProvider(provider, [{ id:'t', role:'user', content:'Reply with just OK', timestamp:Date.now() }])
  return r.ok ? { ok:true, msg:'Connected: ' + r.content.slice(0,80) } : { ok:false, msg: r.error || 'Failed' }
}
