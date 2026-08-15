import api from './client'
import type { BoxSale, BoxSaleRequest, Page } from '../types'

export const getBoxSales = (params: Record<string, unknown>) =>
  api.get<Page<BoxSale>>('/box-sales', { params }).then((r) => r.data)

export const getBoxSale = (id: number) =>
  api.get<BoxSale>(`/box-sales/${id}`).then((r) => r.data)

export const createBoxSale = (req: BoxSaleRequest) =>
  api.post<BoxSale>('/box-sales', req).then((r) => r.data)

export const cancelBoxSale = (id: number) =>
  api.patch(`/box-sales/${id}/cancel`)
