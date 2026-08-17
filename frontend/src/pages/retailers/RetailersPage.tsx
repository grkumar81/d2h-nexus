import { useEffect, useRef, useState } from 'react'
import { AgGridReact } from 'ag-grid-react'
import type { ColDef, ICellRendererParams } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import {
  getRetailers, createRetailer, updateRetailer,
  activateRetailer, deactivateRetailer, uploadRetailers,
} from '../../api/retailers'
import type { Retailer, RetailerRequest, UploadResult } from '../../types'
import UploadResultPanel from '../../components/UploadResultPanel'
import styles from './Retailers.module.css'

ModuleRegistry.registerModules([AllCommunityModule])

const EMPTY: RetailerRequest = {
  retailerCode: '', retailerName: '', mobile: '', alternateMobile: '',
  email: '', address: '', city: '', state: '', pinCode: '',
  gstNumber: '', panNumber: '', joiningDate: '',
}

export default function RetailersPage() {
  const gridRef = useRef<AgGridReact<Retailer>>(null)
  const [rowData, setRowData] = useState<Retailer[]>([])
  const [search, setSearch] = useState('')
  const [uploadResult, setUploadResult] = useState<UploadResult | null>(null)
  const [uploading, setUploading] = useState(false)
  const [showFormat, setShowFormat] = useState(false)

  // Modal state
  const [modal, setModal] = useState<'add' | 'edit' | null>(null)
  const [editing, setEditing] = useState<Retailer | null>(null)
  const [form, setForm] = useState<RetailerRequest>(EMPTY)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')

  const fetchData = (q: string) =>
    getRetailers({ page: 0, size: 500, ...(q && { query: q }) })
      .then(d => setRowData(d.content))
      .catch(() => {})

  useEffect(() => { fetchData(search) }, [search])

  const openAdd = () => { setForm(EMPTY); setEditing(null); setFormError(''); setModal('add') }

  const openEdit = (r: Retailer) => {
    setEditing(r)
    setForm({
      retailerName: r.retailerName,
      mobile: r.mobile ?? '',
      alternateMobile: r.alternateMobile ?? '',
      email: r.email ?? '',
      address: r.address ?? '',
      city: r.city ?? '',
      state: r.state ?? '',
      pinCode: r.pinCode ?? '',
      gstNumber: r.gstNumber ?? '',
      panNumber: r.panNumber ?? '',
      joiningDate: r.joiningDate ?? '',
    })
    setFormError('')
    setModal('edit')
  }

  const closeModal = () => { setModal(null); setEditing(null) }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true); setFormError('')
    try {
      if (modal === 'add') {
        await createRetailer(form)
      } else if (editing) {
        await updateRetailer(editing.id, form)
      }
      closeModal()
      fetchData(search)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setFormError(msg ?? 'Failed to save. Check your inputs.')
    } finally {
      setSaving(false)
    }
  }

  const handleToggleStatus = async (r: Retailer) => {
    try {
      if (r.status === 'ACTIVE') await deactivateRetailer(r.id)
      else await activateRetailer(r.id)
      fetchData(search)
    } catch { /* ignore */ }
  }

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

  const set = (k: keyof RetailerRequest) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm(f => ({ ...f, [k]: e.target.value }))

  const COLS: ColDef<Retailer>[] = [
    { field: 'retailerCode', headerName: 'Code', width: 110 },
    { field: 'retailerName', headerName: 'Name', flex: 1, minWidth: 140 },
    { field: 'mobile', headerName: 'Mobile', width: 120 },
    { field: 'city', headerName: 'City', width: 110 },
    { field: 'state', headerName: 'State', width: 100 },
    {
      field: 'status', headerName: 'Status', width: 100,
      cellRenderer: (p: ICellRendererParams<Retailer>) => (
        <span style={{
          padding: '2px 8px', borderRadius: 12, fontSize: 12, fontWeight: 600,
          background: p.value === 'ACTIVE' ? '#dcfce7' : '#fee2e2',
          color: p.value === 'ACTIVE' ? '#16a34a' : '#dc2626',
        }}>{p.value}</span>
      ),
    },
    {
      headerName: 'Actions', width: 160, sortable: false, filter: false,
      cellRenderer: (p: ICellRendererParams<Retailer>) => p.data ? (
        <div style={{ display: 'flex', gap: 6, alignItems: 'center', height: '100%' }}>
          <button className={styles.btnEdit} onClick={() => openEdit(p.data!)}>Edit</button>
          <button
            className={p.data.status === 'ACTIVE' ? styles.btnDeactivate : styles.btnActivate}
            onClick={() => handleToggleStatus(p.data!)}
          >
            {p.data.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
          </button>
        </div>
      ) : null,
    },
  ]

  return (
    <div className={styles.page}>
      {/* Toolbar */}
      <div className={styles.toolbar}>
        <h2>Retailers</h2>
        <div className={styles.actions}>
          <input
            placeholder="Search…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            className={styles.search}
          />
          <button className={styles.btnPrimary} onClick={openAdd}>+ Add Retailer</button>
          <label className={styles.uploadBtn}>
            {uploading ? 'Uploading…' : '⬆ Upload CSV/Excel'}
            <input type="file" accept=".csv,.xlsx,.xls" onChange={handleUpload} hidden />
          </label>
          <button className={styles.btnFormat} onClick={() => setShowFormat(v => !v)}>
            {showFormat ? 'Hide Format' : '? Upload Format'}
          </button>
        </div>
      </div>

      {/* Upload format info */}
      {showFormat && (
        <div className={styles.formatPanel}>
          <div className={styles.formatTitle}>📋 Upload File Format (CSV or Excel)</div>
          <p className={styles.formatNote}>
            Required columns: <strong>retailer_code</strong>, <strong>retailer_name</strong>, <strong>mobile</strong>.
            All other columns are optional.
          </p>
          <div className={styles.formatTableWrap}>
            <table className={styles.formatTable}>
              <thead>
                <tr>
                  {['retailer_code','retailer_name','mobile','alternate_mobile','email','address','city','state','pin_code','gst_number','pan_number','joining_date'].map(h => (
                    <th key={h}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>RET001</td><td>Sharma Electronics</td><td>9876543210</td>
                  <td>9876543211</td><td>sharma@email.com</td><td>123 Main St</td>
                  <td>Mumbai</td><td>Maharashtra</td><td>400001</td>
                  <td>27AAPFU0939F1ZV</td><td>AAPFU0939F</td><td>2024-01-15</td>
                </tr>
              </tbody>
            </table>
          </div>
          <ul className={styles.formatRules}>
            <li><strong>retailer_code</strong> — unique, letters/digits/hyphens/underscores only</li>
            <li><strong>mobile</strong> — exactly 10 digits</li>
            <li><strong>alternate_mobile</strong> — exactly 10 digits (optional)</li>
            <li><strong>pin_code</strong> — exactly 6 digits (optional)</li>
            <li><strong>joining_date</strong> — format <code>YYYY-MM-DD</code> (optional)</li>
            <li><strong>gst_number</strong> — 15-char GST format (optional)</li>
            <li><strong>pan_number</strong> — 10-char PAN format (optional)</li>
          </ul>
        </div>
      )}

      {uploadResult && <UploadResultPanel result={uploadResult} />}

      {/* Grid */}
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

      {/* Add / Edit Modal */}
      {modal && (
        <div className={styles.overlay} onClick={closeModal}>
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <h3>{modal === 'add' ? 'Add Retailer' : `Edit — ${editing?.retailerCode}`}</h3>
            <form className={styles.form} onSubmit={handleSave}>
              <div className={styles.formGrid}>
                {modal === 'add' && (
                  <label className={styles.fieldFull}>
                    <span>Retailer Code *</span>
                    <input value={form.retailerCode ?? ''} onChange={set('retailerCode')} required placeholder="e.g. RET001" />
                  </label>
                )}
                <label className={styles.fieldFull}>
                  <span>Retailer Name *</span>
                  <input value={form.retailerName} onChange={set('retailerName')} required />
                </label>
                <label>
                  <span>Mobile *</span>
                  <input value={form.mobile} onChange={set('mobile')} required placeholder="10 digits" maxLength={10} />
                </label>
                <label>
                  <span>Alternate Mobile</span>
                  <input value={form.alternateMobile ?? ''} onChange={set('alternateMobile')} placeholder="10 digits" maxLength={10} />
                </label>
                <label>
                  <span>Email</span>
                  <input type="email" value={form.email ?? ''} onChange={set('email')} />
                </label>
                <label>
                  <span>City</span>
                  <input value={form.city ?? ''} onChange={set('city')} />
                </label>
                <label>
                  <span>State</span>
                  <input value={form.state ?? ''} onChange={set('state')} />
                </label>
                <label>
                  <span>PIN Code</span>
                  <input value={form.pinCode ?? ''} onChange={set('pinCode')} placeholder="6 digits" maxLength={6} />
                </label>
                <label className={styles.fieldFull}>
                  <span>Address</span>
                  <input value={form.address ?? ''} onChange={set('address')} />
                </label>
                <label>
                  <span>GST Number</span>
                  <input value={form.gstNumber ?? ''} onChange={set('gstNumber')} placeholder="15-char GST" maxLength={15} />
                </label>
                <label>
                  <span>PAN Number</span>
                  <input value={form.panNumber ?? ''} onChange={set('panNumber')} placeholder="10-char PAN" maxLength={10} />
                </label>
                <label>
                  <span>Joining Date</span>
                  <input type="date" value={form.joiningDate ?? ''} onChange={set('joiningDate')} />
                </label>
              </div>
              {formError && <p className={styles.error}>{formError}</p>}
              <div className={styles.modalActions}>
                <button type="button" onClick={closeModal}>Cancel</button>
                <button type="submit" disabled={saving}>
                  {saving ? 'Saving…' : modal === 'add' ? 'Add Retailer' : 'Save Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
