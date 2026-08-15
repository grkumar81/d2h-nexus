import client from './client'
import type { Page, UserDto, CreateUserRequest, AuditLog } from '../types'

export const listUsers = (page = 0, size = 20) =>
  client.get<{ data: Page<UserDto> }>('/api/v1/users', { params: { page, size } })
    .then(r => r.data.data)

export const createUser = (req: CreateUserRequest) =>
  client.post<{ data: UserDto }>('/api/v1/users', req).then(r => r.data.data)

export const activateUser = (id: number) =>
  client.post<{ data: UserDto }>(`/api/v1/users/${id}/activate`).then(r => r.data.data)

export const deactivateUser = (id: number) =>
  client.post<{ data: UserDto }>(`/api/v1/users/${id}/deactivate`).then(r => r.data.data)

export const changePassword = (currentPassword: string, newPassword: string) =>
  client.post('/api/v1/users/me/change-password', { currentPassword, newPassword })

export const listAuditLogs = (params: {
  entityType?: string; entityId?: string; action?: string;
  performedBy?: string; from?: string; to?: string; page?: number; size?: number
}) =>
  client.get<{ data: Page<AuditLog> }>('/api/v1/audit', { params })
    .then(r => r.data.data)
