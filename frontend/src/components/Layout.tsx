import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useEffect, useState } from 'react'
import { getSubscriptionStatus } from '../api/tenantProfile'
import type { SubscriptionStatusDto } from '../types'
import styles from './Layout.module.css'

const TENANT_NAV = [
  { to: '/dashboard',           icon: '📊', label: 'Dashboard',       section: 'Overview' },
  { to: '/retailers',           icon: '🏪', label: 'Retailers',        section: 'Operations' },
  { to: '/assets',              icon: '📦', label: 'Assets',           section: 'Operations' },
  { to: '/box-sales',           icon: '🛒', label: 'Box Sales',        section: 'Operations' },
  { to: '/finance',             icon: '💰', label: 'Finance',          section: 'Finance' },
  { to: '/finance/outstanding', icon: '⚠️', label: 'Outstanding',      section: 'Finance' },
  { to: '/recharges',           icon: '🔋', label: 'Recharge',         section: 'Finance' },
  { to: '/reports',             icon: '📈', label: 'Reports',          section: 'Reports' },
  { to: '/users',               icon: '👥', label: 'Users',            section: 'Settings' },
  { to: '/notifications',       icon: '🔔', label: 'Notifications',    section: 'Settings' },
  { to: '/me/change-password',  icon: '🔑', label: 'Change Password',  section: 'Settings' },
]

const PLATFORM_NAV = [
  { to: '/subscription',        icon: '🏢', label: 'Tenants',          section: 'Platform' },
  { to: '/me/change-password',  icon: '🔑', label: 'Change Password',  section: 'Settings' },
]

export default function Layout() {
  const { auth, logout } = useAuth()
  const location = useLocation()
  const isPlatformAdmin = auth?.roles.includes('PLATFORM_ADMIN') ?? false
  const [subscription, setSubscription] = useState<SubscriptionStatusDto | null>(null)
  const [collapsed, setCollapsed] = useState(false)

  // On mobile start collapsed
  useEffect(() => {
    if (window.innerWidth < 768) setCollapsed(true)
  }, [])

  // Close sidebar on route change on mobile
  useEffect(() => {
    if (window.innerWidth < 768) setCollapsed(true)
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
      {/* Mobile overlay */}
      {!collapsed && <div className={styles.overlay} onClick={() => setCollapsed(true)} />}

      <aside className={`${styles.sidebar} ${collapsed ? styles.sidebarCollapsed : ''}`}>
        {/* Brand */}
        <div className={styles.brand}>
          <div className={styles.brandIcon}>📡</div>
          {!collapsed && <span className={styles.brandText}>D2H</span>}
        </div>

        {/* Nav */}
        <nav className={styles.navSection}>
          {sections.map(section => (
            <div key={section}>
              {!collapsed && <div className={styles.navLabel}>{section}</div>}
              {collapsed && <div className={styles.navDivider} />}
              {navItems.filter(n => n.section === section).map(n => (
                <NavLink
                  key={n.to}
                  to={n.to}
                  title={collapsed ? n.label : undefined}
                  className={({ isActive }) =>
                    `${styles.link} ${collapsed ? styles.linkCollapsed : ''} ${isActive ? styles.active : ''}`
                  }
                >
                  <span className={styles.linkIcon}>{n.icon}</span>
                  {!collapsed && <span className={styles.linkLabel}>{n.label}</span>}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        {!collapsed && <div className={styles.sidebarFooter}>v1.0</div>}
      </aside>

      <div className={styles.main}>
        <header className={styles.topbar}>
          <div className={styles.topbarLeft}>
            <button
              className={styles.hamburger}
              onClick={() => setCollapsed(v => !v)}
              aria-label="Toggle menu"
              title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            >
              {collapsed ? '☰' : '◀'}
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
