import api from './client'
import type { Asset, AssetRequest, AssetStatus, Page } from '../types'

export const getAssets = (params: Record<string, unknown>) =>
  api.get<Page<Asset>>('/assets', { params }).then((r) => r.data)

export const getAsset = (id: number) =>
  api.get<Asset>(`/assets/${id}`).then((r) => r.data)

export const createAsset = (req: AssetRequest) =>
  api.post<Asset>('/assets', req).then((r) => r.data)

export const updateAsset = (id: number, req: AssetRequest) =>
  api.put<Asset>(`/assets/${id}`, req).then((r) => r.data)

export const updateAssetStatus = (id: number, status: AssetStatus) =>
  api.patch<Asset>(`/assets/${id}/status`, { status }).then((r) => r.data)

export const assignAsset = (id: number, retailerCode: string) =>
  api.patch<Asset>(`/assets/${id}/assign`, { retailerCode }).then((r) => r.data)

export const unassignAsset = (id: number) =>
  api.patch<Asset>(`/assets/${id}/unassign`).then((r) => r.data)

export const getAssetHistory = (id: number) =>
  api.get<unknown[]>(`/assets/${id}/history`).then((r) => r.data)
