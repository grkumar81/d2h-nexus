import { createContext, useContext, useState, type ReactNode } from 'react'
import { login as apiLogin } from '../api/auth'
import type { LoginRequest } from '../types'

interface AuthState { token: string; username: string; tenantCode: string; roles: string[] }

interface AuthContextValue {
  auth: AuthState | null
  login: (req: LoginRequest) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function loadAuth(): AuthState | null {
  try {
    const token = localStorage.getItem('token')
    const username = localStorage.getItem('username')
    const tenantCode = localStorage.getItem('tenantCode') // may be 'null' string for PLATFORM_ADMIN
    const role = localStorage.getItem('role')
    if (token && username && tenantCode !== null && role)
      return { token, username, tenantCode: tenantCode === 'null' ? null! : tenantCode, roles: JSON.parse(role) }
  } catch { /* ignore */ }
  return null
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState | null>(loadAuth)

  const login = async (req: LoginRequest) => {
    const res = await apiLogin(req)
    localStorage.setItem('token', res.token)
    localStorage.setItem('username', res.username)
    localStorage.setItem('tenantCode', res.tenantCode ?? 'null')
    localStorage.setItem('role', JSON.stringify(res.roles))
    setAuth({ token: res.token, username: res.username, tenantCode: res.tenantCode, roles: res.roles })
  }

  const logout = () => {
    localStorage.clear()
    setAuth(null)
  }

  return <AuthContext.Provider value={{ auth, login, logout }}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
