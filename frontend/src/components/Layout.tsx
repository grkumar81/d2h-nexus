import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useEffect, useState } from 'react'
import { getSubscriptionStatus } from '../api/tenantProfile'
import type { SubscriptionStatusDto } from '../types'
import styles from './Layout.module.css'

const NAV = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/retailers', label: 'Retailers' },
  { to: '/assets', label: 'Assets' },
  { to: '/box-sales', label: 'Box Sales' },
  { to: '/finance', label: 'Finance' },
  { to: '/finance/outstanding', label: 'Outstanding' },
  { to: '/recharges', label: 'Recharge' },
  { to: '/notifications', label: 'Notifications' },
  { to: '/reports', label: 'Reports' },
  { to: '/users', label: 'Users' },
  { to: '/tenant/profile', label: 'Tenant Profile' },
  { to: '/subscription', label: 'Subscriptions' },
  { to: '/admin', label: 'Administration' },
  { to: '/audit', label: 'Audit Log' },
  { to: '/me/change-password', label: 'Change Password' },
]

export default function Layout() {
  const { auth, logout } = useAuth()
  const isPlatformAdmin = auth?.roles.includes('PLATFORM_ADMIN') ?? false
  const [subscription, setSubscription] = useState<SubscriptionStatusDto | null>(null)

  useEffect(() => {
    if (!isPlatformAdmin && auth?.tenantCode) {
      getSubscriptionStatus().then(setSubscription).catch(() => {})
    }
  }, [auth?.tenantCode, isPlatformAdmin])

  const banner = subscription && ['EXPIRY_WARNING', 'GRACE_PERIOD', 'EXPIRED'].includes(subscription.subscriptionStatus)
    ? subscription : null
  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar}>
        <div className={styles.brand}>D2H</div>
        <nav>
          {NAV.map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              className={({ isActive }) => `${styles.link} ${isActive ? styles.active : ''}`}
            >
              {n.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <div className={styles.main}>
        <header className={styles.topbar}>
          <span>{auth?.tenantCode} — {auth?.username}</span>
          <button onClick={logout}>Logout</button>
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
