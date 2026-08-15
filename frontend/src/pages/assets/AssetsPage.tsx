import { useMemo, useRef, useState } from 'react'
import { AgGridReact } from 'ag-grid-react'
import type { ColDef, IServerSideDatasource } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import { getAssets, assignAsset, unassignAsset, updateAssetStatus } from '../../api/assets'
import type { Asset, AssetStatus } from '../../types'
import styles from './Assets.module.css'

ModuleRegistry.registerModules([AllCommunityModule])

const STATUSES: AssetStatus[] = ['AVAILABLE', 'ASSIGNED', 'FAULTY', 'RETIRED']

const COLS: ColDef<Asset>[] = [
  { field: 'serialNumber', headerName: 'Serial No.', width: 160 },
  { field: 'modelNumber', headerName: 'Model', width: 140 },
  { field: 'assetType', headerName: 'Type', width: 120 },
  { field: 'status', headerName: 'Status', width: 110 },
  { field: 'retailerCode', headerName: 'Retailer', width: 120 },
  { field: 'purchaseDate', headerName: 'Purchased', width: 120 },
  { field: 'warrantyExpiry', headerName: 'Warranty', width: 120 },
]

export default function AssetsPage() {
  const gridRef = useRef<AgGridReact<Asset>>(null)
  const [statusFilter, setStatusFilter] = useState('')
  const [selected, setSelected] = useState<Asset | null>(null)
  const [retailerCode, setRetailerCode] = useState('')
  const [newStatus, setNewStatus] = useState<AssetStatus>('AVAILABLE')
  const [error, setError] = useState('')

  const datasource: IServerSideDatasource = useMemo(() => ({
    getRows(params) {
      const { startRow = 0, endRow = 20 } = params.request
      const page = Math.floor(startRow / (endRow - startRow))
      const size = endRow - startRow
      getAssets({ page, size, ...(statusFilter && { status: statusFilter }) })
        .then((d) => params.success({ rowData: d.content, rowCount: d.totalElements }))
        .catch(() => params.fail())
    },
  }), [statusFilter])

  const refresh = () => gridRef.current?.api.refreshServerSide({ purge: true })

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
        <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); refresh() }}>
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
