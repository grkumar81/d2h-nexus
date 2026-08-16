import { useEffect, useRef, useState } from 'react'
import { AgGridReact } from 'ag-grid-react'
import type { ColDef } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import { getAssets, assignAsset, unassignAsset, updateAssetStatus } from '../../api/assets'
import type { Asset, AssetStatus } from '../../types'
import styles from './Assets.module.css'

ModuleRegistry.registerModules([AllCommunityModule])

const STATUSES: AssetStatus[] = ['AVAILABLE', 'ALLOCATED', 'SOLD', 'ACTIVATED', 'RETURNED', 'DAMAGED', 'LOST']

const COLS: ColDef<Asset>[] = [
  { field: 'serialNumber', headerName: 'Serial No.', width: 160 },
  { field: 'model', headerName: 'Model', width: 140 },
  { field: 'manufacturer', headerName: 'Manufacturer', width: 140 },
  { field: 'status', headerName: 'Status', width: 110 },
  { field: 'retailerCode', headerName: 'Retailer', width: 120 },
  { field: 'purchaseDate', headerName: 'Purchased', width: 120 },
  { field: 'taggingDate', headerName: 'Tagged', width: 120 },
]

export default function AssetsPage() {
  const gridRef = useRef<AgGridReact<Asset>>(null)
  const [rowData, setRowData] = useState<Asset[]>([])
  const [statusFilter, setStatusFilter] = useState('')
  const [selected, setSelected] = useState<Asset | null>(null)
  const [retailerCode, setRetailerCode] = useState('')
  const [newStatus, setNewStatus] = useState<AssetStatus>('AVAILABLE')
  const [error, setError] = useState('')

  const fetchData = (status: string) =>
    getAssets({ page: 0, size: 200, ...(status && { status }) })
      .then(d => setRowData(d.content))
      .catch(() => {})

  useEffect(() => { fetchData(statusFilter) }, [statusFilter])

  const refresh = () => fetchData(statusFilter)

  const handleAssign = async () => {
    if (!selected || !retailerCode.trim()) return
    try { await assignAsset(selected.id, retailerCode.trim()); setSelected(null); refresh() }
    catch { setError('Assign failed.') }
  }

  const handleUnassign = async () => {
    if (!selected) return
    try { await unassignAsset(selected.id); setSelected(null); refresh() }
    catch { setError('Unassign failed.') }
  }

  const handleStatusChange = async () => {
    if (!selected) return
    try { await updateAssetStatus(selected.id, newStatus); setSelected(null); refresh() }
    catch { setError('Status update failed.') }
  }

  return (
    <div className={styles.page}>
      <div className={styles.toolbar}>
        <h2>Assets</h2>
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s}>{s}</option>)}
        </select>
      </div>
      {error && <p className={styles.error}>{error}</p>}
      {selected && (
        <div className={styles.actionBar}>
          <strong>{selected.serialNumber}</strong>
          <input placeholder="Retailer code" value={retailerCode} onChange={(e) => setRetailerCode(e.target.value)} />
          <button onClick={handleAssign}>Assign</button>
          <button onClick={handleUnassign}>Unassign</button>
          <select value={newStatus} onChange={(e) => setNewStatus(e.target.value as AssetStatus)}>
            {STATUSES.map((s) => <option key={s}>{s}</option>)}
          </select>
          <button onClick={handleStatusChange}>Set Status</button>
          <button onClick={() => setSelected(null)}>✕</button>
        </div>
      )}
      <div className="ag-theme-alpine" style={{ flex: 1, marginTop: 12 }}>
        <AgGridReact<Asset>
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
