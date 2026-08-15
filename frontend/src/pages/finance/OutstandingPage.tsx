import { useEffect, useState } from 'react'
import { AgGridReact } from 'ag-grid-react'
import type { ColDef } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import { getRetailerSummaries, getTenantSummary } from '../../api/finance'
import type { FinanceSummary, RetailerFinanceSummary } from '../../types'
import styles from './Outstanding.module.css'

ModuleRegistry.registerModules([AllCommunityModule])

const fmt = (n: number) => `₹${n.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`

const COLS: ColDef<RetailerFinanceSummary>[] = [
  { field: 'retailerCode', headerName: 'Code', width: 120 },
  { field: 'retailerName', headerName: 'Name', flex: 1 },
  { field: 'totalBoxSales', headerName: 'Box Sales', width: 120, valueFormatter: (p) => fmt(p.value) },
  { field: 'totalDue', headerName: 'Total Due', width: 120, valueFormatter: (p) => fmt(p.value) },
  { field: 'totalReceived', headerName: 'Received', width: 120, valueFormatter: (p) => fmt(p.value) },
  { field: 'outstanding', headerName: 'Outstanding', width: 130, valueFormatter: (p) => fmt(p.value),
    cellStyle: (p) => ({ color: p.value > 0 ? '#c62828' : '#2e7d32', fontWeight: 600 }) },
  { field: 'totalRecharge', headerName: 'Recharge', width: 120, valueFormatter: (p) => fmt(p.value) },
]

export default function OutstandingPage() {
  const [summary, setSummary] = useState<FinanceSummary | null>(null)
  const [rows, setRows] = useState<RetailerFinanceSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([getTenantSummary(), getRetailerSummaries()])
      .then(([s, r]) => { setSummary(s); setRows(r) })
      .catch(() => setError('Failed to load summary.'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p>Loading…</p>
  if (error) return <p className={styles.error}>{error}</p>

  return (
    <div className={styles.page}>
      <h2>Outstanding Summary</h2>
      {summary && (
        <div className={styles.cards}>
          <div className={styles.card}><span>Total Due</span><strong>{fmt(summary.totalDue)}</strong></div>
          <div className={styles.card}><span>Received</span><strong>{fmt(summary.totalReceived)}</strong></div>
          <div className={`${styles.card} ${styles.highlight}`}><span>Outstanding</span><strong>{fmt(summary.outstanding)}</strong></div>
          <div className={styles.card}><span>Recharge</span><strong>{fmt(summary.totalRecharge)}</strong></div>
          <div className={styles.card}><span>Transactions</span><strong>{summary.transactionCount}</strong></div>
        </div>
      )}
      <div className="ag-theme-alpine" style={{ marginTop: 20 }}>
        <AgGridReact<RetailerFinanceSummary>
          columnDefs={COLS}
          rowData={rows}
          domLayout="autoHeight"
        />
      </div>
    </div>
  )
}
