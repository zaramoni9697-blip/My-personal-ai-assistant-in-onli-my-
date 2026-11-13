export function detectLanguage(text: string): 'bn'|'en' {
  const hasBengali = /[\u0980-\u09FF]/.test(text)
  return hasBengali ? 'bn' : 'en'
}

export function creatorAnswer(lang: 'bn'|'en') {
  return lang === 'bn' ? 'আমাকে তৈরি করেছেন Md Foysal।' : 'I was created by Md Foysal.'
}

// Local fallback for offline/demo
export function localFallbackReply(userText: string): string {
  const lower = userText.toLowerCase()
  const lang = detectLanguage(userText)
  if (lower.includes('who created you') || lower.includes('who made you') || lower.includes('তোমাকে কে তৈরি করেছে') || lower.includes('তোমাকে কে বানিয়েছে') || lower.includes('তুমি কে')) {
    return lang === 'bn' ? 'আমি JARVIS AI, আমাকে তৈরি করেছেন Md Foysal। আমি আপনার personal AI assistant।' : 'I am JARVIS AI, created by Md Foysal. I am your personal AI assistant!'
  }
  if (lower.includes('hello') || lower.includes('hi') || lower.includes('হ্যালো')) {
    return lang === 'bn' ? 'হ্যালো! আমি JARVIS, প্রস্তুত আছি। আপনাকে কিভাবে সাহায্য করতে পারি?' : 'Hello! I am JARVIS, ready to help. What can I do for you?'
  }
  if (lower.includes('open') && lower.includes('setting')) return lang==='bn' ? 'আমি আপনার জন্য supported settings page খুলে দিচ্ছি।' : "I'll help you open the supported settings page."
  if (lower.includes('code') || lower.includes('python') || lower.includes('function')) {
    return 'Here is a helpful code example:\n```python\ndef jarvis_greet(name):\n    return f"Hello {name}, I am JARVIS by Md Foysal!"\n```\nLet me know what you want to build!'
  }
  return lang==='bn'
    ? `আপনার বার্তা পেয়েছি: "${userText}" — আমি JARVIS AI, Md Foysal-এর তৈরি। বর্তমানে কোনো AI provider কনফিগার করা নেই বা সংযোগ ব্যর্থ হয়েছে। অনুগ্রহ করে Provider সেটিংসে গিয়ে Gemini/OpenAI ইত্যাদির API key যোগ করুন।`
    : `Got it: "${userText}" — I'm JARVIS AI by Md Foysal. No active provider is configured or the connection failed. Please add an API key in Provider Settings (Gemini/OpenAI etc.) to get live AI responses. I can still help with basic tasks!`
}
