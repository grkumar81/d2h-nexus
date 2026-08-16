import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { useAuth } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/Layout'
import LoginPage from './pages/Login'
import RetailersPage from './pages/retailers/RetailersPage'
import AssetsPage from './pages/assets/AssetsPage'
import BoxSalesPage from './pages/boxsales/BoxSalesPage'
import FinancePage from './pages/finance/FinancePage'
import OutstandingPage from './pages/finance/OutstandingPage'
import RechargePage from './pages/recharge/RechargePage'
import NotificationsPage from './pages/notifications/NotificationsPage'
import DashboardPage from './pages/dashboard/DashboardPage'
import ReportsPage from './pages/reports/ReportsPage'
import UsersPage from './pages/users/UsersPage'
import ChangePasswordPage from './pages/profile/ChangePasswordPage'
import SubscriptionPage from './pages/subscription/SubscriptionPage'

function DefaultRedirect() {
  const { auth } = useAuth()
  const isPlatformAdmin = auth?.roles.includes('PLATFORM_ADMIN') ?? false
  return <Navigate to={isPlatformAdmin ? '/subscription' : '/dashboard'} replace />
}

function TenantRoute({ children }: { children: React.ReactNode }) {
  const { auth } = useAuth()
  if (auth?.roles.includes('PLATFORM_ADMIN')) return <Navigate to="/subscription" replace />
  return <>{children}</>
}

function PlatformRoute({ children }: { children: React.ReactNode }) {
  const { auth } = useAuth()
  if (!auth?.roles.includes('PLATFORM_ADMIN')) return <Navigate to="/dashboard" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route index element={<DefaultRedirect />} />
            {/* Tenant-only routes */}
            <Route path="dashboard"          element={<TenantRoute><DashboardPage /></TenantRoute>} />
            <Route path="retailers"          element={<TenantRoute><RetailersPage /></TenantRoute>} />
            <Route path="assets"             element={<TenantRoute><AssetsPage /></TenantRoute>} />
            <Route path="box-sales"          element={<TenantRoute><BoxSalesPage /></TenantRoute>} />
            <Route path="finance"            element={<TenantRoute><FinancePage /></TenantRoute>} />
            <Route path="finance/outstanding" element={<TenantRoute><OutstandingPage /></TenantRoute>} />
            <Route path="recharges"          element={<TenantRoute><RechargePage /></TenantRoute>} />
            <Route path="notifications"      element={<TenantRoute><NotificationsPage /></TenantRoute>} />
            <Route path="reports"            element={<TenantRoute><ReportsPage /></TenantRoute>} />
            <Route path="users"              element={<TenantRoute><UsersPage /></TenantRoute>} />
            {/* Platform-admin-only routes */}
            <Route path="subscription"       element={<PlatformRoute><SubscriptionPage /></PlatformRoute>} />
            {/* Shared routes */}
            <Route path="me/change-password" element={<ChangePasswordPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
