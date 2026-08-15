import { useState, useCallback } from 'react'
import { AgGridReact } from 'ag-grid-react'
import type { ColDef } from 'ag-grid-community'
import { listAuditLogs } from '../../api/admin'
import type { AuditLog } from '../../types'
import { useAuth } from '../../context/AuthContext'
import styles from './Audit.module.css'

export default function AuditPage() {
  const { auth } = useAuth()
  const isAdmin = auth?.roles.includes('TENANT_ADMIN')

  const [rows, setRows] = useState<AuditLog[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)

  const [filters, setFilters] = useState({
    entityType: '', action: '', performedBy: '', from: '', to: ''
  })

  const load = useCallback(async (p = 0) => {
    setLoading(true)
    setError(null)
    try {
      const params = {
        entityType: filters.entityType || undefined,
        action: filters.action || undefined,
        performedBy: filters.performedBy || undefined,
        from: filters.from ? filters.from + 'T00:00:00Z' : undefined,
        to: filters.to ? filters.to + 'T23:59:59Z' : undefined,
        page: p,
        size: 50,
      }
      const data = await listAuditLogs(params)
      setRows(data.content)
      setTotal(data.totalElements)
      setPage(p)
    } catch {
      setError('Failed to load audit logs')
    } finally {
      setLoading(false)
    }
  }, [filters])

  const colDefs: ColDef<AuditLog>[] = [
    { field: 'createdAt', headerName: 'Time', width: 180,
      valueFormatter: p => p.value ? new Date(p.value as string).toLocaleString() : '' },
    { field: 'entityType', headerName: 'Entity', width: 160 },
    { field: 'entityId', headerName: 'ID', width: 100 },
    { field: 'action', headerName: 'Action', width: 130 },
    { field: 'performedBy', headerName: 'By', width: 140 },
    { field: 'details', headerName: 'Details', flex: 1 },
  ]

  if (!isAdmin) return <p className={styles.denied}>Access denied — TENANT_ADMIN role required.</p>

  return (
    <div className={styles.page}>
      <h2>Audit Log</h2>

      <div className={styles.filters}>
        <input placeholder="Entity type" value={filters.entityType}
          onChange={e => setFilters(f => ({ ...f, entityType: e.target.value }))} />
        <input placeholder="Action" value={filters.action}
          onChange={e => setFilters(f => ({ ...f, action: e.target.value }))} />
        <input placeholder="Performed by" value={filters.performedBy}
          onChange={e => setFilters(f => ({ ...f, performedBy: e.target.value }))} />
        <input type="date" value={filters.from}
          onChange={e => setFilters(f => ({ ...f, from: e.target.value }))} />
        <input type="date" value={filters.to}
          onChange={e => setFilters(f => ({ ...f, to: e.target.value }))} />
        <button className={styles.btnSearch} onClick={() => load(0)}>Search</button>
      </div>

      {error && <p className={styles.error}>{error}</p>}
      <p className={styles.count}>{total} record(s)</p>

      <div className="ag-theme-alpine" style={{ height: 480 }}>
        <AgGridReact<AuditLog>
          rowData={rows}
          columnDefs={colDefs}
          loading={loading}
          suppressCellFocus
        />
      </div>

      <div className={styles.pagination}>
        <button disabled={page === 0} onClick={() => load(page - 1)}>← Prev</button>
        <span>Page {page + 1}</span>
        <button disabled={rows.length < 50} onClick={() => load(page + 1)}>Next →</button>
      </div>
    </div>
  )
}
