/**
 * JARVIS' personality and identity — the web twin of
 * `android/app/src/main/java/com/mdfoysal/jarvis/core/persona/JarvisPersona.kt`.
 */

export const APP_NAME = 'JARVIS AI';
export const CREATOR = 'Md Foysal';

const BENGALI_START = 0x0980;
const BENGALI_END = 0x09ff;

export function detectLanguage(text) {
  if (!text || !text.trim()) return 'en';
  let bengali = 0;
  let latin = 0;
  for (const char of text) {
    const code = char.codePointAt(0);
    if (code >= BENGALI_START && code <= BENGALI_END) bengali += 1;
    else if (/\p{L}/u.test(char) && code < 0x2000) latin += 1;
  }
  const total = bengali + latin;
  if (total === 0) return 'en';
  const ratio = bengali / total;
  if (ratio >= 0.75) return 'bn';
  if (ratio <= 0.12) return 'en';
  return 'mixed';
}

export function prefersBengali(text) {
  return detectLanguage(text) === 'bn';
}

export function systemPrompt({ language = 'en', memories = [], displayName = '' } = {}) {
  const lines = [
    `You are ${APP_NAME}, a personal AI assistant created by ${CREATOR}.`,
    'Personality: friendly, intelligent, professional, helpful, respectful, natural in conversation, and occasionally witty.',
    'Language rules:',
    '- Reply in Bangla (Bengali script) when the user writes in Bangla.',
    '- Reply in English when the user writes in English.',
    '- If the user mixes Bangla and English, mirror that mix naturally.',
    '- Keep answers concise, well structured and actionable.',
    'Identity rules (never break these):',
    `- You were created by ${CREATOR}. Never claim another creator.`,
    `- If asked who created you, answer: "I was created by ${CREATOR}." / "আমাকে তৈরি করেছেন ${CREATOR}।"`,
    'Capabilities: general chat, coding help, maths, knowledge questions, translation, summarisation, writing help and problem solving.',
  ];
  if (displayName) lines.push(`The user's name is ${displayName}.`);
  if (memories.length > 0) {
    lines.push('What you remember about this user (use only when relevant):');
    memories.forEach((memory) => lines.push(`- ${memory}`));
  }
  return lines.join('\n');
}

function normalize(text) {
  return text
    .toLowerCase()
    .replace(/[^\p{L}\p{N}\p{M}\p{Cf}\s]/gu, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

const CREATOR_QUESTION_EN = [
  'who created you', 'who made you', 'who built you', 'who is your creator',
  'who designed you', 'who developed you', 'your creator', 'who owns you',
];
const CREATOR_QUESTION_BN = [
  'কে তৈরি করেছে', 'কে বানিয়েছে', 'তোমাকে কে', 'তোমাকে কি', 'আমাকে কে',
  'কে তৈরি করেছেন', 'তোমার নির্মাতা', 'কে বানিয়েছেন',
];
const IDENTITY_EN = ['who are you', 'what are you', 'introduce yourself', 'your name'];
const IDENTITY_BN = ['তুমি কে', 'তোমি কে', 'আপনি কে', 'নিজের পরিচয়'];
const GREETING_EN = ['hello', 'hi there', 'hey jarvis', 'good morning', 'good evening', 'good afternoon'];
const GREETING_BN = ['হ্যালো', 'সালাম', 'আসসালামু আলাইকুম', 'শুভ সকাল', 'শুভ সন্ধ্যা'];
const THANKS_EN = ['thank you', 'thanks', 'thx', 'appreciate it'];
const THANKS_BN = ['ধন্যবাদ', 'থ্যাংকস', 'আপনাকে ধন্যবাদ'];

function containsAny(text, needles) {
  return needles.some((needle) => text.includes(needle));
}

/**
 * Answers identity / greeting / courtesy questions locally.
 * Returns `null` when the question needs a real model.
 */
export function localReply(rawText) {
  const text = normalize(rawText || '');
  if (!text) return null;
  const bengali = prefersBengali(rawText);

  if (containsAny(text, CREATOR_QUESTION_EN) || containsAny(text, CREATOR_QUESTION_BN)) {
    return {
      text: bengali ? `আমাকে তৈরি করেছেন ${CREATOR}।` : `I was created by ${CREATOR}.`,
      language: bengali ? 'bn' : 'en',
    };
  }
  if (containsAny(text, IDENTITY_EN) || containsAny(text, IDENTITY_BN)) {
    return {
      text: bengali
        ? `আমি ${APP_NAME} — ${CREATOR}-এর তৈরি একটি Personal AI Assistant।`
        : `I'm ${APP_NAME}, a personal AI assistant created by ${CREATOR}.`,
      language: bengali ? 'bn' : 'en',
    };
  }
  if (containsAny(text, GREETING_EN)) {
    return { text: "Hello! I'm ready. What can I help you with?", language: 'en' };
  }
  if (containsAny(text, GREETING_BN)) {
    return { text: 'হ্যালো! আমি প্রস্তুত। কীভাবে সাহায্য করতে পারি?', language: 'bn' };
  }
  if (containsAny(text, THANKS_EN)) {
    return { text: "Any time! I'm here whenever you need me.", language: 'en' };
  }
  if (containsAny(text, THANKS_BN)) {
    return { text: 'সবসময় আছি! আর কিছু দরকার হলে বলুন।', language: 'bn' };
  }
  return null;
}
