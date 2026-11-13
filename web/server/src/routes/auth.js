import express from 'express';
import crypto from 'node:crypto';
import { hashPassword, verifyPassword } from '../security/passwords.js';
import { signToken, verifyToken, bearerToken } from '../security/tokens.js';

const EMAIL_PATTERN = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
const MIN_PASSWORD_LENGTH = 8;

export function createAuthRouter({ db, tokenSecret, onDeleteUser }) {
  const router = express.Router();

  const findUserByEmail = (email) =>
    db.prepare('SELECT * FROM users WHERE email = ?').get(String(email || '').trim().toLowerCase());

  router.post('/register', (req, res) => {
    const email = String(req.body?.email || '').trim().toLowerCase();
    const password = String(req.body?.password || '');
    const displayName = String(req.body?.displayName || '').trim() || email.split('@')[0];

    if (!EMAIL_PATTERN.test(email)) {
      return res.status(400).json({ error: 'Enter a valid email address' });
    }
    if (password.length < MIN_PASSWORD_LENGTH) {
      return res.status(400).json({ error: `Password must be at least ${MIN_PASSWORD_LENGTH} characters` });
    }
    if (findUserByEmail(email)) {
      return res.status(409).json({ error: 'An account with this email already exists' });
    }

    const id = crypto.randomUUID();
    const createdAt = Date.now();
    db.prepare(
      `INSERT INTO users (id, email, display_name, password_hash, created_at, last_login_at)
       VALUES (?, ?, ?, ?, ?, ?)`
    ).run(id, email, displayName, hashPassword(password), createdAt, createdAt);

    const token = signToken({ sub: id, email }, tokenSecret);
    res.status(201).json({ token, user: { id, email, displayName } });
  });

  router.post('/login', (req, res) => {
    const email = String(req.body?.email || '').trim().toLowerCase();
    const password = String(req.body?.password || '');
    const user = findUserByEmail(email);
    if (!user || !verifyPassword(password, user.password_hash)) {
      return res.status(401).json({ error: 'Incorrect email or password' });
    }
    db.prepare('UPDATE users SET last_login_at = ? WHERE id = ?').run(Date.now(), user.id);
    const token = signToken({ sub: user.id, email: user.email }, tokenSecret);
    res.json({
      token,
      user: { id: user.id, email: user.email, displayName: user.display_name },
    });
  });

  // No email delivery is configured yet. Never return a reset token to the caller:
  // doing so would allow anyone who knows an email address to take over an account.
  router.post('/forgot-password', (_req, res) => {
    res.status(503).json({ error: 'Password reset is not configured on this server' });
  });

  router.post('/reset-password', (_req, res) => {
    res.status(503).json({ error: 'Password reset is not configured on this server' });
  });

  router.get('/me', (req, res) => {
    const user = currentUser(req);
    if (!user) return res.status(401).json({ error: 'Not signed in' });
    res.json({ user: { id: user.id, email: user.email, displayName: user.display_name } });
  });

  router.delete('/account', (req, res) => {
    const user = currentUser(req);
    if (!user) return res.status(401).json({ error: 'Not signed in' });
    onDeleteUser(user.id);
    res.json({ message: 'Account deleted' });
  });

  function currentUser(req) {
    const token = bearerToken(req);
    if (!token) return null;
    const payload = verifyToken(token, tokenSecret);
    if (!payload?.sub) return null;
    return db.prepare('SELECT * FROM users WHERE id = ?').get(payload.sub) || null;
  }

  router.currentUser = currentUser;
  return router;
}

/** Middleware that rejects requests without a valid session. */
export function requireAuth(authRouter) {
  return (req, res, next) => {
    const user = authRouter.currentUser(req);
    if (!user) {
      return res.status(401).json({ error: 'Not signed in' });
    }
    req.user = user;
    next();
  };
}
