// Secure local storage with simple obfuscation (AES would need backend for true security)
// For web we store encrypted via Web Crypto would be heavy; use base64+user isolation

export const Storage = {
  get<T>(key: string, fallback: T): T {
    try { const v = localStorage.getItem(key); return v ? JSON.parse(v) : fallback } catch { return fallback }
  },
  set(key: string, value: any) { localStorage.setItem(key, JSON.stringify(value)) },
  remove(key: string) { localStorage.removeItem(key) }
}

// Per-user namespaced helpers
export function userKey(userId: string, key: string) { return `jarvis:${userId}:${key}` }

// Simple "encryption" for demo - base64
export function encryptApiKey(key: string): string { try { return btoa(key) } catch { return key } }
export function decryptApiKey(enc: string): string { try { return atob(enc) } catch { return enc } }
