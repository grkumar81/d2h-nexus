import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import styles from './Login.module.css'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    if (!username || !password) { setError('Username and password are required.'); return }
    setLoading(true); setError('')
    try {
      await login({ username, password })
      navigate('/', { replace: true })
    } catch {
      setError('Invalid credentials. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.page}>
      {/* Hero panel */}
      <div className={styles.hero}>
        <div className={styles.heroLogo}>
          <div className={styles.heroLogoIcon}>📡</div>
          <span className={styles.heroLogoText}>D2H</span>
        </div>
        <h1 className={styles.heroTitle}>
          Distributor<br /><span>Management</span><br />Platform
        </h1>
        <p className={styles.heroSub}>
          Complete operations management for D2H distributors — retailers, assets, finance, and more.
        </p>
        <div className={styles.heroFeatures}>
          {['Multi-tenant & role-based access', 'Real-time financial tracking', 'Asset lifecycle management', 'Bulk upload & reporting'].map(f => (
            <div key={f} className={styles.heroFeature}>
              <span className={styles.heroFeatureDot} />
              {f}
            </div>
          ))}
        </div>
      </div>

      {/* Form panel */}
      <div className={styles.formPanel}>
        <div className={styles.formHeader}>
          <h2 className={styles.formTitle}>Welcome back</h2>
          <p className={styles.formSub}>Sign in to your account to continue</p>
        </div>

        <form className={styles.form} onSubmit={submit}>
          {error && <div className={styles.error}>⚠ {error}</div>}

          <div className={styles.fieldGroup}>
            <label className={styles.fieldLabel}>Username</label>
            <input
              className={styles.fieldInput}
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoFocus
              autoComplete="username"
            />
          </div>

          <div className={styles.fieldGroup}>
            <label className={styles.fieldLabel}>Password</label>
            <div className={styles.passwordWrapper}>
              <input
                className={styles.fieldInput}
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
              />
              <button
                type="button"
                className={styles.togglePassword}
                onClick={() => setShowPassword(v => !v)}
                tabIndex={-1}
              >
                {showPassword ? '🙈' : '👁️'}
              </button>
            </div>
          </div>

          <button type="submit" className={styles.submitBtn} disabled={loading}>
            {loading ? 'Signing in…' : 'Sign in →'}
          </button>
        </form>

        <div className={styles.formFooter}>
          D2H Distributor Management Platform · v1.0
        </div>
      </div>
    </div>
  )
}
