import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useStore } from '../store/useStore'
import { Mail, Lock, User, Eye, EyeOff } from 'lucide-react'

function validateEmail(e:string){ return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(e) }

export function Login() {
  const nav = useNavigate()
  const setUser = useStore(s=>s.setUser)
  const [email, setEmail]=useState('')
  const [pass, setPass]=useState('')
  const [show, setShow]=useState(false)
  const [err, setErr]=useState<string|null>(null)
  const [loading, setLoading]=useState(false)

  const submit = async (e:React.FormEvent)=>{
    e.preventDefault(); setErr(null)
    if(!validateEmail(email)) return setErr('Invalid email format')
    if(pass.length<6) return setErr('Password must be at least 6 characters')
    setLoading(true)
    // Mock auth - check localStorage users
    const users = JSON.parse(localStorage.getItem('jarvis:users')||'[]') as any[]
    const found = users.find(u=>u.email.toLowerCase()===email.toLowerCase())
    if(!found) { setLoading(false); return setErr('Account not found. Please sign up.') }
    if(found.password !== pass) { setLoading(false); return setErr('Incorrect password') }
    setUser({ id: found.id, email: found.email, displayName: found.displayName })
    setLoading(false); nav('/providers')
  }

  return (
    <div className="min-h-screen bg-[#0A0E1A] flex items-center justify-center p-6">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-[#00D4FF] to-[#003366] mx-auto flex items-center justify-center text-white font-bold">J</div>
          <h1 className="text-white text-2xl font-bold mt-4">Welcome Back</h1>
          <p className="text-white/50 text-sm">Sign in to JARVIS AI</p>
        </div>
        <form onSubmit={submit} className="bg-[#0F1A2E] rounded-2xl p-5 space-y-4">
          <div className="relative">
            <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-[#00D4FF]" size={16}/>
            <input value={email} onChange={e=>setEmail(e.target.value)} placeholder="Email" className="w-full pl-10 pr-3 py-3 rounded-xl bg-[#0A0E1A] border border-white/10 text-white placeholder:text-white/30 outline-none focus:border-[#00D4FF]" />
          </div>
          <div className="relative">
            <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-[#00D4FF]" size={16}/>
            <input type={show?'text':'password'} value={pass} onChange={e=>setPass(e.target.value)} placeholder="Password" className="w-full pl-10 pr-10 py-3 rounded-xl bg-[#0A0E1A] border border-white/10 text-white placeholder:text-white/30 outline-none focus:border-[#00D4FF]" />
            <button type="button" onClick={()=>setShow(!show)} className="absolute right-3 top-1/2 -translate-y-1/2 text-white/40">{show?<EyeOff size={16}/>:<Eye size={16}/>}</button>
          </div>
          {err && <p className="text-red-400 text-xs">{err}</p>}
          <div className="text-right"><Link to="/forgot" className="text-[#00D4FF] text-xs">Forgot Password?</Link></div>
          <button disabled={loading} className="w-full py-3 rounded-xl bg-[#00D4FF] text-white font-bold tracking-widest hover:bg-[#00c0e6] disabled:opacity-50">{loading?'SIGNING IN...':'LOGIN'}</button>
        </form>
        <p className="text-center text-white/50 text-sm mt-4">Don&apos;t have an account? <Link to="/signup" className="text-[#00D4FF] font-bold">Sign Up</Link></p>
      </div>
    </div>
  )
}

export function Signup() {
  const nav = useNavigate()
  const setUser = useStore(s=>s.setUser)
  const [name,setName]=useState(''); const [email,setEmail]=useState(''); const [pass,setPass]=useState(''); const [confirm,setConfirm]=useState(''); const [err,setErr]=useState<string|null>(null); const [loading,setLoading]=useState(false)
  const submit = async (e:React.FormEvent)=>{
    e.preventDefault(); setErr(null)
    if(!name.trim()) return setErr('Display name required')
    if(!validateEmail(email)) return setErr('Invalid email')
    if(pass.length<6) return setErr('Password must be at least 6 characters')
    if(pass!==confirm) return setErr('Passwords do not match')
    setLoading(true)
    const users = JSON.parse(localStorage.getItem('jarvis:users')||'[]') as any[]
    if(users.find(u=>u.email.toLowerCase()===email.toLowerCase())) { setLoading(false); return setErr('Account already exists') }
    const newUser = { id: Math.random().toString(36).slice(2,9), email, displayName: name, password: pass }
    users.push(newUser); localStorage.setItem('jarvis:users', JSON.stringify(users))
    setUser({ id:newUser.id, email:newUser.email, displayName:newUser.displayName })
    setLoading(false); nav('/providers')
  }
  return (
    <div className="min-h-screen bg-[#0A0E1A] flex items-center justify-center p-6">
      <div className="w-full max-w-sm">
        <div className="text-center mb-6"><h1 className="text-white text-2xl font-bold">Create Account</h1><p className="text-white/50 text-xs">Join JARVIS AI by Md Foysal</p></div>
        <form onSubmit={submit} className="bg-[#0F1A2E] rounded-2xl p-5 space-y-3">
          <div className="relative"><User className="absolute left-3 top-1/2 -translate-y-1/2 text-[#00D4FF]" size={16}/><input value={name} onChange={e=>setName(e.target.value)} placeholder="Full Name" className="w-full pl-10 pr-3 py-3 rounded-xl bg-[#0A0E1A] border border-white/10 text-white outline-none focus:border-[#00D4FF]" /></div>
          <div className="relative"><Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-[#00D4FF]" size={16}/><input value={email} onChange={e=>setEmail(e.target.value)} placeholder="Email" className="w-full pl-10 pr-3 py-3 rounded-xl bg-[#0A0E1A] border border-white/10 text-white outline-none focus:border-[#00D4FF]" /></div>
          <input type="password" value={pass} onChange={e=>setPass(e.target.value)} placeholder="Password" className="w-full px-3 py-3 rounded-xl bg-[#0A0E1A] border border-white/10 text-white outline-none focus:border-[#00D4FF]" />
          <input type="password" value={confirm} onChange={e=>setConfirm(e.target.value)} placeholder="Confirm Password" className="w-full px-3 py-3 rounded-xl bg-[#0A0E1A] border border-white/10 text-white outline-none focus:border-[#00D4FF]" />
          {err && <p className="text-red-400 text-xs">{err}</p>}
          <button disabled={loading} className="w-full py-3 rounded-xl bg-[#00D4FF] text-white font-bold">{loading?'CREATING...':'SIGN UP'}</button>
        </form>
        <p className="text-center mt-4"><Link to="/login" className="text-[#00D4FF] text-sm">Already have account? Login</Link></p>
      </div>
    </div>
  )
}

export function Forgot() {
  const [email,setEmail]=useState(''); const [msg,setMsg]=useState<string|null>(null); const [err,setErr]=useState<string|null>(null)
  const submit=(e:React.FormEvent)=>{
    e.preventDefault(); setErr(null); setMsg(null)
    if(!validateEmail(email)) return setErr('Invalid email')
    const users = JSON.parse(localStorage.getItem('jarvis:users')||'[]') as any[]
    if(!users.find(u=>u.email.toLowerCase()===email.toLowerCase())) return setErr('No account with this email')
    setMsg(`Password reset instructions sent to ${email} (simulated). Check your inbox.`)
  }
  return (
    <div className="min-h-screen bg-[#0A0E1A] flex items-center justify-center p-6">
      <form onSubmit={submit} className="w-full max-w-sm bg-[#0F1A2E] rounded-2xl p-6 space-y-4 text-center">
        <h1 className="text-white font-bold text-xl">Forgot Password</h1>
        <input value={email} onChange={e=>setEmail(e.target.value)} placeholder="Enter your email" className="w-full px-3 py-3 rounded-xl bg-[#0A0E1A] border border-white/10 text-white outline-none focus:border-[#00D4FF]" />
        {err && <p className="text-red-400 text-xs">{err}</p>}
        {msg && <p className="text-[#00D4FF] text-xs">{msg}</p>}
        <button className="w-full py-3 rounded-xl bg-[#00D4FF] text-white font-bold">SEND RESET LINK</button>
        <Link to="/login" className="text-[#00D4FF] text-sm block">Back to Login</Link>
      </form>
    </div>
  )
}
