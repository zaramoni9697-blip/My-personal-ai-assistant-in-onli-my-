import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { ShieldCheck, Plus, Trash2, CheckCircle2, Network, Eye, EyeOff, ArrowRight } from 'lucide-react'
import { useStore } from '../store/useStore'
import { ProviderType, PROVIDER_META } from '../types'
import { testProvider } from '../services/providerAdapters'

export function Providers() {
  const nav = useNavigate()
  const providers = useStore(s=>s.providers)
  const addProvider = useStore(s=>s.addProvider)
  const deleteProvider = useStore(s=>s.deleteProvider)
  const setActive = useStore(s=>s.setActiveProvider)
  const updateProvider = useStore(s=>s.updateProvider)
  const [showAdd, setShowAdd]=useState(false)
  const [testingId, setTestingId]=useState<string|null>(null)

  const handleTest = async (id:string)=>{
    const p = providers.find(x=>x.id===id); if(!p) return
    setTestingId(id)
    const r = await testProvider(p)
    updateProvider(id, { lastTest: { ok:r.ok, msg:r.msg, at:Date.now() } })
    setTestingId(null)
  }

  return (
    <div className="min-h-screen bg-[#0A0E1A] p-4 md:p-6 max-w-4xl mx-auto">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-white font-bold text-lg">AI Provider Setup</h1>
        <Link to="/dashboard" className="text-[#00D4FF] text-sm flex items-center gap-1">Skip <ArrowRight size={14}/></Link>
      </div>

      <div className="bg-[#0F1A2E] rounded-2xl p-4 mb-4 flex gap-3 items-start">
        <ShieldCheck className="text-[#00D4FF] shrink-0" size={20}/>
        <div>
          <p className="text-white text-sm font-medium">Your API keys are encrypted locally</p>
          <p className="text-white/50 text-xs mt-1">Each user has isolated storage. Select your preferred AI provider. You can add multiple and switch anytime.</p>
        </div>
      </div>

      <div className="space-y-3 mb-6">
        {providers.length===0 ? (
          <div className="bg-[#0F1A2E] rounded-2xl py-12 text-center">
            <div className="w-12 h-12 rounded-full bg-white/5 flex items-center justify-center mx-auto mb-3"><Network className="text-white/30"/></div>
            <p className="text-white/60 text-sm">No providers configured</p>
            <p className="text-white/30 text-xs">Tap + to add your first provider</p>
          </div>
        ) : providers.map(p=>(
          <div key={p.id} className="bg-[#0F1A2E] rounded-2xl p-4 flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#00D4FF]/15 flex items-center justify-center text-[#00D4FF] font-bold text-xs">{p.type.slice(0,2).toUpperCase()}</div>
            <div className="flex-1 min-w-0">
              <p className="text-white font-semibold text-sm truncate">{p.displayName || PROVIDER_META[p.type].label}</p>
              <p className="text-xs truncate" style={{ color: p.isActive ? '#00D4FF' : 'rgba(255,255,255,0.4)' }}>{p.modelId} • {p.isActive ? 'Active' : 'Inactive'}</p>
              {p.lastTest && <p className={`text-[10px] mt-1 ${p.lastTest.ok?'text-green-400':'text-red-400'}`}>{p.lastTest.msg.slice(0,80)}</p>}
            </div>
            <div className="flex items-center gap-1">
              {p.isActive && <CheckCircle2 size={18} className="text-[#00D4FF]"/>}
              <button onClick={()=>handleTest(p.id)} disabled={testingId===p.id} className="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center hover:bg-white/10">
                {testingId===p.id ? <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"/> : <Network size={14} className="text-white/60"/>}
              </button>
              <button onClick={()=>deleteProvider(p.id)} className="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center hover:bg-red-500/20"><Trash2 size={14} className="text-white/40"/></button>
            </div>
          </div>
        ))}
      </div>

      {providers.length>0 && (
        <div className="space-y-2 mb-6">
          {providers.filter(p=>!p.isActive).map(p=>(
            <button key={p.id} onClick={()=>setActive(p.id)} className="w-full text-left px-4 py-2 rounded-xl bg-white/5 text-[#00D4FF] text-xs">Set {p.displayName || p.type} as Active</button>
          ))}
        </div>
      )}

      <button onClick={()=>setShowAdd(true)} className="fixed bottom-20 right-4 md:bottom-6 w-14 h-14 rounded-full bg-[#00D4FF] flex items-center justify-center shadow-lg hover:bg-[#00c0e6]">
        <Plus className="text-white" />
      </button>

      <button onClick={()=>nav('/dashboard')} disabled={providers.length===0} className="w-full py-3.5 rounded-xl bg-[#00D4FF] text-white font-bold tracking-widest disabled:opacity-40">CONTINUE TO JARVIS</button>

      {showAdd && <AddDialog onClose={()=>setShowAdd(false)} onSave={(cfg)=>{
        addProvider({ ...cfg, id: Math.random().toString(36).slice(2,9), isActive: providers.length===0 })
        setShowAdd(false)
      }}/>}
    </div>
  )
}

function AddDialog({ onClose, onSave }: { onClose:()=>void; onSave:(c:any)=>void }) {
  const [type, setType]=useState<ProviderType>('gemini')
  const meta = PROVIDER_META[type]
  const [apiKey,setApiKey]=useState(''); const [modelId,setModelId]=useState(meta.defaultModel); const [baseUrl,setBaseUrl]=useState(meta.defaultBaseUrl); const [displayName,setDisplayName]=useState(''); const [showKey,setShowKey]=useState(false)
  // sync defaults when type changes
  const changeType = (t:ProviderType)=>{ setType(t); setModelId(PROVIDER_META[t].defaultModel); setBaseUrl(PROVIDER_META[t].defaultBaseUrl) }
  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50">
      <div className="bg-[#0F1A2E] rounded-2xl w-full max-w-md p-5 space-y-3 max-h-[90vh] overflow-auto">
        <h2 className="text-white font-bold">Add Provider</h2>
        <div>
          <label className="text-white/60 text-xs">Provider</label>
          <select value={type} onChange={e=>changeType(e.target.value as ProviderType)} className="w-full mt-1 px-3 py-2.5 rounded-xl bg-[#0A0E1A] border border-white/10 text-white">
            {(Object.keys(PROVIDER_META) as ProviderType[]).map(k=> <option key={k} value={k}>{PROVIDER_META[k].label}</option>)}
          </select>
        </div>
        <div>
          <label className="text-white/60 text-xs">API Key *</label>
          <div className="relative mt-1">
            <input value={apiKey} onChange={e=>setApiKey(e.target.value)} placeholder={meta.placeholder} type={showKey?'text':'password'} className="w-full px-3 py-2.5 pr-10 rounded-xl bg-[#0A0E1A] border border-white/10 text-white placeholder:text-white/30" />
            <button onClick={()=>setShowKey(!showKey)} className="absolute right-2 top-1/2 -translate-y-1/2 text-white/40">{showKey?<EyeOff size={16}/>:<Eye size={16}/>}</button>
          </div>
        </div>
        <div><label className="text-white/60 text-xs">Model ID *</label><input value={modelId} onChange={e=>setModelId(e.target.value)} className="w-full mt-1 px-3 py-2.5 rounded-xl bg-[#0A0E1A] border border-white/10 text-white" /></div>
        <div><label className="text-white/60 text-xs">Base URL</label><input value={baseUrl} onChange={e=>setBaseUrl(e.target.value)} placeholder="https://api.example.com/v1/" className="w-full mt-1 px-3 py-2.5 rounded-xl bg-[#0A0E1A] border border-white/10 text-white placeholder:text-white/30" /></div>
        <div><label className="text-white/60 text-xs">Display Name (optional)</label><input value={displayName} onChange={e=>setDisplayName(e.target.value)} placeholder="My Gemini" className="w-full mt-1 px-3 py-2.5 rounded-xl bg-[#0A0E1A] border border-white/10 text-white" /></div>
        <div className="flex gap-3 pt-2">
          <button onClick={onClose} className="flex-1 py-2.5 rounded-xl bg-white/10 text-white">Cancel</button>
          <button disabled={!apiKey || !modelId} onClick={()=>onSave({ type, apiKey, modelId, baseUrl, displayName: displayName || meta.label })} className="flex-1 py-2.5 rounded-xl bg-[#00D4FF] text-white font-bold disabled:opacity-40">Save</button>
        </div>
      </div>
    </div>
  )
}
