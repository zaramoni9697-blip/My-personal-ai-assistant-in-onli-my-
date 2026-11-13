import { useState, useRef, useEffect } from 'react'
import { Send, Plus, History, Trash2, X, Bot, User } from 'lucide-react'
import { Shell } from '../components/Layout'
import { useStore } from '../store/useStore'
import { chatWithProvider } from '../services/providerAdapters'
import { localFallbackReply } from '../utils/lang'

export function Chat() {
  const store = useStore()
  const activeId = store.activeConversationId
  const activeConv = store.conversations.find(c=>c.id===activeId)
  const messages = activeConv?.messages ?? []
  const [input, setInput]=useState('')
  const [showHistory, setShowHistory]=useState(false)
  const [error, setError]=useState<string|null>(null)
  const listRef = useRef<HTMLDivElement>(null)

  useEffect(()=>{ if(listRef.current) listRef.current.scrollTop = listRef.current.scrollHeight }, [messages, store.isThinking])

  const send = async ()=>{
    if(!input.trim() || store.isThinking) return
    const text = input.trim(); setInput(''); setError(null)
    let convId = activeId
    if(!convId) convId = store.createConversation(text.slice(0,40))
    const userMsg = { id: Math.random().toString(36).slice(2,9), role:'user' as const, content:text, timestamp:Date.now() }
    store.addMessage(convId, userMsg)
    store.setThinking(true)
    try {
      const provider = store.getActiveProvider()
      // history for provider: send all messages including new user
      const history = [...(store.conversations.find(c=>c.id===convId)?.messages ?? []), userMsg].map(m=>({ id:m.id, role:m.role, content:m.content, timestamp:m.timestamp }))
      let replyText = ''
      if (provider) {
        const r = await chatWithProvider(provider, history as any)
        if (r.ok) replyText = r.content
        else { setError(r.error || 'Provider error'); replyText = localFallbackReply(text) + `\n\n⚠️ Provider error: ${r.error?.slice(0,200)}` }
      } else {
        replyText = localFallbackReply(text)
      }
      const assistantMsg = { id: Math.random().toString(36).slice(2,9), role:'assistant' as const, content: replyText, timestamp:Date.now(), providerType: provider?.type, modelId: provider?.modelId }
      store.addMessage(convId, assistantMsg)
      // Trigger speech if enabled elsewhere? We can speak here optionally
    } catch (e:any) { setError(e.message) }
    finally { store.setThinking(false) }
  }

  return (
    <Shell>
      <div className="flex flex-col h-[calc(100vh-112px)]">
        <div className="flex items-center justify-between px-4 py-3 border-b border-white/5 bg-[#0A0E1A] sticky top-0">
          <h2 className="text-white font-semibold">JARVIS Chat</h2>
          <div className="flex gap-2">
            <button onClick={()=>store.createConversation()} className="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center"><Plus size={16} className="text-white"/></button>
            <button onClick={()=>setShowHistory(true)} className="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center"><History size={16} className="text-white"/></button>
          </div>
        </div>

        <div ref={listRef} className="flex-1 overflow-auto p-4 space-y-3">
          {messages.length===0 && !store.isThinking ? (
            <div className="flex flex-col items-center justify-center h-full text-center py-12">
              <div className="w-16 h-16 rounded-2xl bg-[#00D4FF]/10 flex items-center justify-center mb-3"><Bot className="text-[#00D4FF]/50" size={28}/></div>
              <p className="text-white font-medium">How can I help you today?</p>
              <p className="text-white/40 text-xs mt-1">Ask coding, math, translation, or anything.</p>
              <div className="flex flex-wrap gap-2 justify-center mt-4">
                {['Who created you?','তুমি কে?','Explain Android Compose'].map(q=>(
                  <button key={q} onClick={()=>setInput(q)} className="px-3 py-1.5 rounded-full bg-white/5 text-white/70 text-xs hover:bg-white/10">{q}</button>
                ))}
              </div>
            </div>
          ) : (
            <>
              {messages.map(m=>{
                const isUser = m.role==='user'
                return (
                  <div key={m.id} className={`flex gap-2 ${isUser?'justify-end':'justify-start'}`}>
                    {!isUser && <div className="w-7 h-7 rounded-full bg-[#00D4FF] flex items-center justify-center shrink-0 text-white text-xs font-bold">J</div>}
                    <div className={`max-w-[78%] rounded-2xl px-3.5 py-2.5 ${isUser ? 'bg-[#00D4FF] text-white rounded-br-sm' : 'bg-[#0F1A2E] text-white rounded-bl-sm'}`}>
                      <p className="text-[13px] leading-5 whitespace-pre-wrap">{m.content}</p>
                      {m.modelId && <p className="text-[10px] opacity-50 mt-1">{m.providerType} • {m.modelId}</p>}
                    </div>
                  </div>
                )
              })}
              {store.isThinking && (
                <div className="flex gap-2">
                  <div className="w-7 h-7 rounded-full bg-[#00D4FF]/50 flex items-center justify-center text-white text-xs font-bold">J</div>
                  <div className="bg-[#0F1A2E] rounded-2xl px-4 py-3 flex items-center gap-2"><div className="w-3 h-3 border-2 border-white/20 border-t-[#00D4FF] rounded-full animate-spin"/><span className="text-white/50 text-xs">JARVIS is thinking...</span></div>
                </div>
              )}
            </>
          )}
        </div>

        <div className="p-3 bg-[#0F1A2E] border-t border-white/5">
          {error && <div className="mb-2 bg-red-900/30 rounded-lg px-3 py-2 flex items-center gap-2"><span className="text-red-400 text-xs flex-1">{error}</span><button onClick={()=>setError(null)}><X size={14} className="text-white/50"/></button></div>}
          <div className="flex gap-2 items-end">
            <textarea value={input} onChange={e=>setInput(e.target.value)} onKeyDown={e=>{ if(e.key==='Enter' && !e.shiftKey){ e.preventDefault(); send() }}} placeholder="Ask Jarvis anything..." rows={1}
              className="flex-1 px-4 py-3 rounded-3xl bg-[#0A0E1A] border border-white/10 text-white placeholder:text-white/30 text-sm resize-none outline-none focus:border-[#00D4FF] max-h-24" />
            <button onClick={send} disabled={!input.trim() || store.isThinking} className="w-12 h-12 rounded-full bg-[#00D4FF] flex items-center justify-center disabled:opacity-40 shrink-0">
              {store.isThinking ? <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"/> : <Send size={18} className="text-white"/>}
            </button>
          </div>
        </div>
      </div>

      {showHistory && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex justify-end">
          <div className="w-[85%] max-w-sm bg-[#0A0E1A] h-full p-4 flex flex-col">
            <div className="flex items-center justify-between mb-4"><h3 className="text-white font-bold">Conversations</h3><button onClick={()=>setShowHistory(false)} className="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center"><X size={16} className="text-white"/></button></div>
            <div className="flex-1 overflow-auto space-y-2">
              {store.conversations.length===0 ? <p className="text-white/40 text-sm">No conversations yet</p> : store.conversations.map(c=>(
                <div key={c.id} className={`rounded-xl p-3 flex items-center gap-3 ${c.id===activeId?'bg-[#00D4FF]/10 border border-[#00D4FF]/20':'bg-[#0F1A2E]'}`}>
                  <button onClick={()=>{ store.selectConversation(c.id); setShowHistory(false)}} className="flex-1 text-left">
                    <p className="text-white text-sm truncate">{c.title}</p><p className="text-white/40 text-xs">{c.messages.length} messages</p>
                  </button>
                  <button onClick={()=>store.deleteConversation(c.id)} className="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center"><Trash2 size={14} className="text-white/40"/></button>
                </div>
              ))}
            </div>
            <button onClick={()=>{ store.createConversation(); setShowHistory(false)}} className="mt-4 w-full py-3 rounded-xl bg-[#00D4FF] text-white font-bold">New Conversation</button>
          </div>
        </div>
      )}
    </Shell>
  )
}
