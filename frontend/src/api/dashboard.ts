import api from './client'
import type { Dashboard } from '../types'

export const getDashboard = (fyYear?: number) =>
  api.get<Dashboard>('/dashboard', { params: fyYear ? { fyYear } : {} }).then((r) => r.data)
