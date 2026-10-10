import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function ProtectedRoute() {
  const { authenticated, loading } = useAuth()
  const location = useLocation()
  if (loading) return <div className="grid min-h-screen place-items-center text-sm text-slate-500">Restoring your secure session…</div>
  return authenticated ? <Outlet /> : <Navigate to="/login" state={{ from: location }} replace />
}
