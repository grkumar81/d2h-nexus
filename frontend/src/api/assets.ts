import api from './client'
import type { Asset, AssetRequest, AssetStatus, Page } from '../types'

interface R<T> { data: T; success: boolean }

export const getAssets = (params: Record<string, unknown>) =>
  api.get<R<Page<Asset>>>('/assets', { params }).then(r => r.data.data)

export const createAsset = (req: AssetRequest) =>
  api.post<R<Asset>>('/assets', req).then(r => r.data.data)

export const updateAsset = (id: number, req: AssetRequest) =>
  api.put<R<Asset>>(`/assets/${id}`, req).then(r => r.data.data)

export const updateAssetStatus = (id: number, status: AssetStatus) =>
  api.patch<R<Asset>>(`/assets/${id}/status`, { status }).then(r => r.data.data)

export const assignAsset = (id: number, retailerCode: string) =>
  api.patch<R<Asset>>(`/assets/${id}/assign`, { retailerCode }).then(r => r.data.data)

export const unassignAsset = (id: number) =>
  api.patch<R<Asset>>(`/assets/${id}/unassign`).then(r => r.data.data)

export const getAssetHistory = (id: number) =>
  api.get<R<unknown[]>>(`/assets/${id}/history`).then(r => r.data.data)
