import { useEffect, useState } from 'react'
import { AgGridReact } from 'ag-grid-react'
import type { ColDef } from 'ag-grid-community'
import {
  getAllRetailerReport, getPeriodReport,
  exportRetailersCsv, exportRetailersExcel,
  exportPeriodCsv, exportPeriodExcel,
} from '../../api/reports'
import type { RetailerReport, PeriodReport } from '../../types'
import styles from './Reports.module.css'

const fmt = (n: number) =>
  n >= 10_000_000 ? `₹${(n / 10_000_000).toFixed(2)}Cr`
  : n >= 100_000 ? `₹${(n / 100_000).toFixed(2)}L`
  : `₹${n.toLocaleString('en-IN')}`

const amountCol = (field: string, header: string): ColDef<RetailerReport> => ({
  field: field as keyof RetailerReport,
  headerName: header,
  valueFormatter: (p) => fmt(p.value ?? 0),
  type: 'numericColumn',
  width: 140,
})

const RETAILER_COLS: ColDef<RetailerReport>[] = [
  { field: 'retailerCode', headerName: 'Code', width: 120, pinned: 'left' },
  { field: 'retailerName', headerName: 'Name', flex: 1, minWidth: 160 },
  amountCol('boxSales', 'Box Sales'),
  amountCol('received', 'Received'),
  amountCol('outstanding', 'Outstanding'),
  amountCol('recharge', 'Recharge'),
]

type Tab = 'retailer' | 'period'

export default function ReportsPage() {
  const [tab, setTab] = useState<Tab>('retailer')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo] = useState('')

  const [retailerRows, setRetailerRows] = useState<RetailerReport[]>([])
  const [periodData, setPeriodData] = useState<PeriodReport | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const params = { dateFrom: dateFrom || undefined, dateTo: dateTo || undefined }

  const load = () => {
    setLoading(true)
    setError(null)
    const promise = tab === 'retailer'
      ? getAllRetailerReport(params).then((d) => setRetailerRows(d))
      : getPeriodReport(params).then((d) => setPeriodData(d))
    promise.catch(() => setError('Failed to load report')).finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [tab])

  const download = (blob: Blob, filename: string) => {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = filename; a.click()
    URL.revokeObjectURL(url)
  }

  const handleExport = async (format: 'csv' | 'excel') => {
    try {
      if (tab === 'retailer') {
        const blob = format === 'csv'
          ? await exportRetailersCsv(params)
          : await exportRetailersExcel(params)
        download(blob, `retailer-report.${format === 'csv' ? 'csv' : 'xlsx'}`)
      } else {
        const blob = format === 'csv'
          ? await exportPeriodCsv(params)
          : await exportPeriodExcel(params)
        download(blob, `period-report.${format === 'csv' ? 'csv' : 'xlsx'}`)
      }
    } catch {
      setError('Export failed')
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <h2>Reports</h2>
        <div className={styles.tabs}>
          <button className={tab === 'retailer' ? styles.activeTab : styles.tab} onClick={() => setTab('retailer')}>
            Retailer Summary
          </button>
          <button className={tab === 'period' ? styles.activeTab : styles.tab} onClick={() => setTab('period')}>
            Period Summary
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className={styles.filters}>
        <label>From:
          <input type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} />
        </label>
        <label>To:
          <input type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} />
        </label>
        <button className={styles.btnPrimary} onClick={load} disabled={loading}>
          {loading ? 'Loading…' : 'Run Report'}
        </button>
        <button className={styles.btnSecondary} onClick={() => handleExport('csv')}>Export CSV</button>
        <button className={styles.btnSecondary} onClick={() => handleExport('excel')}>Export Excel</button>
      </div>

      {error && <div className={styles.error}>{error}</div>}

      {/* Retailer Summary Tab */}
      {tab === 'retailer' && (
        <div className={`ag-theme-alpine ${styles.grid}`}>
          <AgGridReact<RetailerReport>
            rowData={retailerRows}
            columnDefs={RETAILER_COLS}
            defaultColDef={{ sortable: true, resizable: true, filter: true }}
            pagination
            paginationPageSize={20}
            domLayout="autoHeight"
          />
        </div>
      )}

      {/* Period Summary Tab */}
      {tab === 'period' && periodData && (
        <div className={styles.periodCard}>
          <div className={styles.periodGrid}>
            <KpiCard label="Box Sales" value={fmt(periodData.boxSales)} />
            <KpiCard label="Received" value={fmt(periodData.received)} />
            <KpiCard label="Outstanding" value={fmt(periodData.outstanding)} />
            <KpiCard label="Recharge" value={fmt(periodData.recharge)} />
            <KpiCard label="Transactions" value={periodData.transactionCount.toString()} />
          </div>
          <div className={styles.periodMeta}>
            {periodData.dateFrom && <span>From: {periodData.dateFrom}</span>}
            {periodData.dateTo && <span>To: {periodData.dateTo}</span>}
          </div>
        </div>
      )}

      {tab === 'period' && !periodData && !loading && (
        <div className={styles.empty}>Run the report to see period summary</div>
      )}
    </div>
  )
}

function KpiCard({ label, value }: { label: string; value: string }) {
  return (
    <div className={styles.kpiCard}>
      <div className={styles.kpiValue}>{value}</div>
      <div className={styles.kpiLabel}>{label}</div>
    </div>
  )
}
