import client from './client'
import type { TenantProfile, UpdateTenantProfileRequest, SubscriptionStatusDto } from '../types'

export const getTenantProfile = () =>
  client.get<{ data: TenantProfile }>('/tenant/profile').then(r => r.data.data)

export const updateTenantProfile = (req: UpdateTenantProfileRequest) =>
  client.put<{ data: TenantProfile }>('/tenant/profile', req).then(r => r.data.data)

export const getSubscriptionStatus = () =>
  client.get<{ data: SubscriptionStatusDto }>('/tenant/profile/subscription').then(r => r.data.data)
