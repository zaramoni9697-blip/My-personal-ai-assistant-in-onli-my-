import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Splash } from './pages/Splash'
import { Login, Signup, Forgot } from './pages/Auth'
import { Providers } from './pages/Providers'
import { Dashboard } from './pages/Dashboard'
import { Chat } from './pages/Chat'
import { Voice } from './pages/Voice'
import { Device } from './pages/Device'
import { SettingsPage } from './pages/Settings'
import { useStore } from './store/useStore'

function Protected({ children }: { children: React.ReactNode }) {
  const user = useStore(s=>s.user)
  if(!user) return <Navigate to="/login" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Splash/>} />
        <Route path="/login" element={<Login/>} />
        <Route path="/signup" element={<Signup/>} />
        <Route path="/forgot" element={<Forgot/>} />
        <Route path="/providers" element={<Protected><Providers/></Protected>} />
        <Route path="/dashboard" element={<Protected><Dashboard/></Protected>} />
        <Route path="/chat" element={<Protected><Chat/></Protected>} />
        <Route path="/voice" element={<Protected><Voice/></Protected>} />
        <Route path="/device" element={<Protected><Device/></Protected>} />
        <Route path="/settings" element={<Protected><SettingsPage/></Protected>} />
      </Routes>
    </BrowserRouter>
  )
}
