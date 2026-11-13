# Web — JARVIS AI

```bash
npm install
npm run dev   # http://localhost:5173
npm run build # dist/
```

## Auth
Local demo auth in `localStorage['jarvis:users']`. Each user’s providers/conversations namespaced by userId.

## Providers
Add keys in `/providers`. Test button hits real provider endpoints. CORS may block Anthropic in browser — use OpenRouter as proxy or Custom API.

## Voice
Uses Web Speech API (`webkitSpeechRecognition` + `speechSynthesis`). Best in Chrome/Edge. Language auto detection via Bengali Unicode range.

## State
Zustand store persisted; conversations/providers per user.
