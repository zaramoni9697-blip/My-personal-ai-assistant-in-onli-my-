import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { Conversation, Message, ProviderConfig, User } from '../types'
import { Storage, userKey } from '../services/storage'

interface AppState {
  user: User | null
  providers: ProviderConfig[]
  activeProviderId: string | null
  conversations: Conversation[]
  activeConversationId: string | null
  isThinking: boolean
  // auth
  setUser: (u: User | null) => void
  // providers
  addProvider: (p: ProviderConfig) => void
  updateProvider: (id: string, patch: Partial<ProviderConfig>) => void
  deleteProvider: (id: string) => void
  setActiveProvider: (id: string) => void
  // conversations
  createConversation: (title?: string) => string
  selectConversation: (id: string) => void
  deleteConversation: (id: string) => void
  addMessage: (convId: string, msg: Message) => void
  setThinking: (v: boolean) => void
  // utils
  getActiveProvider: () => ProviderConfig | null
  hydrateForUser: (userId: string) => void
}

export const useStore = create<AppState>()(
  persist(
    (set, get) => ({
      user: null,
      providers: [],
      activeProviderId: null,
      conversations: [],
      activeConversationId: null,
      isThinking: false,

      setUser: (u) => {
        set({ user: u })
        if (u) {
          // load per-user data
          const p = Storage.get<ProviderConfig[]>(userKey(u.id, 'providers'), [])
          const c = Storage.get<Conversation[]>(userKey(u.id, 'conversations'), [])
          const active = Storage.get<string | null>(userKey(u.id, 'activeProviderId'), null)
          set({ providers: p, conversations: c, activeProviderId: active, activeConversationId: c[0]?.id ?? null })
        } else {
          set({ providers: [], conversations: [], activeProviderId: null, activeConversationId: null })
        }
      },

      addProvider: (p) => {
        const { user, providers } = get()
        const next = [...providers, p]
        set({ providers: next })
        if (user) Storage.set(userKey(user.id, 'providers'), next)
        if (p.isActive) get().setActiveProvider(p.id)
      },
      updateProvider: (id, patch) => {
        const { providers, user } = get()
        const next = providers.map(x => x.id === id ? { ...x, ...patch } : x)
        set({ providers: next })
        if (user) Storage.set(userKey(user.id, 'providers'), next)
      },
      deleteProvider: (id) => {
        const { providers, user, activeProviderId } = get()
        const next = providers.filter(x => x.id !== id)
        set({ providers: next, activeProviderId: activeProviderId === id ? (next[0]?.id ?? null) : activeProviderId })
        if (user) {
          Storage.set(userKey(user.id, 'providers'), next)
          Storage.set(userKey(user.id, 'activeProviderId'), get().activeProviderId)
        }
      },
      setActiveProvider: (id) => {
        const { providers, user } = get()
        const next = providers.map(p => ({ ...p, isActive: p.id === id }))
        set({ providers: next, activeProviderId: id })
        if (user) {
          Storage.set(userKey(user.id, 'providers'), next)
          Storage.set(userKey(user.id, 'activeProviderId'), id)
        }
      },

      createConversation: (title) => {
        const id = Math.random().toString(36).slice(2,9)
        const now = Date.now()
        const conv: Conversation = { id, title: title?.slice(0,40) || 'New Chat', messages: [], createdAt: now, updatedAt: now }
        const next = [conv, ...get().conversations]
        set({ conversations: next, activeConversationId: id })
        const { user } = get(); if (user) Storage.set(userKey(user.id, 'conversations'), next)
        return id
      },
      selectConversation: (id) => set({ activeConversationId: id }),
      deleteConversation: (id) => {
        const next = get().conversations.filter(c => c.id !== id)
        set({ conversations: next, activeConversationId: next[0]?.id ?? null })
        const { user } = get(); if (user) Storage.set(userKey(user.id, 'conversations'), next)
      },
      addMessage: (convId, msg) => {
        const next = get().conversations.map(c => c.id === convId ? { ...c, messages: [...c.messages, msg], updatedAt: Date.now(), title: c.messages.length===0 && msg.role==='user' ? msg.content.slice(0,40) : c.title } : c)
        set({ conversations: next })
        const { user } = get(); if (user) Storage.set(userKey(user.id, 'conversations'), next)
      },
      setThinking: (v) => set({ isThinking: v }),
      getActiveProvider: () => get().providers.find(p => p.id === get().activeProviderId) ?? get().providers[0] ?? null,
      hydrateForUser: (userId) => {
        const p = Storage.get<ProviderConfig[]>(userKey(userId, 'providers'), [])
        const c = Storage.get<Conversation[]>(userKey(userId, 'conversations'), [])
        const active = Storage.get<string | null>(userKey(userId, 'activeProviderId'), null)
        set({ providers: p, conversations: c, activeProviderId: active, activeConversationId: c[0]?.id ?? null })
      }
    }),
    {
      name: 'jarvis-root',
      partialize: (s) => ({ user: s.user })
    }
  )
)
