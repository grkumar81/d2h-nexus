import { useState, useCallback, useRef } from 'react'
import { AgGridReact } from 'ag-grid-react'
import type { ColDef, GridReadyEvent } from 'ag-grid-community'
import { listUsers, createUser, activateUser, deactivateUser } from '../../api/admin'
import type { UserDto, CreateUserRequest } from '../../types'
import { useAuth } from '../../context/AuthContext'
import styles from './Admin.module.css'

const ROLES = ['TENANT_ADMIN', 'FINANCE_USER', 'OPERATIONS_USER', 'READ_ONLY']

const defaultForm: CreateUserRequest = {
  username: '', email: '', password: '', fullName: '', roles: ['READ_ONLY']
}

export default function AdminPage() {
  const { auth } = useAuth()
  const isAdmin = auth?.roles.includes('TENANT_ADMIN')

  const [rows, setRows] = useState<UserDto[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [showModal, setShowModal] = useState(false)
  const [form, setForm] = useState<CreateUserRequest>(defaultForm)
  const [formError, setFormError] = useState<string | null>(null)
  const pageRef = useRef(0)

  const load = useCallback(async (page = 0) => {
    setLoading(true)
    setError(null)
    try {
      const data = await listUsers(page, 20)
      setRows(data.content)
      setTotal(data.totalElements)
      pageRef.current = page
    } catch {
      setError('Failed to load users')
    } finally {
      setLoading(false)
    }
  }, [])

  const onGridReady = useCallback((_e: GridReadyEvent) => { load(0) }, [load])

  const handleStatusToggle = async (user: UserDto) => {
    try {
      if (user.status === 'ACTIVE') await deactivateUser(user.id)
      else await activateUser(user.id)
      load(pageRef.current)
    } catch {
      setError('Failed to update user status')
    }
  }

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)
    if (!form.username || !form.email || !form.password) {
      setFormError('Username, email and password are required')
      return
    }
    if (form.roles.length === 0) {
      setFormError('At least one role is required')
      return
    }
    try {
      await createUser(form)
      setShowModal(false)
      setForm(defaultForm)
      load(0)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })
        ?.response?.data?.message ?? 'Failed to create user'
      setFormError(msg)
    }
  }

  const colDefs: ColDef<UserDto>[] = [
    { field: 'username', headerName: 'Username', flex: 1 },
    { field: 'email', headerName: 'Email', flex: 2 },
    { field: 'fullName', headerName: 'Full Name', flex: 1 },
    { field: 'roles', headerName: 'Roles', flex: 1,
      valueFormatter: p => (p.value as string[]).join(', ') },
    { field: 'status', headerName: 'Status', width: 110 },
    { headerName: 'Actions', width: 130, cellRenderer: (p: { data: UserDto }) => (
      <button
        className={p.data.status === 'ACTIVE' ? styles.btnDeactivate : styles.btnActivate}
        onClick={() => handleStatusToggle(p.data)}
      >
        {p.data.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
      </button>
    )},
  ]

  if (!isAdmin) return <p className={styles.denied}>Access denied — TENANT_ADMIN role required.</p>

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <h2>User Management</h2>
        <button className={styles.btnPrimary} onClick={() => setShowModal(true)}>+ New User</button>
      </div>

      {error && <p className={styles.error}>{error}</p>}
      <p className={styles.count}>{total} user(s)</p>

      <div className="ag-theme-alpine" style={{ height: 420 }}>
        <AgGridReact<UserDto>
          rowData={rows}
          columnDefs={colDefs}
          loading={loading}
          onGridReady={onGridReady}
          suppressCellFocus
        />
      </div>

      {showModal && (
        <div className={styles.overlay}>
          <div className={styles.modal}>
            <h3>Create User</h3>
            {formError && <p className={styles.error}>{formError}</p>}
            <form onSubmit={handleCreate} className={styles.form}>
              <label>Username *
                <input value={form.username} onChange={e => setForm(f => ({ ...f, username: e.target.value }))} />
              </label>
              <label>Email *
                <input type="email" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} />
              </label>
              <label>Password *
                <input type="password" value={form.password} onChange={e => setForm(f => ({ ...f, password: e.target.value }))} />
              </label>
              <label>Full Name
                <input value={form.fullName ?? ''} onChange={e => setForm(f => ({ ...f, fullName: e.target.value }))} />
              </label>
              <label>Roles *
                <div className={styles.roles}>
                  {ROLES.map(r => (
                    <label key={r} className={styles.roleCheck}>
                      <input
                        type="checkbox"
                        checked={form.roles.includes(r)}
                        onChange={e => setForm(f => ({
                          ...f,
                          roles: e.target.checked ? [...f.roles, r] : f.roles.filter(x => x !== r)
                        }))}
                      />
                      {r}
                    </label>
                  ))}
                </div>
              </label>
              <div className={styles.actions}>
                <button type="submit" className={styles.btnPrimary}>Create</button>
                <button type="button" onClick={() => { setShowModal(false); setForm(defaultForm); setFormError(null) }}>
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
