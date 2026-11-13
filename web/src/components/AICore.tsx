import { motion } from 'framer-motion'

export function AICore({ listening, thinking, speaking }: { listening?: boolean; thinking?: boolean; speaking?: boolean }) {
  const color = listening ? '#00FF88' : thinking ? '#FFD600' : speaking ? '#00D4FF' : '#00D4FF'
  return (
    <div className="relative w-40 h-40 flex items-center justify-center">
      <div className="absolute inset-0 rounded-full blur-xl opacity-30" style={{ background: `radial-gradient(circle, ${color} 0%, transparent 70%)` }} />
      <motion.div
        className="absolute inset-2 rounded-full border"
        style={{ borderColor: color + '30' }}
        animate={{ rotate: 360 }}
        transition={{ duration: 8, repeat: Infinity, ease: 'linear' }}
      >
        <div className="absolute w-2 h-2 rounded-full -top-1 left-1/2 -translate-x-1/2" style={{ background: color }} />
        <div className="absolute w-2 h-2 rounded-full -bottom-1 left-1/2 -translate-x-1/2" style={{ background: color }} />
        <div className="absolute w-1.5 h-1.5 rounded-full top-1/2 -left-1 -translate-y-1/2" style={{ background: color }} />
        <div className="absolute w-1.5 h-1.5 rounded-full top-1/2 -right-1 -translate-y-1/2" style={{ background: color }} />
      </motion.div>
      <div className="absolute inset-4 rounded-full border opacity-20" style={{ borderColor: color }} />
      <motion.div
        animate={{ scale: listening || speaking ? [1, 1.08, 1] : 1 }}
        transition={{ duration: 0.9, repeat: listening || speaking ? Infinity : 0 }}
        className="w-20 h-20 rounded-full flex items-center justify-center relative overflow-hidden"
        style={{ background: `radial-gradient(circle at 30% 30%, ${color}, #003366)` }}
      >
        {speaking || listening ? (
          <div className="flex gap-1 items-center">
            {[6, 14, 10, 16, 8].map((h, i) => (
              <motion.div key={i} className="w-1 bg-white rounded-full" animate={{ height: [h, h+8, h] }} transition={{ duration: 0.35 + i*0.06, repeat: Infinity, repeatType: 'reverse' }} style={{ height: h }} />
            ))}
          </div>
        ) : (
          <span className="text-white font-bold text-2xl">◉</span>
        )}
      </motion.div>
    </div>
  )
}

export function StatusChips({ listening, thinking, speaking }: any) {
  const Chip = ({ label, active, color }: { label:string; active?: boolean; color: string }) => (
    <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full text-[10px] font-medium" style={{ background: active ? color+'22' : 'rgba(255,255,255,0.06)', color: active ? color : 'rgba(255,255,255,0.5)' }}>
      <div className="w-1.5 h-1.5 rounded-full" style={{ background: active ? color : 'rgba(255,255,255,0.3)' }} />
      {label}
    </div>
  )
  return <div className="flex gap-2"><Chip label="Listening" active={listening} color="#00FF88"/><Chip label="Thinking" active={thinking} color="#FFD600"/><Chip label="Speaking" active={speaking} color="#00D4FF"/></div>
}
