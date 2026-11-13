import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Settings as SettingsIcon, Mic, Globe, Trash2, LogOut, User, Shield, HelpCircle, Info } from 'lucide-react'
import { Shell } from '../components/Layout'
import { useStore } from '../store/useStore'

export function SettingsPage() {
  const nav = useNavigate()
  const user = useStore(s=>s.user)
  const setUser = useStore(s=>s.setUser)
  const [voiceEnabled, setVoiceEnabled]=useState(true)
  const [wakeEnabled, setWakeEnabled]=useState(false)
  const [ttsRate, setTtsRate]=useState(1)
  const [lang, setLang]=useState('Auto')
  const [showDelete, setShowDelete]=useState(false)

  const logout = ()=>{
    if(user) { localStorage.removeItem(`jarvis:${user.id}:providers`); }
    setUser(null); nav('/login')
  }
  const deleteAccount = ()=>{
    if(!user) return
    localStorage.removeItem(`jarvis:${user.id}:providers`)
    localStorage.removeItem(`jarvis:${user.id}:conversations`)
    localStorage.removeItem(`jarvis:${user.id}:activeProviderId`)
    const users = JSON.parse(localStorage.getItem('jarvis:users')||'[]'); localStorage.setItem('jarvis:users', JSON.stringify(users.filter((u:any)=>u.id!==user.id)))
    setUser(null); nav('/login')
  }

  return (
    <Shell>
      <div className="p-4 space-y-4">
        <div className="bg-[#0F1A2E] rounded-2xl p-4 flex items-center gap-3">
          <div className="w-12 h-12 rounded-full bg-[#00D4FF] flex items-center justify-center text-white font-bold">{user?.displayName?.slice(0,2).toUpperCase()||'MF'}</div>
          <div className="flex-1"><p className="text-white font-bold text-sm">{user?.displayName||'Md Foysal'}</p><p className="text-white/40 text-xs">{user?.email||'md.foysal@example.com'}</p></div>
        </div>

        <p className="text-white font-semibold text-xs">Voice & Speech</p>
        <div className="bg-[#0F1A2E] rounded-2xl p-3 space-y-3">
          <ToggleRow title="Voice Responses" subtitle="Speak answers aloud" checked={voiceEnabled} onChange={setVoiceEnabled}/>
          <div className="h-px bg-white/5"/>
          <ToggleRow title="Wake Word" subtitle="সবসময় প্রস্তুত (foreground service)" checked={wakeEnabled} onChange={setWakeEnabled}/>
          <div className="h-px bg-white/5"/>
          <div className="flex items-center justify-between">
            <span className="text-white text-sm">Speech Rate</span>
            <div className="flex items-center gap-2">
              <input type="range" min={0.5} max={2} step={0.1} value={ttsRate} onChange={e=>setTtsRate(parseFloat(e.target.value))} className="accent-[#00D4FF]"/>
              <span className="text-[#00D4FF] text-xs">{ttsRate.toFixed(1)}x</span>
            </div>
          </div>
        </div>

        <p className="text-white font-semibold text-xs">General</p>
        <div className="bg-[#0F1A2E] rounded-2xl overflow-hidden">
          <SettingRow icon={Globe} title="Language" subtitle={lang} onClick={()=>setLang(lang==='Auto'?'English': lang==='English'?'বাংলা':'Auto')}/>
          <SettingRow icon={Shield} title="AI Providers" subtitle="Manage API keys" onClick={()=>nav('/providers')}/>
          <SettingRow icon={Trash2} title="Clear Conversations" subtitle="Delete all history" onClick={()=>{ if(confirm('Clear all conversations?')) { useStore.getState().conversations.forEach(c=>useStore.getState().deleteConversation(c.id)) } }}/>
        </div>

        <p className="text-white font-semibold text-xs">Support</p>
        <div className="bg-[#0F1A2E] rounded-2xl overflow-hidden">
          <SettingRow icon={Info} title="About JARVIS AI" subtitle="v1.0.0 by Md Foysal"/>
          <SettingRow icon={Shield} title="Privacy Policy" subtitle="Encrypted local storage"/>
          <SettingRow icon={HelpCircle} title="Help & Feedback" subtitle="Contact support"/>
        </div>

        <button onClick={logout} className="w-full py-3 rounded-xl border border-white/10 text-white flex items-center justify-center gap-2"><LogOut size={16}/> Logout</button>
        <button onClick={()=>setShowDelete(true)} className="w-full py-3 rounded-xl bg-red-900/30 text-red-400 flex items-center justify-center gap-2"><Trash2 size={16}/> Delete Account</button>
      </div>

      {showDelete && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-6 z-50">
          <div className="bg-[#0F1A2E] rounded-2xl p-6 max-w-sm w-full text-center">
            <h3 className="text-white font-bold">Delete Account?</h3>
            <p className="text-white/50 text-sm mt-2">This will permanently delete your data, conversations and API configs.</p>
            <div className="flex gap-3 mt-5">
              <button onClick={()=>setShowDelete(false)} className="flex-1 py-2.5 rounded-xl bg-white/10 text-white">Cancel</button>
              <button onClick={deleteAccount} className="flex-1 py-2.5 rounded-xl bg-red-600 text-white font-bold">Delete</button>
            </div>
          </div>
        </div>
      )}
    </Shell>
  )
}

function ToggleRow({ title, subtitle, checked, onChange }: { title:string; subtitle:string; checked:boolean; onChange:(v:boolean)=>void }) {
  return <div className="flex items-center justify-between">
    <div><p className="text-white text-sm">{title}</p><p className="text-white/40 text-xs">{subtitle}</p></div>
    <button onClick={()=>onChange(!checked)} className={`w-11 h-6 rounded-full p-1 transition ${checked?'bg-[#00D4FF]':'bg-white/10'}`}>
      <div className={`w-4 h-4 rounded-full bg-white transition ${checked?'translate-x-5':'translate-x-0'}`} />
    </button>
  </div>
}
function SettingRow({ icon:Icon, title, subtitle, onClick }: any) {
  return <button onClick={onClick} className="w-full flex items-center gap-3 px-4 py-3.5 hover:bg-white/5 text-left">
    <Icon size={16} className="text-[#00D4FF]"/><div className="flex-1"><p className="text-white text-sm">{title}</p><p className="text-white/40 text-xs">{subtitle}</p></div>
    <span className="text-white/20">›</span>
  </button>
}
