import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
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
]

export default function Layout() {
  const { auth, logout } = useAuth()
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
        <main className={styles.content}>
          <Outlet />
        </main>
      </div>
    </div>
  )
}
