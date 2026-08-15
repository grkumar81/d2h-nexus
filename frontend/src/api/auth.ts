import api from './client'
import type { LoginRequest, LoginResponse } from '../types'

interface ApiResponse<T> { success: boolean; data: T }

export const login = (req: LoginRequest) =>
  api.post<ApiResponse<LoginResponse>>('/auth/login', req).then((r) => r.data.data)
