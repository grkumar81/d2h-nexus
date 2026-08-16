import { useEffect, useRef, useState } from 'react'
import { AgGridReact } from 'ag-grid-react'
import type { ColDef } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import { getRetailers, uploadRetailers } from '../../api/retailers'
import type { Retailer, UploadResult } from '../../types'
import UploadResultPanel from '../../components/UploadResultPanel'
import styles from './Retailers.module.css'

ModuleRegistry.registerModules([AllCommunityModule])

const COLS: ColDef<Retailer>[] = [
  { field: 'retailerCode', headerName: 'Code', width: 120 },
  { field: 'retailerName', headerName: 'Name', flex: 1 },
  { field: 'mobile', headerName: 'Phone', width: 140 },
  { field: 'city', headerName: 'City', width: 120 },
  { field: 'state', headerName: 'State', width: 100 },
  { field: 'status', headerName: 'Status', width: 100 },
]

export default function RetailersPage() {
  const gridRef = useRef<AgGridReact<Retailer>>(null)
  const [rowData, setRowData] = useState<Retailer[]>([])
  const [search, setSearch] = useState('')
  const [uploadResult, setUploadResult] = useState<UploadResult | null>(null)
  const [uploading, setUploading] = useState(false)

  const fetchData = (q: string) =>
    getRetailers({ page: 0, size: 200, ...(q && { query: q }) })
      .then(d => setRowData(d.content))
      .catch(() => {})

  useEffect(() => { fetchData(search) }, [search])

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    try {
      const result = await uploadRetailers(file)
      setUploadResult(result)
      fetchData(search)
    } finally {
      setUploading(false)
      e.target.value = ''
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.toolbar}>
        <h2>Retailers</h2>
        <div className={styles.actions}>
          <input
            placeholder="Search…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className={styles.search}
          />
          <label className={styles.uploadBtn}>
            {uploading ? 'Uploading…' : 'Upload CSV/Excel'}
            <input type="file" accept=".csv,.xlsx,.xls" onChange={handleUpload} hidden />
          </label>
        </div>
      </div>
      {uploadResult && <UploadResultPanel result={uploadResult} />}
      <div className="ag-theme-alpine" style={{ flex: 1, marginTop: 12 }}>
        <AgGridReact<Retailer>
          ref={gridRef}
          columnDefs={COLS}
          rowData={rowData}
          pagination
          paginationPageSize={20}
          domLayout="autoHeight"
        />
      </div>
    </div>
  )
}
