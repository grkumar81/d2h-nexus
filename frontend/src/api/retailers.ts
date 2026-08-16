import api from './client'
import type { Page, Retailer, RetailerRequest, UploadResult } from '../types'

interface R<T> { data: T; success: boolean }

export const getRetailers = (params: Record<string, unknown>) =>
  api.get<R<Page<Retailer>>>('/retailers', { params }).then(r => r.data.data)

export const createRetailer = (req: RetailerRequest) =>
  api.post<R<Retailer>>('/retailers', req).then(r => r.data.data)

export const updateRetailer = (id: number, req: RetailerRequest) =>
  api.put<R<Retailer>>(`/retailers/${id}`, req).then(r => r.data.data)

export const activateRetailer = (id: number) =>
  api.patch<R<Retailer>>(`/retailers/${id}/activate`).then(r => r.data.data)

export const deactivateRetailer = (id: number) =>
  api.patch<R<Retailer>>(`/retailers/${id}/deactivate`).then(r => r.data.data)

export const uploadRetailers = (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return api.post<R<UploadResult>>('/retailers/upload', form).then(r => r.data.data)
}
