import { useEffect, useRef, useState } from 'react'
import { AgGridReact } from 'ag-grid-react'
import type { ColDef, ICellRendererParams } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import * as XLSX from 'xlsx-js-style'
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

// Send null for empty optional fields so backend pattern validators don't fire on ''
function sanitize(f: RetailerRequest): RetailerRequest {
  const opt = (v?: string) => (v?.trim() ? v.trim() : undefined)
  return {
    retailerCode: f.retailerCode?.trim() || undefined,
    retailerName: f.retailerName.trim(),
    mobile: f.mobile.trim(),
    alternateMobile: opt(f.alternateMobile),
    email: opt(f.email),
    address: opt(f.address),
    city: opt(f.city),
    state: opt(f.state),
    pinCode: opt(f.pinCode),
    gstNumber: opt(f.gstNumber),
    panNumber: opt(f.panNumber),
    joiningDate: opt(f.joiningDate),
  }
}

// Extract readable message from backend error response
function extractError(err: unknown): string {
  const data = (err as { response?: { data?: { message?: string; fieldErrors?: { field: string; message: string }[] } } })?.response?.data
  if (!data) return 'Failed to save. Please try again.'
  if (data.fieldErrors?.length) {
    return data.fieldErrors.map(fe => {
      const label = fe.field
        .replace(/([A-Z])/g, ' $1')
        .replace(/^./, s => s.toUpperCase())
      return `${label}: ${fe.message}`
    }).join('\n')
  }
  return data.message ?? 'Failed to save. Please try again.'
}

export default function RetailersPage() {
  const gridRef = useRef<AgGridReact<Retailer>>(null)
  const [rowData, setRowData] = useState<Retailer[]>([])
  const [search, setSearch] = useState('')
  const [uploadResult, setUploadResult] = useState<UploadResult | null>(null)
  const [uploading, setUploading] = useState(false)
  const [showFormat, setShowFormat] = useState(false)

  // Add / Edit modal
  const [modal, setModal] = useState<'add' | 'edit' | null>(null)
  const [editing, setEditing] = useState<Retailer | null>(null)
  const [form, setForm] = useState<RetailerRequest>(EMPTY)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')

  // Confirm dialog for activate / deactivate
  const [confirm, setConfirm] = useState<{ retailer: Retailer; action: 'activate' | 'deactivate' } | null>(null)
  const [confirming, setConfirming] = useState(false)

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
      const payload = sanitize(form)
      if (modal === 'add') await createRetailer(payload)
      else if (editing) await updateRetailer(editing.id, payload)
      closeModal()
      fetchData(search)
    } catch (err) {
      setFormError(extractError(err))
    } finally {
      setSaving(false)
    }
  }

  const handleConfirmToggle = async () => {
    if (!confirm) return
    setConfirming(true)
    try {
      if (confirm.action === 'deactivate') await deactivateRetailer(confirm.retailer.id)
      else await activateRetailer(confirm.retailer.id)
      fetchData(search)
    } catch { /* ignore */ } finally {
      setConfirming(false)
      setConfirm(null)
    }
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

  const downloadTemplate = () => {
    const columns = [
      { label: 'Retailer Code',    hint: 'Unique code — letters, digits, hyphens and underscores only',  required: true  },
      { label: 'Retailer Name',    hint: 'Full name of the retailer',                                     required: true  },
      { label: 'Mobile',           hint: '10-digit mobile number',                                        required: true  },
      { label: 'Alternate Mobile', hint: '10-digit alternate mobile number',                              required: false },
      { label: 'Email',            hint: 'Valid email address (e.g. name@domain.com)',                    required: false },
      { label: 'Address',          hint: 'Full street address',                                           required: false },
      { label: 'City',             hint: 'City name',                                                     required: false },
      { label: 'State',            hint: 'State name',                                                    required: false },
      { label: 'Pin Code',         hint: '6-digit PIN code',                                              required: false },
      { label: 'GST Number',       hint: '15-character GST number (e.g. 27AAPFU0939F1ZV)',               required: false },
      { label: 'PAN Number',       hint: '10-character PAN number (e.g. AAPFU0939F)',                    required: false },
      { label: 'Joining Date',     hint: 'Date in YYYY-MM-DD format (e.g. 2024-01-15)',                  required: false },
    ]

    // Row 1: format hints (light grey italic)
    const hintRow = columns.map(c => ({ v: c.hint, t: 's', s: {
      font: { italic: true, color: { rgb: '6B7280' }, sz: 9 },
      fill: { patternType: 'solid', fgColor: { rgb: 'F3F4F6' } },
      alignment: { horizontal: 'left', vertical: 'center', wrapText: true },
    }}))

    // Row 2: column headers — mandatory red, optional blue
    const headerRow = columns.map(c => ({ v: c.label, t: 's', s: {
      font: { bold: true, sz: 11, color: { rgb: c.required ? 'CC0000' : '1E56A0' } },
      fill: { patternType: 'solid', fgColor: { rgb: c.required ? 'FFE4E4' : 'EFF6FF' } },
      alignment: { horizontal: 'center', vertical: 'center' },
      border: {
        top:    { style: 'thin',   color: { rgb: c.required ? 'CC0000' : '2E6FD4' } },
        bottom: { style: 'medium', color: { rgb: c.required ? 'CC0000' : '2E6FD4' } },
        left:   { style: 'thin',   color: { rgb: 'D1D5DB' } },
        right:  { style: 'thin',   color: { rgb: 'D1D5DB' } },
      },
    }}))

    const ws = XLSX.utils.aoa_to_sheet([hintRow, headerRow])
    ws['!cols']  = columns.map(() => ({ wch: 32 }))
    ws['!rows']  = [{ hpt: 36 }, { hpt: 20 }]

    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, 'Retailers')
    XLSX.writeFile(wb, 'Retailer_Upload_Template.xlsx')
  }

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
      headerName: 'Actions', width: 170, sortable: false, filter: false,
      cellRenderer: (p: ICellRendererParams<Retailer>) => p.data ? (
        <div style={{ display: 'flex', gap: 6, alignItems: 'center', height: '100%' }}>
          <button className={styles.btnEdit} onClick={() => openEdit(p.data!)}>Edit</button>
          <button
            className={p.data.status === 'ACTIVE' ? styles.btnDeactivate : styles.btnActivate}
            onClick={() => setConfirm({
              retailer: p.data!,
              action: p.data!.status === 'ACTIVE' ? 'deactivate' : 'activate',
            })}
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
          <button className={styles.btnDownload} onClick={downloadTemplate}>
            ⬇ Download Template
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
                  <span>Alternate Mobile <em>(optional)</em></span>
                  <input value={form.alternateMobile ?? ''} onChange={set('alternateMobile')} placeholder="10 digits" maxLength={10} />
                </label>
                <label>
                  <span>Email <em>(optional)</em></span>
                  <input value={form.email ?? ''} onChange={set('email')} placeholder="example@email.com" />
                </label>
                <label>
                  <span>City <em>(optional)</em></span>
                  <input value={form.city ?? ''} onChange={set('city')} />
                </label>
                <label>
                  <span>State <em>(optional)</em></span>
                  <input value={form.state ?? ''} onChange={set('state')} />
                </label>
                <label>
                  <span>PIN Code <em>(optional)</em></span>
                  <input value={form.pinCode ?? ''} onChange={set('pinCode')} placeholder="6 digits" maxLength={6} />
                </label>
                <label className={styles.fieldFull}>
                  <span>Address <em>(optional)</em></span>
                  <input value={form.address ?? ''} onChange={set('address')} />
                </label>
                <label>
                  <span>GST Number <em>(optional)</em></span>
                  <input value={form.gstNumber ?? ''} onChange={set('gstNumber')} placeholder="15-char GST" maxLength={15} />
                </label>
                <label>
                  <span>PAN Number <em>(optional)</em></span>
                  <input value={form.panNumber ?? ''} onChange={set('panNumber')} placeholder="10-char PAN" maxLength={10} />
                </label>
                <label>
                  <span>Joining Date <em>(optional)</em></span>
                  <input type="date" value={form.joiningDate ?? ''} onChange={set('joiningDate')} />
                </label>
              </div>
              {formError && (
                <div className={styles.errorBox}>
                  {formError.split('\n').map((line, i) => <p key={i}>{line}</p>)}
                </div>
              )}
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

      {/* Confirm activate / deactivate */}
      {confirm && (
        <div className={styles.overlay} onClick={() => setConfirm(null)}>
          <div className={styles.confirmModal} onClick={e => e.stopPropagation()}>
            <div className={confirm.action === 'deactivate' ? styles.confirmIconDanger : styles.confirmIconSuccess}>
              {confirm.action === 'deactivate' ? '⚠️' : '✅'}
            </div>
            <h3>{confirm.action === 'deactivate' ? 'Deactivate Retailer?' : 'Activate Retailer?'}</h3>
            <p>
              {confirm.action === 'deactivate'
                ? <>Are you sure you want to deactivate <strong>{confirm.retailer.retailerName}</strong> ({confirm.retailer.retailerCode})? They will no longer be accessible for new transactions.</>
                : <>Are you sure you want to activate <strong>{confirm.retailer.retailerName}</strong> ({confirm.retailer.retailerCode})?</>
              }
            </p>
            <div className={styles.confirmActions}>
              <button className={styles.btnCancel} onClick={() => setConfirm(null)} disabled={confirming}>
                Cancel
              </button>
              <button
                className={confirm.action === 'deactivate' ? styles.btnConfirmDanger : styles.btnConfirmSuccess}
                onClick={handleConfirmToggle}
                disabled={confirming}
              >
                {confirming ? 'Please wait…' : confirm.action === 'deactivate' ? 'Yes, Deactivate' : 'Yes, Activate'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
