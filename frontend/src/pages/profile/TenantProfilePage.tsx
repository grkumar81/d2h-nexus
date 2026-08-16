import { useEffect, useState } from 'react'
import { getTenantProfile, updateTenantProfile } from '../../api/tenantProfile'
import { useAuth } from '../../context/AuthContext'
import type { TenantProfile, UpdateTenantProfileRequest } from '../../types'
import styles from './Profile.module.css'

export default function TenantProfilePage() {
  const { auth } = useAuth()
  const isAdmin = auth?.roles.includes('TENANT_ADMIN') ?? false

  const [profile, setProfile] = useState<TenantProfile | null>(null)
  const [form, setForm] = useState<UpdateTenantProfileRequest>({ name: '', email: '', phone: '' })
  const [editing, setEditing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    getTenantProfile().then(p => {
      setProfile(p)
      setForm({ name: p.name, email: p.email ?? '', phone: p.phone ?? '' })
    })
  }, [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    setSaving(true)
    try {
      const updated = await updateTenantProfile(form)
      setProfile(updated)
      setEditing(false)
      setSuccess('Profile updated successfully')
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to update profile')
    } finally {
      setSaving(false)
    }
  }

  if (!profile) return <p>Loading…</p>

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <h2>Tenant Profile</h2>
        {isAdmin && !editing && (
          <button className={styles.editBtn} onClick={() => { setEditing(true); setSuccess('') }}>Edit</button>
        )}
      </div>

      {success && <p className={styles.success}>{success}</p>}
      {error && <p className={styles.error}>{error}</p>}

      {!editing ? (
        <div className={styles.card}>
          <div className={styles.row}><span>Tenant Code</span><strong>{profile.tenantCode}</strong></div>
          <div className={styles.row}><span>Name</span><strong>{profile.name}</strong></div>
          <div className={styles.row}><span>Email</span><strong>{profile.email ?? '—'}</strong></div>
          <div className={styles.row}><span>Phone</span><strong>{profile.phone ?? '—'}</strong></div>
          <div className={styles.row}><span>Status</span><strong>{profile.status}</strong></div>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className={styles.form}>
          <label>Name *<input required value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} /></label>
          <label>Email<input type="email" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} /></label>
          <label>Phone<input value={form.phone} onChange={e => setForm(f => ({ ...f, phone: e.target.value }))} /></label>
          <div className={styles.actions}>
            <button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save'}</button>
            <button type="button" onClick={() => { setEditing(false); setError('') }}>Cancel</button>
          </div>
        </form>
      )}
    </div>
  )
}
