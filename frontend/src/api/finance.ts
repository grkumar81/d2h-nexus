import api from './client'
import type {
  AdjustRequest, FinanceRequest, FinanceSummary,
  FinancialTransaction, Page, RetailerFinanceSummary, UploadResult,
} from '../types'

interface R<T> { data: T; success: boolean }

export const getTransactions = (params: Record<string, unknown>) =>
  api.get<R<Page<FinancialTransaction>>>('/finance/transactions', { params }).then(r => r.data.data)

export const createTransaction = (req: FinanceRequest) =>
  api.post<R<FinancialTransaction>>('/finance/transactions', req).then(r => r.data.data)

export const reverseTransaction = (id: number, reason?: string) =>
  api.post<R<FinancialTransaction>>(`/finance/transactions/${id}/reverse`, null, {
    params: reason ? { reason } : {},
  }).then(r => r.data.data)

export const adjustTransaction = (id: number, req: AdjustRequest) =>
  api.post<R<FinancialTransaction>>(`/finance/transactions/${id}/adjust`, req).then(r => r.data.data)

export const getRetailerSummaries = () =>
  api.get<R<RetailerFinanceSummary[]>>('/finance/summary/retailers').then(r => r.data.data)

export const getTenantSummary = () =>
  api.get<R<FinanceSummary>>('/finance/summary').then(r => r.data.data)

export const uploadFinance = (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return api.post<R<UploadResult>>('/finance/upload', form).then(r => r.data.data)
}
