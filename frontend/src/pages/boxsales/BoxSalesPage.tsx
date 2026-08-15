import { useMemo, useRef, useState } from 'react'
import { AgGridReact } from 'ag-grid-react'
import type { ColDef, IServerSideDatasource } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import { getBoxSales, createBoxSale, cancelBoxSale } from '../../api/boxsales'
import type { BoxSale, BoxSaleRequest } from '../../types'
import styles from './BoxSales.module.css'

ModuleRegistry.registerModules([AllCommunityModule])

const COLS: ColDef<BoxSale>[] = [
  { field: 'retailerCode', headerName: 'Retailer', width: 120 },
  { field: 'retailerName', headerName: 'Name', flex: 1 },
  { field: 'saleDate', headerName: 'Date', width: 120 },
  { field: 'quantity', headerName: 'Qty', width: 80 },
  { field: 'unitPrice', headerName: 'Unit Price', width: 110, valueFormatter: (p) => `₹${p.value}` },
  { field: 'totalAmount', headerName: 'Total', width: 110, valueFormatter: (p) => `₹${p.value}` },
  { field: 'status', headerName: 'Status', width: 110 },
  { field: 'invoiceNumber', headerName: 'Invoice', width: 130 },
]

const EMPTY: BoxSaleRequest = { retailerCode: '', saleDate: '', quantity: 1, unitPrice: 0 }

export default function BoxSalesPage() {
  const gridRef = useRef<AgGridReact<BoxSale>>(null)
  const [form, setForm] = useState<BoxSaleRequest>(EMPTY)
  const [showForm, setShowForm] = useState(false)
  const [selected, setSelected] = useState<BoxSale | null>(null)
  const [error, setError] = useState('')

  const datasource: IServerSideDatasource = useMemo(() => ({
    getRows(params) {
      const { startRow = 0, endRow = 20 } = params.request
      const page = Math.floor(startRow / (endRow - startRow))
      getBoxSales({ page, size: endRow - startRow })
        .then((d) => params.success({ rowData: d.content, rowCount: d.totalElements }))
        .catch(() => params.fail())
    },
  }), [])

  const refresh = () => gridRef.current?.api.refreshServerSide({ purge: true })

  const handleCreate = async () => {
    if (!form.retailerCode || !form.saleDate || form.quantity < 1 || form.unitPrice <= 0) {
      setError('All fields are required and must be valid.'); return
    }
    try { await createBoxSale(form); setForm(EMPTY); setShowForm(false); refresh() }
    catch { setError('Failed to create sale.') }
  }

  const handleCancel = async () => {
    if (!selected) return
    try { await cancelBoxSale(selected.id); setSelected(null); refresh() }
    catch { setError('Cancel failed.') }
  }

  return (
    <div className={styles.page}>
      <div className={styles.toolbar}>
        <h2>Box Sales</h2>
        <div className={styles.actions}>
          {selected && <button className={styles.danger} onClick={handleCancel}>Cancel Sale</button>}
          <button onClick={() => { setShowForm(!showForm); setError('') }}>+ New Sale</button>
        </div>
      </div>
      {error && <p className={styles.error}>{error}</p>}
      {showForm && (
        <div className={styles.form}>
          <input placeholder="Retailer code" value={form.retailerCode} onChange={(e) => setForm({ ...form, retailerCode: e.target.value })} />
          <input type="date" value={form.saleDate} onChange={(e) => setForm({ ...form, saleDate: e.target.value })} />
          <input type="number" placeholder="Qty" min={1} value={form.quantity} onChange={(e) => setForm({ ...form, quantity: +e.target.value })} />
          <input type="number" placeholder="Unit price" min={0} step="0.01" value={form.unitPrice} onChange={(e) => setForm({ ...form, unitPrice: +e.target.value })} />
          <input placeholder="Invoice # (optional)" value={form.invoiceNumber ?? ''} onChange={(e) => setForm({ ...form, invoiceNumber: e.target.value })} />
          <button onClick={handleCreate}>Save</button>
          <button onClick={() => setShowForm(false)}>Cancel</button>
        </div>
      )}
      <div className="ag-theme-alpine" style={{ flex: 1, marginTop: 12 }}>
        <AgGridReact<BoxSale>
          ref={gridRef}
          columnDefs={COLS}
          rowModelType="serverSide"
          serverSideDatasource={datasource}
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
