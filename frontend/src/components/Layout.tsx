import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useEffect, useState } from 'react'
import { getSubscriptionStatus } from '../api/tenantProfile'
import type { SubscriptionStatusDto } from '../types'
import styles from './Layout.module.css'

const TENANT_NAV = [
  { to: '/dashboard',           label: '📊 Dashboard',        section: 'Overview' },
  { to: '/retailers',           label: '🏪 Retailers',         section: 'Operations' },
  { to: '/assets',              label: '📦 Assets',            section: 'Operations' },
  { to: '/box-sales',           label: '🛒 Box Sales',         section: 'Operations' },
  { to: '/finance',             label: '💰 Finance',           section: 'Finance' },
  { to: '/finance/outstanding', label: '⚠️ Outstanding',       section: 'Finance' },
  { to: '/recharges',           label: '🔋 Recharge',          section: 'Finance' },
  { to: '/reports',             label: '📈 Reports',           section: 'Reports' },
  { to: '/users',               label: '👥 Users',             section: 'Settings' },
  { to: '/notifications',       label: '🔔 Notifications',     section: 'Settings' },
  { to: '/me/change-password',  label: '🔑 Change Password',   section: 'Settings' },
]

const PLATFORM_NAV = [
  { to: '/subscription',        label: '🏢 Tenants',           section: 'Platform' },
  { to: '/me/change-password',  label: '🔑 Change Password',   section: 'Settings' },
]

export default function Layout() {
  const { auth, logout } = useAuth()
  const location = useLocation()
  const isPlatformAdmin = auth?.roles.includes('PLATFORM_ADMIN') ?? false
  const [subscription, setSubscription] = useState<SubscriptionStatusDto | null>(null)
  const [sidebarOpen, setSidebarOpen] = useState(true)

  // Close sidebar on route change (mobile UX)
  useEffect(() => {
    if (window.innerWidth < 768) setSidebarOpen(false)
  }, [location.pathname])

  useEffect(() => {
    if (!isPlatformAdmin && auth?.tenantCode) {
      getSubscriptionStatus().then(setSubscription).catch(() => {})
    }
  }, [auth?.tenantCode, isPlatformAdmin])

  const banner = subscription && ['EXPIRY_WARNING', 'GRACE_PERIOD', 'EXPIRED'].includes(subscription.subscriptionStatus)
    ? subscription : null

  const navItems = isPlatformAdmin ? PLATFORM_NAV : TENANT_NAV
  const sections = [...new Set(navItems.map(n => n.section))]
  const initials = (auth?.username ?? '?').slice(0, 2).toUpperCase()

  return (
    <div className={styles.shell}>
      {/* Overlay backdrop — visible on mobile when sidebar is open */}
      {sidebarOpen && (
        <div className={styles.overlay} onClick={() => setSidebarOpen(false)} />
      )}

      <aside className={`${styles.sidebar} ${sidebarOpen ? styles.sidebarOpen : styles.sidebarClosed}`}>
        <div className={styles.brand}>
          <div className={styles.brandIcon}>📡</div>
          <span className={styles.brandText}>D2H</span>
          {/* Close button inside sidebar (visible on mobile) */}
          <button
            className={styles.sidebarCloseBtn}
            onClick={() => setSidebarOpen(false)}
            aria-label="Close menu"
          >
            ✕
          </button>
        </div>
        <nav className={styles.navSection}>
          {sections.map(section => (
            <div key={section}>
              <div className={styles.navLabel}>{section}</div>
              {navItems.filter(n => n.section === section).map(n => (
                <NavLink
                  key={n.to}
                  to={n.to}
                  className={({ isActive }) => `${styles.link} ${isActive ? styles.active : ''}`}
                >
                  {n.label}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>
        <div className={styles.sidebarFooter}>v1.0</div>
      </aside>

      <div className={styles.main}>
        <header className={styles.topbar}>
          <div className={styles.topbarLeft}>
            {/* Hamburger button */}
            <button
              className={styles.hamburger}
              onClick={() => setSidebarOpen(v => !v)}
              aria-label="Toggle menu"
              title={sidebarOpen ? 'Collapse sidebar' : 'Expand sidebar'}
            >
              {sidebarOpen ? '◀' : '☰'}
            </button>
            {isPlatformAdmin
              ? <span className={styles.topbarBadge}>Platform Admin</span>
              : <><span>Tenant:</span><span className={styles.topbarBadge}>{auth?.tenantCode}</span></>
            }
          </div>
          <div className={styles.topbarRight}>
            <div className={styles.topbarUser}>
              <div className={styles.topbarAvatar}>{initials}</div>
              <span>{auth?.username}</span>
            </div>
            <button className={styles.logoutBtn} onClick={logout}>Logout</button>
          </div>
        </header>

        {banner && (
          <div className={banner.subscriptionStatus === 'EXPIRED' ? styles.bannerError : styles.bannerWarn}>
            {banner.subscriptionStatus === 'EXPIRY_WARNING' && `⚠️ Subscription expires in ${banner.daysUntilExpiry} day(s). Please renew.`}
            {banner.subscriptionStatus === 'GRACE_PERIOD' && `🟠 Subscription expired. Grace period: ${banner.graceDaysRemaining} day(s) remaining.`}
            {banner.subscriptionStatus === 'EXPIRED' && '🔴 Subscription expired. Access is restricted. Contact your administrator.'}
          </div>
        )}

        <main className={styles.content}>
          <Outlet />
        </main>
      </div>
    </div>
  )
}
