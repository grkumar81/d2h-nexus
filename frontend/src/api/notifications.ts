import client from './client'
import type {
  NotificationConfig,
  NotificationDelivery,
  SaveNotificationConfigRequest,
  Page,
} from '../types'

const BASE = '/api/v1/notifications'

export const listConfigs = () =>
  client.get<{ data: NotificationConfig[] }>(`${BASE}/config`).then(r => r.data.data)

export const saveConfig = (req: SaveNotificationConfigRequest) =>
  client.post<{ data: NotificationConfig }>(`${BASE}/config`, req).then(r => r.data.data)

export const deleteConfig = (id: number) =>
  client.delete(`${BASE}/config/${id}`)

export const listDeliveries = (page = 0, size = 20) =>
  client
    .get<{ data: Page<NotificationDelivery> }>(`${BASE}/history`, { params: { page, size } })
    .then(r => r.data.data)
