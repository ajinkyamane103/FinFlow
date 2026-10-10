import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api, TOKEN_KEY } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => sessionStorage.getItem(TOKEN_KEY))
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(Boolean(sessionStorage.getItem(TOKEN_KEY)))

  const logout = useCallback(() => {
    sessionStorage.removeItem(TOKEN_KEY)
    setToken(null)
    setUser(null)
    setLoading(false)
  }, [])

  const refreshProfile = useCallback(async () => {
    const { data } = await api.me()
    setUser(data)
    return data
  }, [])

  useEffect(() => {
    if (!token) return
    refreshProfile().catch(logout).finally(() => setLoading(false))
  }, [token, refreshProfile, logout])

  useEffect(() => {
    const onUnauthorized = () => logout()
    window.addEventListener('finflow:unauthorized', onUnauthorized)
    return () => window.removeEventListener('finflow:unauthorized', onUnauthorized)
  }, [logout])

  const login = async (credentials) => {
    const { data } = await api.login(credentials)
    sessionStorage.setItem(TOKEN_KEY, data.accessToken)
    setToken(data.accessToken)
    const profile = await refreshProfile()
    return profile
  }

  return <AuthContext.Provider value={{ user, token, loading, login, logout, refreshProfile, authenticated: Boolean(token) }}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
