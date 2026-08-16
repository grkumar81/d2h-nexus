import { useEffect, useRef, useState } from 'react'
import { AgGridReact } from 'ag-grid-react'
import type { ColDef } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import {
  getRecharges, createRecharge, reverseRecharge,
  cancelRecharge, uploadRecharge, getRechargeSummary,
} from '../../api/recharges'
import type {
  CreateRechargeRequest, RechargeTransaction,
  RechargeType, RechargeSummary, UploadResult,
} from '../../types'
import UploadResultPanel from '../../components/UploadResultPanel'
import styles from './Recharge.module.css'

ModuleRegistry.registerModules([AllCommunityModule])

const TYPES: RechargeType[] = ['REGULAR', 'MONTHLY', 'QUARTERLY', 'ANNUAL', 'PROMOTIONAL', 'MANUAL', 'OTHER']

const COLS: ColDef<RechargeTransaction>[] = [
  { field: 'rechargeDate', headerName: 'Date', width: 120 },
  { field: 'retailerCode', headerName: 'Retailer', width: 120 },
  { field: 'rechargeType', headerName: 'Type', width: 120 },
  { field: 'amount', headerName: 'Amount', width: 120, valueFormatter: (p) => `₹${p.value?.toLocaleString()}` },
  { field: 'rechargeStatus', headerName: 'Status', width: 110 },
  { field: 'reference', headerName: 'Reference', flex: 1 },
  { field: 'paymentMethod', headerName: 'Method', width: 120 },
]

const EMPTY_FORM: CreateRechargeRequest = {
  retailerId: 0, rechargeDate: '', amount: 0, rechargeType: 'REGULAR',
}

export default function RechargePage() {
  const gridRef = useRef<AgGridReact<RechargeTransaction>>(null)
  const [rowData, setRowData] = useState<RechargeTransaction[]>([])
  const [selected, setSelected] = useState<RechargeTransaction | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState<CreateRechargeRequest>(EMPTY_FORM)
  const [uploadResult, setUploadResult] = useState<UploadResult | null>(null)
  const [uploading, setUploading] = useState(false)
  const [summary, setSummary] = useState<RechargeSummary | null>(null)
  const [error, setError] = useState('')

  const fetchData = () => {
    getRecharges({ page: 0, size: 200 }).then(d => setRowData(d.content)).catch(() => {})
    getRechargeSummary().then(setSummary).catch(() => {})
  }

  useEffect(() => { fetchData() }, [])

  const handleCreate = async () => {
    if (!form.retailerId || !form.rechargeDate || form.amount <= 0) {
      setError('Retailer ID, date and a positive amount are required.'); return
    }
    try { await createRecharge(form); setForm(EMPTY_FORM); setShowForm(false); setError(''); fetchData() }
    catch { setError('Failed to create recharge.') }
  }

  const handleReverse = async () => {
    if (!selected) return
    const reason = window.prompt('Reason for reversal (optional):') ?? undefined
    try { await reverseRecharge(selected.id, reason); setSelected(null); fetchData() }
    catch { setError('Reversal failed.') }
  }

  const handleCancel = async () => {
    if (!selected) return
    const reason = window.prompt('Reason for cancellation (optional):') ?? undefined
    try { await cancelRecharge(selected.id, reason); setSelected(null); fetchData() }
    catch { setError('Cancellation failed.') }
  }

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    try { const r = await uploadRecharge(file); setUploadResult(r); fetchData() }
    catch { setError('Upload failed.') }
    finally { setUploading(false); e.target.value = '' }
  }

  const canReverse = selected?.rechargeStatus === 'SUCCESS' && !selected.reversedById
  const canCancel = selected?.rechargeStatus === 'PENDING' || selected?.rechargeStatus === 'FAILED'

  return (
    <div className={styles.page}>
      {summary && (
        <div className={styles.kpis}>
          <div className={styles.kpi}><h4>Total</h4><p>₹{summary.totalAmount?.toLocaleString()}</p></div>
          <div className={styles.kpi}><h4>Successful</h4><p>₹{summary.successAmount?.toLocaleString()}</p></div>
          <div className={styles.kpi}><h4>Failed</h4><p>₹{summary.failedAmount?.toLocaleString()}</p></div>
          <div className={styles.kpi}><h4>Reversed</h4><p>₹{summary.reversedAmount?.toLocaleString()}</p></div>
          <div className={styles.kpi}><h4>Count</h4><p>{summary.totalCount}</p></div>
        </div>
      )}
      <div className={styles.toolbar}>
        <h2>Recharge Transactions</h2>
        <div className={styles.actions}>
          {canReverse && <button onClick={handleReverse}>Reverse</button>}
          {canCancel && <button onClick={handleCancel}>Cancel</button>}
          <button onClick={() => { setShowForm(!showForm); setError('') }}>+ New</button>
          <label className={styles.uploadBtn}>
            {uploading ? 'Uploading…' : 'Upload CSV/Excel'}
            <input type="file" accept=".csv,.xlsx,.xls" onChange={handleUpload} hidden />
          </label>
        </div>
      </div>

      {error && <p className={styles.error}>{error}</p>}

      {showForm && (
        <div className={styles.form}>
          <input type="number" placeholder="Retailer ID" min={1} value={form.retailerId || ''}
            onChange={(e) => setForm({ ...form, retailerId: +e.target.value })} />
          <input type="date" value={form.rechargeDate}
            onChange={(e) => setForm({ ...form, rechargeDate: e.target.value })} />
          <input type="number" placeholder="Amount" min={0.01} step="0.01" value={form.amount || ''}
            onChange={(e) => setForm({ ...form, amount: +e.target.value })} />
          <select value={form.rechargeType}
            onChange={(e) => setForm({ ...form, rechargeType: e.target.value as RechargeType })}>
            {TYPES.map((t) => <option key={t}>{t}</option>)}
          </select>
          <input placeholder="Reference (optional)" value={form.reference ?? ''}
            onChange={(e) => setForm({ ...form, reference: e.target.value })} />
          <button onClick={handleCreate}>Save</button>
          <button onClick={() => setShowForm(false)}>Cancel</button>
        </div>
      )}

      {uploadResult && <UploadResultPanel result={uploadResult} />}

      <div className="ag-theme-alpine" style={{ flex: 1, marginTop: 12 }}>
        <AgGridReact<RechargeTransaction>
          ref={gridRef}
          columnDefs={COLS}
          rowData={rowData}
          rowSelection="single"
          onRowClicked={(e) => { setSelected(e.data ?? null); setError('') }}
          pagination
          paginationPageSize={20}
          domLayout="autoHeight"
        />
      </div>
    </div>
  )
}
