import { useEffect, useState } from 'react'
import { useAuth } from '../../context/AuthContext'
import {
  listConfigs,
  saveConfig,
  deleteConfig,
  listDeliveries,
} from '../../api/notifications'
import type {
  NotificationConfig,
  NotificationDelivery,
  NotificationEventType,
  NotificationChannel,
  Page,
} from '../../types'
import styles from './Notifications.module.css'

const EVENT_TYPES: NotificationEventType[] = [
  'FINANCE_TRANSACTION_CREATED',
  'FINANCE_TRANSACTION_REVERSED',
  'FINANCE_TRANSACTION_ADJUSTED',
  'FINANCE_UPLOAD_COMPLETED',
  'RECHARGE_CREATED',
  'RECHARGE_REVERSED',
  'RECHARGE_UPLOAD_COMPLETED',
]

type Tab = 'config' | 'history'

export default function NotificationsPage() {
  const { auth } = useAuth()
  const isAdmin = auth?.roles.includes('ROLE_TENANT_ADMIN') ?? false
  const [tab, setTab] = useState<Tab>('config')

  return (
    <div className={styles.page}>
      <h2 className={styles.title}>Notifications</h2>
      <div className={styles.tabs}>
        <button
          className={`${styles.tab} ${tab === 'config' ? styles.activeTab : ''}`}
          onClick={() => setTab('config')}
        >
          Configuration
        </button>
        <button
          className={`${styles.tab} ${tab === 'history' ? styles.activeTab : ''}`}
          onClick={() => setTab('history')}
        >
          Delivery History
        </button>
      </div>
      {tab === 'config' && <ConfigTab isAdmin={isAdmin} />}
      {tab === 'history' && <HistoryTab />}
    </div>
  )
}

// ── Config Tab ────────────────────────────────────────────────────────────────

function ConfigTab({ isAdmin }: { isAdmin: boolean }) {
  const [configs, setConfigs] = useState<NotificationConfig[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [form, setForm] = useState({
    eventType: EVENT_TYPES[0],
    channel: 'EMAIL' as NotificationChannel,
    enabled: true,
    recipients: '',
  })
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const load = () => {
    setLoading(true)
    listConfigs()
      .then(setConfigs)
      .catch(() => setError('Failed to load configurations'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!form.recipients.trim()) {
      setFormError('Recipients are required')
      return
    }
    setSaving(true)
    setFormError(null)
    try {
      await saveConfig(form)
      load()
      setForm(f => ({ ...f, recipients: '' }))
    } catch {
      setFormError('Failed to save configuration')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: number) => {
    if (!confirm('Delete this notification configuration?')) return
    try {
      await deleteConfig(id)
      setConfigs(c => c.filter(x => x.id !== id))
    } catch {
      setError('Failed to delete configuration')
    }
  }

  if (loading) return <p className={styles.state}>Loading...</p>
  if (error) return <p className={styles.error}>{error}</p>

  return (
    <div>
      {isAdmin && (
        <form className={styles.form} onSubmit={handleSave}>
          <h3>Add / Update Configuration</h3>
          <div className={styles.formRow}>
            <label>Event Type</label>
            <select
              value={form.eventType}
              onChange={e => setForm(f => ({ ...f, eventType: e.target.value as NotificationEventType }))}
            >
              {EVENT_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
          <div className={styles.formRow}>
            <label>Channel</label>
            <select
              value={form.channel}
              onChange={e => setForm(f => ({ ...f, channel: e.target.value as NotificationChannel }))}
            >
              <option value="EMAIL">EMAIL</option>
              <option value="WHATSAPP">WHATSAPP</option>
            </select>
          </div>
          <div className={styles.formRow}>
            <label>Recipients</label>
            <input
              type="text"
              placeholder="Comma-separated emails or phone numbers"
              value={form.recipients}
              onChange={e => setForm(f => ({ ...f, recipients: e.target.value }))}
            />
          </div>
          <div className={styles.formRow}>
            <label>Enabled</label>
            <input
              type="checkbox"
              checked={form.enabled}
              onChange={e => setForm(f => ({ ...f, enabled: e.target.checked }))}
            />
          </div>
          {formError && <p className={styles.error}>{formError}</p>}
          <button type="submit" disabled={saving}>{saving ? 'Saving...' : 'Save'}</button>
        </form>
      )}

      {configs.length === 0 ? (
        <p className={styles.state}>No notification configurations found.</p>
      ) : (
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Event Type</th>
              <th>Channel</th>
              <th>Enabled</th>
              <th>Recipients</th>
              <th>Updated</th>
              {isAdmin && <th>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {configs.map(c => (
              <tr key={c.id}>
                <td>{c.eventType}</td>
                <td>{c.channel}</td>
                <td>{c.enabled ? '✓' : '✗'}</td>
                <td className={styles.recipients}>{c.recipients ?? '—'}</td>
                <td>{new Date(c.updatedAt).toLocaleString()}</td>
                {isAdmin && (
                  <td>
                    <button className={styles.deleteBtn} onClick={() => handleDelete(c.id)}>
                      Delete
                    </button>
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

// ── History Tab ───────────────────────────────────────────────────────────────

function HistoryTab() {
  const [page, setPage] = useState<Page<NotificationDelivery> | null>(null)
  const [currentPage, setCurrentPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    listDeliveries(currentPage, 20)
      .then(setPage)
      .catch(() => setError('Failed to load delivery history'))
      .finally(() => setLoading(false))
  }, [currentPage])

  if (loading) return <p className={styles.state}>Loading...</p>
  if (error) return <p className={styles.error}>{error}</p>
  if (!page || page.content.length === 0)
    return <p className={styles.state}>No delivery records found.</p>

  return (
    <div>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>ID</th>
            <th>Event Type</th>
            <th>Channel</th>
            <th>Recipient</th>
            <th>Status</th>
            <th>Attempts</th>
            <th>Sent At</th>
            <th>Error</th>
          </tr>
        </thead>
        <tbody>
          {page.content.map(d => (
            <tr key={d.id}>
              <td>{d.id}</td>
              <td>{d.eventType}</td>
              <td>{d.channel}</td>
              <td>{d.recipient}</td>
              <td className={statusClass(d.status, styles)}>{d.status}</td>
              <td>{d.attempts}</td>
              <td>{d.sentAt ? new Date(d.sentAt).toLocaleString() : '—'}</td>
              <td className={styles.errorCell}>{d.errorMessage ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className={styles.pagination}>
        <button disabled={currentPage === 0} onClick={() => setCurrentPage(p => p - 1)}>
          Previous
        </button>
        <span>Page {currentPage + 1} of {page.totalPages}</span>
        <button
          disabled={currentPage >= page.totalPages - 1}
          onClick={() => setCurrentPage(p => p + 1)}
        >
          Next
        </button>
      </div>
    </div>
  )
}

function statusClass(status: string, styles: Record<string, string>) {
  if (status === 'SENT') return styles.statusSent
  if (status === 'FAILED') return styles.statusFailed
  if (status === 'RETRYING') return styles.statusRetrying
  return ''
}
