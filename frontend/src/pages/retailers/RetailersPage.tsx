import { useMemo, useRef, useState } from 'react'
import { AgGridReact } from 'ag-grid-react'
import type { ColDef, IServerSideDatasource } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import { getRetailers, uploadRetailers } from '../../api/retailers'
import type { Retailer } from '../../types'
import UploadResultPanel from '../../components/UploadResultPanel'
import type { UploadResult } from '../../types'
import styles from './Retailers.module.css'

ModuleRegistry.registerModules([AllCommunityModule])

const COLS: ColDef<Retailer>[] = [
  { field: 'retailerCode', headerName: 'Code', width: 120 },
  { field: 'name', headerName: 'Name', flex: 1 },
  { field: 'contactPerson', headerName: 'Contact', flex: 1 },
  { field: 'phone', headerName: 'Phone', width: 140 },
  { field: 'city', headerName: 'City', width: 120 },
  { field: 'state', headerName: 'State', width: 100 },
  { field: 'active', headerName: 'Active', width: 90, valueFormatter: (p) => p.value ? 'Yes' : 'No' },
]

export default function RetailersPage() {
  const gridRef = useRef<AgGridReact<Retailer>>(null)
  const [search, setSearch] = useState('')
  const [uploadResult, setUploadResult] = useState<UploadResult | null>(null)
  const [uploading, setUploading] = useState(false)

  const datasource: IServerSideDatasource = useMemo(() => ({
    getRows(params) {
      const { startRow = 0, endRow = 20 } = params.request
      const page = Math.floor(startRow / (endRow - startRow))
      const size = endRow - startRow
      getRetailers({ page, size, search })
        .then((data) => params.success({ rowData: data.content, rowCount: data.totalElements }))
        .catch(() => params.fail())
    },
  }), [search])

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    try {
      const result = await uploadRetailers(file)
      setUploadResult(result)
      gridRef.current?.api.refreshServerSide({ purge: true })
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
            onChange={(e) => { setSearch(e.target.value); gridRef.current?.api.refreshServerSide({ purge: true }) }}
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
          rowModelType="serverSide"
          serverSideDatasource={datasource}
          pagination
          paginationPageSize={20}
          domLayout="autoHeight"
        />
      </div>
    </div>
  )
}
