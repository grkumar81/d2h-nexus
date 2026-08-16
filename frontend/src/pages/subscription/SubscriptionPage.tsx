import { useMemo, useRef, useState } from 'react'
import { AgGridReact } from 'ag-grid-react'
import type { ColDef, IServerSideDatasource } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import { listTenants, renewTenant, suspendTenant } from '../../api/platform'
import type { PlatformTenantDto, SubscriptionStatus } from '../../types'
import styles from './Subscription.module.css'

ModuleRegistry.registerModules([AllCommunityModule])

const STATUS_BADGE: Record<SubscriptionStatus, string> = {
  ACTIVE: '✅ Active',
  ACTIVE_WITH_EXPIRY: '🟢 Active',
  EXPIRY_WARNING: '⚠️ Expiring Soon',
  GRACE_PERIOD: '🟠 Grace Period',
  EXPIRED: '🔴 Expired',
}

const COLS: ColDef<PlatformTenantDto>[] = [
  { field: 'tenantCode', headerName: 'Code', width: 130 },
  { field: 'name', headerName: 'Name', flex: 1 },
  { field: 'status', headerName: 'Status', width: 110 },
  {
    field: 'subscriptionStatus', headerName: 'Subscription', width: 160,
    valueFormatter: p => STATUS_BADGE[p.value as SubscriptionStatus] ?? p.value,
  },
  { field: 'subscriptionExpiry', headerName: 'Expiry', width: 120 },
  {
    field: 'daysUntilExpiry', headerName: 'Days Left', width: 100,
    valueFormatter: p => p.value === Number.MAX_SAFE_INTEGER ? '∞' : String(p.value),
  },
  { field: 'gracePeriodDays', headerName: 'Grace Days', width: 110 },
]

export default function SubscriptionPage() {
  const gridRef = useRef<AgGridReact<PlatformTenantDto>>(null)
  const [selected, setSelected] = useState<PlatformTenantDto | null>(null)
  const [showRenew, setShowRenew] = useState(false)
  const [renewForm, setRenewForm] = useState({ subscriptionExpiry: '', gracePeriodDays: '' })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const datasource: IServerSideDatasource = useMemo(() => ({
    getRows(params) {
      const { startRow = 0, endRow = 20 } = params.request
      const page = Math.floor(startRow / (endRow - startRow))
      const size = endRow - startRow
      listTenants(page, size)
        .then(data => params.success({ rowData: data.content, rowCount: data.totalElements }))
        .catch(() => params.fail())
    },
  }), [])

  const refresh = () => gridRef.current?.api.refreshServerSide({ purge: true })

  const handleRenew = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!selected) return
    setError('')
    setSaving(true)
    try {
      await renewTenant(
        selected.id,
        renewForm.subscriptionExpiry,
        renewForm.gracePeriodDays ? parseInt(renewForm.gracePeriodDays) : undefined
      )
      setShowRenew(false)
      setSelected(null)
      refresh()
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to renew')
    } finally {
      setSaving(false)
    }
  }

  const handleSuspend = async () => {
    if (!selected) return
    try {
      await suspendTenant(selected.id)
      refresh()
    } catch (err: any) {
      alert(err.response?.data?.message ?? 'Failed to suspend')
    }
  }

  const openRenew = () => {
    const sel = gridRef.current?.api.getSelectedRows()[0]
    if (!sel) return
    setSelected(sel)
    setRenewForm({
      subscriptionExpiry: sel.subscriptionExpiry ?? '',
      gracePeriodDays: String(sel.gracePeriodDays),
    })
    setError('')
    setShowRenew(true)
  }

  return (
    <div className={styles.page}>
      <div className={styles.toolbar}>
        <h2>Subscription Management</h2>
        <div className={styles.actions}>
          <button className={styles.btnPrimary} onClick={openRenew}>Renew</button>
          <button className={styles.btnWarn} onClick={handleSuspend}>Suspend</button>
        </div>
      </div>
      <p className={styles.hint}>Select a tenant row then use the actions above.</p>

      <div className="ag-theme-alpine" style={{ flex: 1, marginTop: 8 }}>
        <AgGridReact<PlatformTenantDto>
          ref={gridRef}
          columnDefs={COLS}
          rowModelType="serverSide"
          serverSideDatasource={datasource}
          rowSelection="single"
          pagination
          paginationPageSize={20}
          domLayout="autoHeight"
        />
      </div>

      {showRenew && selected && (
        <div className={styles.overlay}>
          <div className={styles.modal}>
            <h3>Renew — {selected.name}</h3>
            {error && <p className={styles.error}>{error}</p>}
            <form onSubmit={handleRenew} className={styles.form}>
              <label>New Expiry Date *
                <input required type="date" value={renewForm.subscriptionExpiry}
                  onChange={e => setRenewForm(f => ({ ...f, subscriptionExpiry: e.target.value }))} />
              </label>
              <label>Grace Period (days)
                <input type="number" min={0} value={renewForm.gracePeriodDays}
                  onChange={e => setRenewForm(f => ({ ...f, gracePeriodDays: e.target.value }))} />
              </label>
              <div className={styles.modalActions}>
                <button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Renew'}</button>
                <button type="button" onClick={() => setShowRenew(false)}>Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
