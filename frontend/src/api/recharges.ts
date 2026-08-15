import api from './client'
import type {
  CreateRechargeRequest, Page, RechargeTransaction,
  RechargeSummary, RetailerRechargeSummary, UploadResult,
} from '../types'

interface ApiResponse<T> { success: boolean; data: T }

export const getRecharges = (params: Record<string, unknown>) =>
  api.get<ApiResponse<Page<RechargeTransaction>>>('/recharges', { params })
    .then((r) => r.data.data)

export const getRecharge = (id: number) =>
  api.get<ApiResponse<RechargeTransaction>>(`/recharges/${id}`)
    .then((r) => r.data.data)

export const createRecharge = (req: CreateRechargeRequest) =>
  api.post<ApiResponse<RechargeTransaction>>('/recharges', req)
    .then((r) => r.data.data)

export const reverseRecharge = (id: number, reason?: string) =>
  api.post<ApiResponse<RechargeTransaction>>(`/recharges/${id}/reverse`, null, {
    params: reason ? { reason } : {},
  }).then((r) => r.data.data)

export const cancelRecharge = (id: number, reason?: string) =>
  api.post<ApiResponse<RechargeTransaction>>(`/recharges/${id}/cancel`, null, {
    params: reason ? { reason } : {},
  }).then((r) => r.data.data)

export const getRechargeSummary = (params?: Record<string, unknown>) =>
  api.get<ApiResponse<RechargeSummary>>('/recharges/summary', { params })
    .then((r) => r.data.data)

export const getRetailerRechargeSummary = (retailerId: number) =>
  api.get<ApiResponse<RetailerRechargeSummary>>(`/recharges/retailers/${retailerId}/summary`)
    .then((r) => r.data.data)

export const uploadRecharge = (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return api.post<ApiResponse<UploadResult>>('/recharges/upload', form)
    .then((r) => r.data.data)
}
