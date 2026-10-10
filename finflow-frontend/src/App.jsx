import { Navigate, Route, Routes } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute'
import AppShell from './layouts/AppShell'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import Accounts from './pages/Accounts'
import Transactions from './pages/Transactions'
import Transfers from './pages/Transfers'
import Profile from './pages/Profile'

export default function App() { return <Routes><Route path="/login" element={<Login />} /><Route path="/register" element={<Register />} /><Route element={<ProtectedRoute />}><Route element={<AppShell />}><Route path="/dashboard" element={<Dashboard />} /><Route path="/accounts" element={<Accounts />} /><Route path="/transactions" element={<Transactions />} /><Route path="/transfers" element={<Transfers />} /><Route path="/profile" element={<Profile />} /></Route></Route><Route path="*" element={<Navigate to="/dashboard" replace />} /></Routes> }
