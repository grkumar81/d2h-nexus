import api from './client'
import type {
  AdjustRequest, FinanceRequest, FinanceSummary,
  FinancialTransaction, Page, RetailerFinanceSummary, UploadResult,
} from '../types'

export const getTransactions = (params: Record<string, unknown>) =>
  api.get<Page<FinancialTransaction>>('/finance', { params }).then((r) => r.data)

export const getTransaction = (id: number) =>
  api.get<FinancialTransaction>(`/finance/${id}`).then((r) => r.data)

export const createTransaction = (req: FinanceRequest) =>
  api.post<FinancialTransaction>('/finance', req).then((r) => r.data)

export const reverseTransaction = (id: number) =>
  api.post<FinancialTransaction>(`/finance/${id}/reverse`).then((r) => r.data)

export const adjustTransaction = (id: number, req: AdjustRequest) =>
  api.post<FinancialTransaction>(`/finance/${id}/adjust`, req).then((r) => r.data)

export const getRetailerSummaries = () =>
  api.get<RetailerFinanceSummary[]>('/finance/summary/retailers').then((r) => r.data)

export const getTenantSummary = () =>
  api.get<FinanceSummary>('/finance/summary/tenant').then((r) => r.data)

export const uploadFinance = (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return api.post<UploadResult>('/finance/upload', form).then((r) => r.data)
}
