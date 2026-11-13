export type ProviderType = 'gemini'|'openai'|'claude'|'deepseek'|'groq'|'openrouter'|'mistral'|'custom'

export interface ProviderConfig {
  id: string
  type: ProviderType
  displayName: string
  apiKey: string
  modelId: string
  baseUrl: string
  isActive: boolean
  lastTest?: { ok: boolean; msg: string; at: number }
}

export interface Message {
  id: string
  role: 'user'|'assistant'|'system'
  content: string
  timestamp: number
  providerType?: string
  modelId?: string
}

export interface Conversation {
  id: string
  title: string
  messages: Message[]
  providerId?: string
  createdAt: number
  updatedAt: number
}

export interface User {
  id: string
  email: string
  displayName: string
}

export const PROVIDER_META: Record<ProviderType, { label: string; defaultBaseUrl: string; defaultModel: string; placeholder: string }> = {
  gemini: { label: 'Google Gemini', defaultBaseUrl: 'https://generativelanguage.googleapis.com/v1beta/', defaultModel: 'gemini-1.5-flash', placeholder: 'AIza...' },
  openai: { label: 'OpenAI', defaultBaseUrl: 'https://api.openai.com/v1/', defaultModel: 'gpt-4o-mini', placeholder: 'sk-...' },
  claude: { label: 'Anthropic Claude', defaultBaseUrl: 'https://api.anthropic.com/v1/', defaultModel: 'claude-3-5-sonnet-20241022', placeholder: 'sk-ant-...' },
  deepseek: { label: 'DeepSeek', defaultBaseUrl: 'https://api.deepseek.com/v1/', defaultModel: 'deepseek-chat', placeholder: 'sk-...' },
  groq: { label: 'Groq', defaultBaseUrl: 'https://api.groq.com/openai/v1/', defaultModel: 'llama-3.1-8b-instant', placeholder: 'gsk_...' },
  openrouter: { label: 'OpenRouter', defaultBaseUrl: 'https://openrouter.ai/api/v1/', defaultModel: 'openai/gpt-4o-mini', placeholder: 'sk-or-...' },
  mistral: { label: 'Mistral', defaultBaseUrl: 'https://api.mistral.ai/v1/', defaultModel: 'mistral-large-latest', placeholder: '...' },
  custom: { label: 'Custom API', defaultBaseUrl: '', defaultModel: '', placeholder: 'API key' },
}

export const JARVIS_SYSTEM_PROMPT = `You are JARVIS AI, a friendly, intelligent, professional personal AI assistant created by Md Foysal.
Personality: helpful, respectful, natural, a little humorous when appropriate.
Languages: Bengali and English. Detect user language automatically and respond in same language.
If asked who created you, answer: "I was created by Md Foysal." In Bengali: "আমাকে তৈরি করেছেন Md Foysal।"
Never claim to be created by anyone else.`
