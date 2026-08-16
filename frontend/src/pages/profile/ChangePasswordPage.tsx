import { useState } from 'react'
import { changePassword } from '../../api/users'
import styles from './Profile.module.css'

export default function ChangePasswordPage() {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirm: '' })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    if (form.newPassword !== form.confirm) {
      setError('New passwords do not match')
      return
    }
    setSaving(true)
    try {
      await changePassword(form.currentPassword, form.newPassword)
      setSuccess('Password changed successfully')
      setForm({ currentPassword: '', newPassword: '', confirm: '' })
    } catch (err: any) {
      setError(err.response?.data?.message ?? 'Failed to change password')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className={styles.page}>
      <div className={styles.header}><h2>Change Password</h2></div>
      {success && <p className={styles.success}>{success}</p>}
      {error && <p className={styles.error}>{error}</p>}
      <form onSubmit={handleSubmit} className={styles.form}>
        <label>Current Password *<input required type="password" value={form.currentPassword} onChange={e => setForm(f => ({ ...f, currentPassword: e.target.value }))} /></label>
        <label>New Password *<input required type="password" minLength={8} value={form.newPassword} onChange={e => setForm(f => ({ ...f, newPassword: e.target.value }))} /></label>
        <label>Confirm New Password *<input required type="password" value={form.confirm} onChange={e => setForm(f => ({ ...f, confirm: e.target.value }))} /></label>
        <div className={styles.actions}>
          <button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Change Password'}</button>
        </div>
      </form>
    </div>
  )
}
