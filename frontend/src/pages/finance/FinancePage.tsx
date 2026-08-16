import { useEffect, useRef, useState } from 'react'
import { AgGridReact } from 'ag-grid-react'
import type { ColDef } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import {
  getTransactions, createTransaction, reverseTransaction,
  adjustTransaction, uploadFinance,
} from '../../api/finance'
import type { AdjustRequest, FinanceRequest, FinancialTransaction, TransactionType, UploadResult } from '../../types'
import UploadResultPanel from '../../components/UploadResultPanel'
import styles from './Finance.module.css'

ModuleRegistry.registerModules([AllCommunityModule])

const TYPES: TransactionType[] = ['BOX_SALE','PAYMENT_RECEIVED','RECHARGE','REFUND','CREDIT','DEBIT','ADJUSTMENT','REVERSAL','OTHER']

const COLS: ColDef<FinancialTransaction>[] = [
  { field: 'transactionDate', headerName: 'Date', width: 120 },
  { field: 'retailerCode', headerName: 'Retailer', width: 120 },
  { field: 'transactionType', headerName: 'Type', width: 140 },
  { field: 'amount', headerName: 'Amount', width: 120, valueFormatter: (p) => `₹${p.value}` },
  { field: 'transactionStatus', headerName: 'Status', width: 110 },
  { field: 'reference', headerName: 'Reference', flex: 1 },
  { field: 'paymentMethod', headerName: 'Method', width: 120 },
  { field: 'source', headerName: 'Source', width: 90 },
]

const EMPTY_FORM: FinanceRequest = {
  retailerId: 0, transactionType: 'PAYMENT_RECEIVED', transactionDate: '', amount: 0,
}

export default function FinancePage() {
  const gridRef = useRef<AgGridReact<FinancialTransaction>>(null)
  const [rowData, setRowData] = useState<FinancialTransaction[]>([])
  const [selected, setSelected] = useState<FinancialTransaction | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState<FinanceRequest>(EMPTY_FORM)
  const [adjustForm, setAdjustForm] = useState<AdjustRequest>({ amount: 0, description: '' })
  const [showAdjust, setShowAdjust] = useState(false)
  const [uploadResult, setUploadResult] = useState<UploadResult | null>(null)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')

  const fetchData = () =>
    getTransactions({ page: 0, size: 200 }).then(d => setRowData(d.content)).catch(() => {})

  useEffect(() => { fetchData() }, [])

  const refresh = () => fetchData()

  const handleCreate = async () => {
    if (!form.retailerId || !form.transactionDate || form.amount <= 0) {
      setError('Retailer ID, date and a positive amount are required.'); return
    }
    try { await createTransaction(form); setForm(EMPTY_FORM); setShowForm(false); refresh() }
    catch { setError('Failed to create transaction.') }
  }

  const handleReverse = async () => {
    if (!selected) return
    try { await reverseTransaction(selected.id); setSelected(null); refresh() }
    catch { setError('Reversal failed.') }
  }

  const handleAdjust = async () => {
    if (!selected || adjustForm.amount === 0 || !adjustForm.description) {
      setError('Amount and description are required for adjustment.'); return
    }
    try { await adjustTransaction(selected.id, adjustForm); setSelected(null); setShowAdjust(false); refresh() }
    catch { setError('Adjustment failed.') }
  }

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    try { const r = await uploadFinance(file); setUploadResult(r); refresh() }
    finally { setUploading(false); e.target.value = '' }
  }

  return (
    <div className={styles.page}>
      <div className={styles.toolbar}>
        <h2>Finance Transactions</h2>
        <div className={styles.actions}>
          {selected && (
            <>
              <button onClick={handleReverse}>Reverse</button>
              <button onClick={() => setShowAdjust(!showAdjust)}>Adjust</button>
            </>
          )}
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
          <select value={form.transactionType} onChange={(e) => setForm({ ...form, transactionType: e.target.value as TransactionType })}>
            {TYPES.map((t) => <option key={t}>{t}</option>)}
          </select>
          <input type="date" value={form.transactionDate} onChange={(e) => setForm({ ...form, transactionDate: e.target.value })} />
          <input type="number" placeholder="Amount" min={0} step="0.01" value={form.amount || ''}
            onChange={(e) => setForm({ ...form, amount: +e.target.value })} />
          <input placeholder="Reference (optional)" value={form.reference ?? ''}
            onChange={(e) => setForm({ ...form, reference: e.target.value })} />
          <input placeholder="Description (optional)" value={form.description ?? ''}
            onChange={(e) => setForm({ ...form, description: e.target.value })} />
          <button onClick={handleCreate}>Save</button>
          <button onClick={() => setShowForm(false)}>Cancel</button>
        </div>
      )}

      {showAdjust && selected && (
        <div className={styles.form}>
          <strong>Adjust: {selected.reference}</strong>
          <input type="number" placeholder="Adjustment amount" step="0.01" value={adjustForm.amount || ''}
            onChange={(e) => setAdjustForm({ ...adjustForm, amount: +e.target.value })} />
          <input placeholder="Description" value={adjustForm.description}
            onChange={(e) => setAdjustForm({ ...adjustForm, description: e.target.value })} />
          <button onClick={handleAdjust}>Apply</button>
          <button onClick={() => setShowAdjust(false)}>Cancel</button>
        </div>
      )}

      {uploadResult && <UploadResultPanel result={uploadResult} />}

      <div className="ag-theme-alpine" style={{ flex: 1, marginTop: 12 }}>
        <AgGridReact<FinancialTransaction>
          ref={gridRef}
          columnDefs={COLS}
          rowData={rowData}
          rowSelection="single"
          onRowClicked={(e) => { setSelected(e.data ?? null); setError(''); setShowAdjust(false) }}
          pagination
          paginationPageSize={20}
          domLayout="autoHeight"
        />
      </div>
    </div>
  )
}
