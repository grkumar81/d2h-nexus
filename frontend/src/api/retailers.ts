import api from './client'
import type { Page, Retailer, RetailerRequest, UploadResult } from '../types'

export const getRetailers = (params: Record<string, unknown>) =>
  api.get<Page<Retailer>>('/retailers', { params }).then((r) => r.data)

export const getRetailer = (id: number) =>
  api.get<Retailer>(`/retailers/${id}`).then((r) => r.data)

export const createRetailer = (req: RetailerRequest) =>
  api.post<Retailer>('/retailers', req).then((r) => r.data)

export const updateRetailer = (id: number, req: RetailerRequest) =>
  api.put<Retailer>(`/retailers/${id}`, req).then((r) => r.data)

export const deactivateRetailer = (id: number) =>
  api.delete(`/retailers/${id}`)

export const uploadRetailers = (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return api.post<UploadResult>('/retailers/upload', form).then((r) => r.data)
}
