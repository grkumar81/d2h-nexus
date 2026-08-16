import api from './client'
import type { BoxSale, BoxSaleRequest, Page } from '../types'

interface R<T> { data: T; success: boolean }

export const getBoxSales = (params: Record<string, unknown>) =>
  api.get<R<Page<BoxSale>>>('/box-sales', { params }).then(r => r.data.data)

export const createBoxSale = (req: BoxSaleRequest) =>
  api.post<R<BoxSale>>('/box-sales', req).then(r => r.data.data)

export const getBoxSale = (id: number) =>
  api.get<R<BoxSale>>(`/box-sales/${id}`).then(r => r.data.data)
