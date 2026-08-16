import { useMemo, useRef, useState } from 'react'
import { AgGridReact } from 'ag-grid-react'
import type { ColDef, IServerSideDatasource } from 'ag-grid-community'
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community'
import { listUsers, createUser, updateUser, activateUser, deactivateUser, resetPassword } from '../../api/users'
import type { UserDto, CreateUserRequest, UpdateUserRequest } from '../../types'
import styles from './Users.module.css'

ModuleRegistry.registerModules([AllCommunityModule])

const ROLES = ['TENANT_ADMIN', 'FINANCE_USER', 'OPERATIONS_USER', 'READ_ONLY']

const COLS: ColDef<UserDto>[] = [
  { field: 'username', headerName: 'Username', width: 150 },
  { field: 'fullName', headerName: 'Full Name', flex: 1 },
  { field: 'email', headerName: 'Email', flex: 1 },
  { field: 'phone', headerName: 'Phone', width: 140 },
  { field: 'roles', headerName: 'Roles', flex: 1, valueFormatter: p => (p.value as string[]).join(', ') },
  { field: 'status', headerName: 'Status', width: 100 },
]

const EMPTY_CREATE: CreateUserRequest = { username: '', email: '', password: '', fullName: '', phone: '', roles: [] }

export default function UsersPage() {
  const gridRef = useRef<AgGridReact<UserDto>>(null)
  const [showCreate, setShowCreate] = useState(false)
  const [editUser, setEditUser] = useState<UserDto | null>(null)
  const [form, setForm] = useState<CreateUserRequest>(EMPTY_CREATE)
  const [editForm, setEditForm] = useState<UpdateUserRequest>({ fullName: '', phone: '', roles: [] })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const datasource: IServerSideDatasource = useMemo(() => ({
    getRows(params) {
      const { startRow = 0, endRow = 20 } = params.request
      const page = Math.floor(startRow / (endRow - startRow))
      const size = endRow - startRow
      listUsers(page, size)
        .then(data => params.success({ rowData: data.content, rowCount: data.totalElements }))
        .catch(() => params.fail())
    },
  }), [])

  const refresh = () => gridRef.current?.api.refreshServerSide({ purge: true })

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSaving(true)
    try {
      await createUser(form)
      setShowCreate(false)
      setForm(EMPTY_CREATE)
      refresh()
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to create user')
    } finally {
      setSaving(false)
    }
  }

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!editUser) return
    setError('')
    setSaving(true)
    try {
      await updateUser(editUser.id, editForm)
      setEditUser(null)
      refresh()
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to update user')
    } finally {
      setSaving(false)
    }
  }

  const handleRowAction = async (action: string, user: UserDto) => {
    try {
      if (action === 'edit') {
        setEditForm({ fullName: user.fullName ?? '', phone: user.phone ?? '', roles: [...user.roles] })
        setEditUser(user)
      } else if (action === 'activate') {
        await activateUser(user.id); refresh()
      } else if (action === 'deactivate') {
        await deactivateUser(user.id); refresh()
      } else if (action === 'reset') {
        await resetPassword(user.id); alert('Password reset successfully')
      }
    } catch (err: any) {
      alert(err.response?.data?.message ?? 'Action failed')
    }
  }

  const toggleRole = (role: string, current: string[], setter: (r: string[]) => void) => {
    setter(current.includes(role) ? current.filter(r => r !== role) : [...current, role])
  }

  return (
    <div className={styles.page}>
      <div className={styles.toolbar}>
        <h2>Users</h2>
        <button className={styles.addBtn} onClick={() => { setShowCreate(true); setError('') }}>+ Add User</button>
      </div>

      <div className={styles.gridActions}>
        <span className={styles.hint}>Click a row then use actions below</span>
        <div className={styles.rowActions}>
          {['edit', 'activate', 'deactivate', 'reset'].map(action => (
            <button key={action} className={styles.actionBtn}
              onClick={() => {
                const selected = gridRef.current?.api.getSelectedRows()[0]
                if (selected) handleRowAction(action, selected)
              }}>
              {action === 'reset' ? 'Reset Password' : action.charAt(0).toUpperCase() + action.slice(1)}
            </button>
          ))}
        </div>
      </div>

      <div className="ag-theme-alpine" style={{ flex: 1, marginTop: 12 }}>
        <AgGridReact<UserDto>
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

      {/* Create Modal */}
      {showCreate && (
        <div className={styles.overlay}>
          <div className={styles.modal}>
            <h3>Add User</h3>
            {error && <p className={styles.error}>{error}</p>}
            <form onSubmit={handleCreate} className={styles.form}>
              <label>Username *<input required value={form.username} onChange={e => setForm(f => ({ ...f, username: e.target.value }))} /></label>
              <label>Email *<input required type="email" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} /></label>
              <label>Password *<input required type="password" minLength={8} value={form.password} onChange={e => setForm(f => ({ ...f, password: e.target.value }))} /></label>
              <label>Full Name<input value={form.fullName} onChange={e => setForm(f => ({ ...f, fullName: e.target.value }))} /></label>
              <label>Phone<input value={form.phone} onChange={e => setForm(f => ({ ...f, phone: e.target.value }))} /></label>
              <fieldset className={styles.roles}>
                <legend>Roles *</legend>
                {ROLES.map(r => (
                  <label key={r} className={styles.roleCheck}>
                    <input type="checkbox" checked={form.roles.includes(r)}
                      onChange={() => toggleRole(r, form.roles, roles => setForm(f => ({ ...f, roles })))} />
                    {r}
                  </label>
                ))}
              </fieldset>
              <div className={styles.modalActions}>
                <button type="submit" disabled={saving || form.roles.length === 0}>{saving ? 'Saving…' : 'Create'}</button>
                <button type="button" onClick={() => setShowCreate(false)}>Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Modal */}
      {editUser && (
        <div className={styles.overlay}>
          <div className={styles.modal}>
            <h3>Edit User — {editUser.username}</h3>
            {error && <p className={styles.error}>{error}</p>}
            <form onSubmit={handleUpdate} className={styles.form}>
              <label>Full Name<input value={editForm.fullName} onChange={e => setEditForm(f => ({ ...f, fullName: e.target.value }))} /></label>
              <label>Phone<input value={editForm.phone} onChange={e => setEditForm(f => ({ ...f, phone: e.target.value }))} /></label>
              <fieldset className={styles.roles}>
                <legend>Roles *</legend>
                {ROLES.map(r => (
                  <label key={r} className={styles.roleCheck}>
                    <input type="checkbox" checked={editForm.roles.includes(r)}
                      onChange={() => toggleRole(r, editForm.roles, roles => setEditForm(f => ({ ...f, roles })))} />
                    {r}
                  </label>
                ))}
              </fieldset>
              <div className={styles.modalActions}>
                <button type="submit" disabled={saving || editForm.roles.length === 0}>{saving ? 'Saving…' : 'Update'}</button>
                <button type="button" onClick={() => setEditUser(null)}>Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
