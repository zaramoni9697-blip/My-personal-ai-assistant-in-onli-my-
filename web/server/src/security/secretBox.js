import crypto from 'node:crypto';

/**
 * Authenticated encryption for API keys stored on the server.
 *
 * AES-256-GCM with a random IV per value. The output is `iv || ciphertext || tag`, base64
 * encoded. Keys never leave the server in plain text and are never sent to the browser.
 */
export class SecretBox {
  constructor(masterKeyBase64) {
    this.key = Buffer.from(masterKeyBase64, 'base64').subarray(0, 32);
    if (this.key.length !== 32) {
      throw new Error('The master key must decode to 32 bytes');
    }
  }

  encrypt(plaintext) {
    const iv = crypto.randomBytes(12);
    const cipher = crypto.createCipheriv('aes-256-gcm', this.key, iv);
    const encrypted = Buffer.concat([cipher.update(plaintext, 'utf8'), cipher.final()]);
    const tag = cipher.getAuthTag();
    return Buffer.concat([iv, encrypted, tag]).toString('base64');
  }

  decrypt(payload) {
    if (typeof payload !== 'string' || payload.length === 0) return null;
    try {
      const raw = Buffer.from(payload, 'base64');
      if (raw.length < 12 + 16) return null;
      const iv = raw.subarray(0, 12);
      const tag = raw.subarray(raw.length - 16);
      const body = raw.subarray(12, raw.length - 16);
      const decipher = crypto.createDecipheriv('aes-256-gcm', this.key, iv);
      decipher.setAuthTag(tag);
      return Buffer.concat([decipher.update(body), decipher.final()]).toString('utf8');
    } catch {
      return null;
    }
  }
}
