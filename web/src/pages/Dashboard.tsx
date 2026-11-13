import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Mic, Volume2, Square, MessageSquarePlus, Mic2, Smartphone, Lightbulb, ChevronRight, MessageCircle } from 'lucide-react'
import { Shell } from '../components/Layout'
import { AICore, StatusChips } from '../components/AICore'
import { useStore } from '../store/useStore'

export function Dashboard() {
  const nav = useNavigate()
  const conversations = useStore(s=>s.conversations)
  const [listening, setListening]=useState(false)
  const [thinking, setThinking]=useState(false)
  const [speaking, setSpeaking]=useState(false)

  return (
    <Shell>
      <div className="p-4 space-y-4">
        <div className="bg-[#0F1A2E] rounded-3xl p-6 flex flex-col items-center">
          <AICore listening={listening} thinking={thinking} speaking={speaking} />
          <div className="mt-4"><StatusChips listening={listening} thinking={thinking} speaking={speaking} /></div>
          <div className="flex items-center gap-3 mt-6">
            <button onClick={()=>{ setListening(!listening); setThinking(false); setSpeaking(false)}} className={`flex items-center gap-2 px-5 py-2.5 rounded-full font-medium text-sm ${listening?'bg-[#00D4FF] text-white':'bg-white/10 text-white'}`}>
              <Mic size={16}/> {listening?'Listening...':'Tap to Speak'}
            </button>
            <button onClick={()=>setSpeaking(!speaking)} className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center"><Volume2 size={16} className={speaking?'text-[#00D4FF]':'text-white'}/></button>
            <button onClick={()=>{setListening(false); setThinking(false); setSpeaking(false)}} className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center"><Square size={14} className="text-white"/></button>
          </div>
        </div>

        <div>
          <p className="text-white font-semibold text-sm mb-2">Quick Actions</p>
          <div className="grid grid-cols-3 gap-3">
            {[
              { label:'New Chat', icon: MessageSquarePlus, color:'#00D4FF', to:'/chat' },
              { label:'Voice Mode', icon: Mic2, color:'#00FFD1', to:'/voice' },
              { label:'Device', icon: Smartphone, color:'#FF6B35', to:'/device' },
            ].map(c=>(
              <button key={c.label} onClick={()=>nav(c.to)} className="bg-[#0F1A2E] rounded-2xl p-4 flex flex-col items-center gap-2 hover:bg-[#162840]">
                <div className="w-11 h-11 rounded-xl flex items-center justify-center" style={{ background: c.color+'20' }}><c.icon size={20} style={{ color:c.color }}/></div>
                <span className="text-white text-xs font-medium">{c.label}</span>
              </button>
            ))}
          </div>
        </div>

        <div className="bg-[#0F1A2E] rounded-2xl p-4">
          <div className="flex items-center gap-2 mb-3"><Lightbulb size={16} className="text-[#00D4FF]"/><span className="text-white text-sm font-medium">Try saying</span></div>
          <div className="flex flex-wrap gap-2">
            {['তুমি কে?','Open my settings','Explain quantum computing','Write a Python function'].map(t=>(
              <button key={t} onClick={()=>nav('/chat')} className="px-3 py-1.5 rounded-full bg-white/5 text-white/80 text-xs hover:bg-white/10">{t}</button>
            ))}
          </div>
        </div>

        <div>
          <div className="flex items-center justify-between mb-2">
            <p className="text-white font-semibold text-sm">Recent Conversations</p>
            <button onClick={()=>nav('/chat')} className="text-[#00D4FF] text-xs">View all</button>
          </div>
          {conversations.length===0 ? (
            <button onClick={()=>nav('/chat')} className="w-full bg-[#0F1A2E]/60 rounded-xl p-3 flex items-center gap-3 text-left">
              <div className="w-9 h-9 rounded-lg bg-[#00D4FF]/20 flex items-center justify-center"><MessageCircle size={16} className="text-[#00D4FF]"/></div>
              <div className="flex-1"><p className="text-white text-sm">Welcome to JARVIS</p><p className="text-white/40 text-xs">2 min ago • Gemini</p></div>
              <ChevronRight size={16} className="text-white/20"/>
            </button>
          ) : conversations.slice(0,3).map(c=>(
            <button key={c.id} onClick={()=>nav('/chat')} className="w-full bg-[#0F1A2E] rounded-xl p-3 flex items-center gap-3 text-left mb-2">
              <div className="w-9 h-9 rounded-lg bg-[#00D4FF]/20 flex items-center justify-center text-[#00D4FF] text-xs font-bold">{c.title.slice(0,1)}</div>
              <div className="flex-1 min-w-0"><p className="text-white text-sm truncate">{c.title}</p><p className="text-white/40 text-xs">{c.messages.length} messages</p></div>
              <ChevronRight size={16} className="text-white/20"/>
            </button>
          ))}
        </div>
      </div>
    </Shell>
  )
}
