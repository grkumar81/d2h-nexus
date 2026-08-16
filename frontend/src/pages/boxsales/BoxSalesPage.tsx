import { useEffect, useRef, useState } from 'react'
import { AgGridReact } from 'ag-grid-react'
import type { ColDef } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import { getBoxSales } from '../../api/boxsales'
import type { BoxSale } from '../../types'
import styles from './BoxSales.module.css'

ModuleRegistry.registerModules([AllCommunityModule])

const COLS: ColDef<BoxSale>[] = [
  { field: 'retailerCode', headerName: 'Retailer', width: 120 },
  { field: 'retailerName', headerName: 'Name', flex: 1 },
  { field: 'transactionDate', headerName: 'Date', width: 120 },
  { field: 'totalAmount', headerName: 'Total', width: 120, valueFormatter: (p) => `₹${p.value}` },
  { field: 'paymentStatus', headerName: 'Payment', width: 110 },
  { field: 'reference', headerName: 'Reference', width: 150 },
]

export default function BoxSalesPage() {
  const gridRef = useRef<AgGridReact<BoxSale>>(null)
  const [rowData, setRowData] = useState<BoxSale[]>([])

  useEffect(() => {
    getBoxSales({ page: 0, size: 200, sortBy: 'transactionDate', sortDir: 'desc' })
      .then(d => setRowData(d.content))
      .catch(() => {})
  }, [])

  return (
    <div className={styles.page}>
      <div className={styles.toolbar}>
        <h2>Box Sales</h2>
      </div>
      <div className="ag-theme-alpine" style={{ flex: 1, marginTop: 12 }}>
        <AgGridReact<BoxSale>
          ref={gridRef}
          columnDefs={COLS}
          rowData={rowData}
          rowSelection="single"
          pagination
          paginationPageSize={20}
          domLayout="autoHeight"
        />
      </div>
    </div>
  )
}
