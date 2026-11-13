import { useState } from 'react'
import { Lightbulb, Volume2, Battery, Smartphone, AlarmClock, Settings, Wifi, AppWindow, CheckCircle2, XCircle } from 'lucide-react'
import { Shell } from '../components/Layout'

export function Device() {
  const [flashOn, setFlashOn]=useState(false)
  const [alarmH, setAlarmH]=useState('7'); const [alarmM, setAlarmM]=useState('00')
  const [toast, setToast]=useState<string|null>(null)
  const show = (m:string)=>{ setToast(m); setTimeout(()=>setToast(null),2000) }

  return (
    <Shell>
      <div className="p-4 space-y-4">
        {toast && <div className="bg-[#00D4FF]/20 border border-[#00D4FF]/30 rounded-xl px-4 py-2 flex items-center gap-2 text-white text-sm"><CheckCircle2 size={16} className="text-[#00D4FF]"/>{toast}</div>}

        <div>
          <p className="text-white font-semibold text-sm mb-2">Permissions</p>
          <div className="bg-[#0F1A2E] rounded-2xl p-4 space-y-3">
            <PermRow icon={Lightbulb} title="Microphone" desc="Voice commands" granted />
            <PermRow icon={Smartphone} title="Phone" desc="Make calls via intents" granted={false} />
            <PermRow icon={Battery} title="Notifications" desc="Foreground service" granted />
            <p className="text-white/30 text-[10px]">All actions request confirmation and use Android intents. No hidden permissions. Web version simulates device controls.</p>
          </div>
        </div>

        <div>
          <p className="text-white font-semibold text-sm mb-2">Quick Controls</p>
          <div className="grid grid-cols-3 gap-3">
            <button onClick={()=>{ setFlashOn(!flashOn); show(flashOn?'Flashlight OFF':'Flashlight ON') }} className="bg-[#0F1A2E] rounded-2xl p-4 flex flex-col items-center gap-2">
              <Lightbulb className={flashOn?'text-[#FFD600]':'text-[#00D4FF]'} size={20}/><span className="text-white text-xs">{flashOn?'ON':'OFF'}</span><span className="text-white/40 text-[10px]">Flashlight</span>
            </button>
            <button onClick={()=>show('Volume control simulated')} className="bg-[#0F1A2E] rounded-2xl p-4 flex flex-col items-center gap-2">
              <Volume2 className="text-[#00D4FF]" size={20}/><span className="text-white text-xs">Adjust</span><span className="text-white/40 text-[10px]">Volume</span>
            </button>
            <div className="bg-[#0F1A2E] rounded-2xl p-4 flex flex-col items-center gap-2">
              <Battery className="text-[#00D4FF]" size={20}/><span className="text-white text-xs">82%</span><span className="text-white/40 text-[10px]">Battery</span>
            </div>
          </div>
        </div>

        <div className="bg-[#0F1A2E] rounded-2xl p-4">
          <div className="flex items-center gap-2 mb-3"><AlarmClock size={16} className="text-[#00D4FF]"/><span className="text-white font-medium text-sm">Set Alarm</span></div>
          <div className="flex items-center gap-2">
            <input value={alarmH} onChange={e=>setAlarmH(e.target.value)} placeholder="Hour" className="flex-1 px-3 py-2 rounded-xl bg-[#0A0E1A] border border-white/10 text-white text-sm"/>
            <span className="text-white">:</span>
            <input value={alarmM} onChange={e=>setAlarmM(e.target.value)} placeholder="Min" className="flex-1 px-3 py-2 rounded-xl bg-[#0A0E1A] border border-white/10 text-white text-sm"/>
            <button onClick={()=>show(`Alarm set for ${alarmH}:${alarmM}`)} className="px-4 py-2 rounded-xl bg-[#00D4FF] text-white text-sm font-medium">Set</button>
          </div>
          <p className="text-white/30 text-xs mt-2">On Android this fires AlarmClock intent. On web it simulates.</p>
        </div>

        <div className="bg-[#0F1A2E] rounded-2xl p-4">
          <p className="text-white font-medium text-sm mb-2">Device Info</p>
          <p className="text-white/60 text-xs">Device: Web • {navigator.userAgent.slice(0,80)}</p>
          <p className="text-[#00D4FF] text-xs mt-2">Battery: 82% • Android simulation</p>
        </div>

        <div className="flex gap-3">
          <button onClick={()=>show('Opening Settings... (simulated)')} className="flex-1 bg-[#0F1A2E] rounded-xl py-3 flex items-center justify-center gap-2 text-white text-sm"><Settings size={16}/> Settings</button>
          <button onClick={()=>show('Opening Wi-Fi settings...')} className="flex-1 bg-[#0F1A2E] rounded-xl py-3 flex items-center justify-center gap-2 text-white text-sm"><Wifi size={16}/> Wi-Fi</button>
        </div>

        <div className="bg-[#0F1A2E] rounded-2xl p-4">
          <div className="flex items-center gap-2 mb-2"><AppWindow size={16} className="text-[#00D4FF]"/><span className="text-white text-sm">Installed Apps (simulated)</span></div>
          <div className="space-y-2">
            {['Phone','Messages','Camera','Settings','Chrome','Maps','YouTube','Gmail'].map(app=>(
              <div key={app} className="flex items-center justify-between bg-[#0A0E1A] rounded-xl px-3 py-2">
                <span className="text-white text-sm">{app}</span><button onClick={()=>show(`Opened ${app}`)} className="text-[#00D4FF] text-xs">Open</button>
              </div>
            ))}
          </div>
        </div>
      </div>
    </Shell>
  )
}

function PermRow({ icon:Icon, title, desc, granted }: { icon:any; title:string; desc:string; granted:boolean }) {
  return <div className="flex items-center gap-3">
    <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${granted?'bg-[#00D4FF]/15':'bg-white/5'}`}><Icon size={16} className={granted?'text-[#00D4FF]':'text-white/40'}/></div>
    <div className="flex-1"><p className="text-white text-xs font-medium">{title}</p><p className="text-white/40 text-[10px]">{desc}</p></div>
    {granted ? <CheckCircle2 size={16} className="text-[#00D4FF]"/> : <XCircle size={16} className="text-white/20"/>}
  </div>
}
