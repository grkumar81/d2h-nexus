import client from './client'
import type { UserDto, CreateUserRequest, UpdateUserRequest, Page } from '../types'

export const listUsers = (page = 0, size = 20) =>
  client.get<{ data: Page<UserDto> }>(`/users?page=${page}&size=${size}`).then(r => r.data.data)

export const getUser = (id: number) =>
  client.get<{ data: UserDto }>(`/users/${id}`).then(r => r.data.data)

export const createUser = (req: CreateUserRequest) =>
  client.post<{ data: UserDto }>('/users', req).then(r => r.data.data)

export const updateUser = (id: number, req: UpdateUserRequest) =>
  client.put<{ data: UserDto }>(`/users/${id}`, req).then(r => r.data.data)

export const activateUser = (id: number) =>
  client.post<{ data: UserDto }>(`/users/${id}/activate`).then(r => r.data.data)

export const deactivateUser = (id: number) =>
  client.post<{ data: UserDto }>(`/users/${id}/deactivate`).then(r => r.data.data)

export const resetPassword = (id: number) =>
  client.post(`/users/${id}/reset-password`)

export const changePassword = (currentPassword: string, newPassword: string) =>
  client.post('/users/me/change-password', { currentPassword, newPassword })
