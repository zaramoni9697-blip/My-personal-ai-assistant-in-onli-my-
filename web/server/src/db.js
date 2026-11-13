import fs from 'node:fs';
import path from 'node:path';
import { DatabaseSync } from 'node:sqlite';

/**
 * SQLite storage (Node's built-in `node:sqlite`).
 *
 * Every table that holds user data carries a `user_id`, and every query is scoped by it —
 * that is what keeps one user's provider keys and conversations invisible to another user.
 */
export function openDatabase(dataDir) {
  fs.mkdirSync(dataDir, { recursive: true });
  const db = new DatabaseSync(path.join(dataDir, 'jarvis.db'));
  db.exec('PRAGMA journal_mode = WAL');
  db.exec('PRAGMA foreign_keys = ON');

  db.exec(`
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      email TEXT NOT NULL UNIQUE,
      display_name TEXT NOT NULL,
      password_hash TEXT NOT NULL,
      created_at INTEGER NOT NULL,
      last_login_at INTEGER
    );

    CREATE TABLE IF NOT EXISTS password_resets (
      email TEXT PRIMARY KEY,
      token_hash TEXT NOT NULL,
      expires_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS provider_configs (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      provider_id TEXT NOT NULL,
      nickname TEXT NOT NULL,
      base_url TEXT NOT NULL,
      chat_path TEXT NOT NULL,
      test_path TEXT NOT NULL,
      model TEXT NOT NULL,
      auth_type TEXT NOT NULL,
      api_key_header TEXT NOT NULL,
      api_key_query_param TEXT,
      extra_headers TEXT NOT NULL DEFAULT '{}',
      streaming INTEGER NOT NULL DEFAULT 1,
      temperature REAL,
      max_tokens INTEGER,
      is_active INTEGER NOT NULL DEFAULT 0,
      request_style TEXT NOT NULL DEFAULT 'openai',
      response_text_path TEXT,
      api_key_enc TEXT,
      created_at INTEGER NOT NULL,
      updated_at INTEGER NOT NULL
    );

    CREATE INDEX IF NOT EXISTS idx_provider_user ON provider_configs(user_id);

    CREATE TABLE IF NOT EXISTS conversations (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      title TEXT NOT NULL,
      provider_config_id TEXT,
      created_at INTEGER NOT NULL,
      updated_at INTEGER NOT NULL
    );

    CREATE INDEX IF NOT EXISTS idx_conversation_user ON conversations(user_id);

    CREATE TABLE IF NOT EXISTS messages (
      id TEXT PRIMARY KEY,
      conversation_id TEXT NOT NULL,
      user_id TEXT NOT NULL,
      role TEXT NOT NULL,
      content TEXT NOT NULL,
      created_at INTEGER NOT NULL
    );

    CREATE INDEX IF NOT EXISTS idx_message_conversation ON messages(conversation_id);

    CREATE TABLE IF NOT EXISTS memories (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL,
      content TEXT NOT NULL,
      created_at INTEGER NOT NULL
    );

    CREATE INDEX IF NOT EXISTS idx_memory_user ON memories(user_id);

    CREATE TABLE IF NOT EXISTS user_settings (
      user_id TEXT PRIMARY KEY,
      language TEXT NOT NULL DEFAULT 'auto',
      tts_enabled INTEGER NOT NULL DEFAULT 1,
      tts_rate REAL NOT NULL DEFAULT 1.0,
      voice_mode INTEGER NOT NULL DEFAULT 0,
      wake_word_enabled INTEGER NOT NULL DEFAULT 0,
      wake_word_phrase TEXT NOT NULL DEFAULT 'সবসময় প্রস্তুত',
      memory_enabled INTEGER NOT NULL DEFAULT 0
    );
  `);

  return db;
}

export function now() {
  return Date.now();
}

export function newId(prefix = '') {
  const random = cryptoRandom();
  return prefix ? `${prefix}_${random}` : random;
}

function cryptoRandom() {
  // eslint-disable-next-line no-undef
  return globalThis.crypto.randomUUID().replace(/-/g, '');
}
