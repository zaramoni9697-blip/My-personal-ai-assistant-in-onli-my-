import { useState, useEffect, useRef } from 'react'
import { Mic, Square, VolumeX, Volume2 } from 'lucide-react'
import { Shell } from '../components/Layout'
import { AICore } from '../components/AICore'
import { localFallbackReply } from '../utils/lang'

export function Voice() {
  const [listening, setListening]=useState(false)
  const [speaking, setSpeaking]=useState(false)
  const [transcript, setTranscript]=useState('')
  const [lang, setLang]=useState<'auto'|'en'|'bn'>('auto')
  const [wakeEnabled, setWakeEnabled]=useState(false)
  const [history, setHistory]=useState<{role:'user'|'assistant'; text:string}[]>([])
  const [permissionGranted, setPermissionGranted]=useState(false)
  const recogRef = useRef<any>(null)

  const hasRecognition = typeof window !== 'undefined' && ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window)

  useEffect(()=>{
    // @ts-ignore
    const SR = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
    if(!SR) return
    const recog = new SR()
    recog.continuous = false
    recog.interimResults = true
    recog.lang = lang==='bn' ? 'bn-BD' : lang==='en' ? 'en-US' : navigator.language || 'en-US'
    recog.onstart = ()=> setListening(true)
    recog.onend = ()=> setListening(false)
    recog.onerror = (e:any)=>{ setListening(false); console.error(e) }
    recog.onresult = (e:any)=>{
      const text = Array.from(e.results).map((r:any)=>r[0].transcript).join('')
      setTranscript(text)
      if(e.results[0].isFinal){
        const userText = text.trim()
        if(!userText) return
        const reply = localFallbackReply(userText)
        setHistory(h=>[...h, {role:'user', text:userText}, {role:'assistant', text:reply}])
        speak(reply)
      }
    }
    recogRef.current = recog
  }, [lang])

  const speak = (text:string)=>{
    if(!('speechSynthesis' in window)) return
    window.speechSynthesis.cancel()
    const ut = new SpeechSynthesisUtterance(text)
    // pick Bengali voice if needed
    const isBn = /[\u0980-\u09FF]/.test(text)
    ut.lang = isBn ? 'bn-BD' : 'en-US'
    ut.onstart = ()=> setSpeaking(true)
    ut.onend = ()=> setSpeaking(false)
    window.speechSynthesis.speak(ut)
  }

  const toggleListen = async ()=>{
    if(!hasRecognition) { alert('Speech Recognition not supported in this browser. Try Chrome/Edge.'); return }
    if(listening){ recogRef.current?.stop(); return }
    if(!permissionGranted){
      try { await navigator.mediaDevices.getUserMedia({ audio:true }); setPermissionGranted(true) } catch { alert('Microphone permission denied'); return }
    }
    setTranscript('')
    try { recogRef.current?.start() } catch {}
  }

  return (
    <Shell>
      <div className="p-4 space-y-4">
        <div className="bg-[#0F1A2E] rounded-3xl p-5 flex flex-col items-center">
          <div className="flex items-center justify-between w-full mb-3">
            <span className="text-white/60 text-xs">Language</span>
            <div className="flex bg-white/5 rounded-full p-1">
              {(['auto','en','bn'] as const).map(v=>(
                <button key={v} onClick={()=>setLang(v)} className={`px-3 py-1 rounded-full text-xs font-medium ${lang===v?'bg-[#00D4FF] text-white':'text-white/60'}`}>{v==='auto'?'Auto':v.toUpperCase()}</button>
              ))}
            </div>
          </div>
          <AICore listening={listening} speaking={speaking} />
          <p className={`mt-3 text-sm font-medium ${listening?'text-[#00FF88]': speaking?'text-[#00D4FF]':'text-white/60'}`}>
            {listening ? 'Listening... speak now' : speaking ? 'Speaking...' : 'Tap mic to speak'}
          </p>
          {transcript && <div className="mt-3 w-full bg-white/5 rounded-xl p-3 text-white text-sm">{transcript}</div>}
          <div className="flex items-center gap-3 mt-5">
            <button onClick={toggleListen} className={`w-16 h-16 rounded-full flex items-center justify-center shadow-lg ${listening?'bg-red-500':'bg-[#00D4FF]'}`}>
              {listening ? <Square className="text-white" size={20}/> : <Mic className="text-white" size={24}/>}
            </button>
            <button onClick={()=>window.speechSynthesis.cancel()} className="w-12 h-12 rounded-full bg-white/5 flex items-center justify-center"><VolumeX size={18} className="text-white"/></button>
          </div>
          <label className="flex items-center gap-2 mt-4 text-sm">
            <input type="checkbox" checked={wakeEnabled} onChange={e=>setWakeEnabled(e.target.checked)} className="accent-[#00D4FF]"/>
            <span className="text-white text-xs">Wake word: সবসময় প্রস্তুত</span>
          </label>
          <p className="text-white/30 text-[10px] text-center mt-1">Foreground service with notification (Android) • Web uses browser speech</p>
        </div>

        <div>
          <p className="text-white font-semibold text-sm mb-2">Conversation</p>
          {history.length===0 ? <p className="text-white/30 text-xs text-center py-8">Voice history will appear here</p> : (
            <div className="space-y-2 max-h-[260px] overflow-auto">
              {history.map((h,i)=>(
                <div key={i} className={`flex ${h.role==='user'?'justify-end':'justify-start'}`}>
                  <div className={`max-w-[80%] rounded-2xl px-3 py-2 text-xs ${h.role==='user'?'bg-[#00D4FF] text-white':'bg-[#0F1A2E] text-white'}`}>{h.text}
                    {h.role==='assistant' && <button onClick={()=>speak(h.text)} className="ml-2 inline-flex"><Volume2 size={12}/></button>}
                  </div>
                </div>
              ))}
            </div>
          )}
          {history.length>0 && <button onClick={()=>setHistory([])} className="text-[#00D4FF] text-xs mt-2">Clear</button>}
        </div>
      </div>
    </Shell>
  )
}
