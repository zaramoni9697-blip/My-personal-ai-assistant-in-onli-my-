import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Home, MessageSquare, Mic, Smartphone, Settings, LogOut, Cpu } from 'lucide-react'
import { useStore } from '../store/useStore'

export function TopBar() {
  const user = useStore(s=>s.user)
  const nav = useNavigate()
  return (
    <div className="h-14 flex items-center justify-between px-4 bg-[#0A0E1A] border-b border-white/5 sticky top-0 z-20">
      <div className="flex items-center gap-3">
        <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-[#00D4FF] to-[#003366] flex items-center justify-center text-white font-bold text-sm">J</div>
        <div>
          <div className="text-white font-bold text-sm leading-none">JARVIS AI</div>
          <div className="text-[#00D4FF] text-[10px]">by Md Foysal • Online</div>
        </div>
      </div>
      <div className="flex items-center gap-2">
        <Link to="/providers" className="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center"><Cpu size={16} className="text-white/70"/></Link>
        <Link to="/settings" className="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center"><Settings size={16} className="text-white/70"/></Link>
        <button onClick={()=>{ localStorage.removeItem('jarvis-root'); location.href='/'}} className="w-8 h-8 rounded-full bg-white/5 flex items-center justify-center"><LogOut size={14} className="text-white/70"/></button>
      </div>
    </div>
  )
}

export function BottomNav() {
  const loc = useLocation()
  const items = [
    { to:'/dashboard', icon: Home, label:'Home' },
    { to:'/chat', icon: MessageSquare, label:'Chat' },
    { to:'/voice', icon: Mic, label:'Voice' },
    { to:'/device', icon: Smartphone, label:'Device' },
  ]
  return (
    <div className="h-16 bg-[#0F1A2E] border-t border-white/5 flex items-center justify-around sticky bottom-0 z-20">
      {items.map(it => {
        const active = loc.pathname === it.to
        return <Link key={it.to} to={it.to} className={`flex flex-col items-center gap-1 px-4 py-2 rounded-xl ${active ? 'text-[#00D4FF] bg-[#00D4FF]/10' : 'text-white/50'}`}>
          <it.icon size={18}/> <span className="text-[10px]">{it.label}</span>
        </Link>
      })}
    </div>
  )
}

export function Shell({ children }: { children: React.ReactNode }) {
  return <div className="min-h-screen bg-[#0A0E1A] text-white flex flex-col max-w-[480px] mx-auto md:max-w-4xl lg:max-w-6xl shadow-2xl md:border-x md:border-white/5">
    <TopBar/>
    <div className="flex-1 overflow-auto">{children}</div>
    <BottomNav/>
  </div>
}
