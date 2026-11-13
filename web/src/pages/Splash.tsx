import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useStore } from '../store/useStore'

export function Splash() {
  const nav = useNavigate()
  const user = useStore(s=>s.user)
  const [show, setShow] = useState(false)
  useEffect(()=>{ setTimeout(()=>setShow(true),100); const t=setTimeout(()=> nav(user ? '/dashboard' : '/login'), 1800); return ()=>clearTimeout(t) },[user])
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gradient-to-b from-[#050A1A] via-[#0A0E1A] to-[#0A2A4A] p-6 text-center">
      <motion.div initial={{ scale:0.8, opacity:0 }} animate={{ scale: show?1:0.8, opacity: show?1:0 }} transition={{ duration:0.8 }} className="flex flex-col items-center">
        <div className="relative w-32 h-32 mb-6">
          <div className="absolute inset-0 rounded-full bg-[#00D4FF]/20 blur-xl animate-pulse" />
          <div className="absolute inset-4 rounded-full bg-gradient-to-br from-[#00D4FF] to-[#003366] flex items-center justify-center text-white text-4xl font-bold">J</div>
          <motion.div className="absolute inset-0 rounded-full border border-[#00D4FF]/30" animate={{ rotate:360 }} transition={{ duration:8, repeat:Infinity, ease:'linear' }} />
        </div>
        <h1 className="text-3xl font-bold text-white tracking-[0.3em]">JARVIS AI</h1>
        <p className="text-[#00D4FF] text-xs tracking-[0.2em] mt-1">by Md Foysal</p>
        <p className="text-white/50 text-sm mt-3">Advanced Personal Assistant</p>
        <div className="mt-8 flex gap-2">
          <div className="w-2 h-2 rounded-full bg-[#00D4FF] animate-bounce" />
          <div className="w-2 h-2 rounded-full bg-[#00D4FF] animate-bounce [animation-delay:150ms]" />
          <div className="w-2 h-2 rounded-full bg-[#00D4FF] animate-bounce [animation-delay:300ms]" />
        </div>
      </motion.div>
      <p className="absolute bottom-6 text-white/20 text-[10px]">v1.0.0 • Secure • Intelligent</p>
    </div>
  )
}
