import api from './client'
import type { RetailerReport, PeriodReport } from '../types'

export const getAllRetailerReport = (params: { dateFrom?: string; dateTo?: string } = {}) =>
  api.get<RetailerReport[]>('/reports/retailers', { params }).then((r) => r.data)

export const getRetailerReport = (retailerId: number, params: { dateFrom?: string; dateTo?: string } = {}) =>
  api.get<RetailerReport>(`/reports/retailers/${retailerId}`, { params }).then((r) => r.data)

export const getPeriodReport = (params: { dateFrom?: string; dateTo?: string } = {}) =>
  api.get<PeriodReport>('/reports/period', { params }).then((r) => r.data)

export const exportRetailersCsv = (params: { dateFrom?: string; dateTo?: string } = {}) =>
  api.get('/reports/retailers/export/csv', { params, responseType: 'blob' }).then((r) => r.data as Blob)

export const exportRetailersExcel = (params: { dateFrom?: string; dateTo?: string } = {}) =>
  api.get('/reports/retailers/export/excel', { params, responseType: 'blob' }).then((r) => r.data as Blob)

export const exportPeriodCsv = (params: { dateFrom?: string; dateTo?: string } = {}) =>
  api.get('/reports/period/export/csv', { params, responseType: 'blob' }).then((r) => r.data as Blob)

export const exportPeriodExcel = (params: { dateFrom?: string; dateTo?: string } = {}) =>
  api.get('/reports/period/export/excel', { params, responseType: 'blob' }).then((r) => r.data as Blob)
