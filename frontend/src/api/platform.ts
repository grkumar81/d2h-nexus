import client from './client'
import type { Page, PlatformTenantDto } from '../types'

export const listTenants = (page = 0, size = 20) =>
  client.get<{ data: Page<PlatformTenantDto> }>('/platform/tenants', { params: { page, size } })
    .then(r => r.data.data)

export const renewTenant = (id: number, subscriptionExpiry: string, gracePeriodDays?: number) =>
  client.post<{ data: PlatformTenantDto }>(`/platform/tenants/${id}/renew`, { subscriptionExpiry, gracePeriodDays })
    .then(r => r.data.data)

export const suspendTenant = (id: number) =>
  client.post<{ data: PlatformTenantDto }>(`/platform/tenants/${id}/suspend`).then(r => r.data.data)

export const approveTenant = (id: number) =>
  client.post<{ data: PlatformTenantDto }>(`/platform/tenants/${id}/approve`).then(r => r.data.data)
