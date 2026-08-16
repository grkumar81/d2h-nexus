import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
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
import AdminPage from './pages/admin/AdminPage'
import AuditPage from './pages/audit/AuditPage'
import UsersPage from './pages/users/UsersPage'
import TenantProfilePage from './pages/profile/TenantProfilePage'
import ChangePasswordPage from './pages/profile/ChangePasswordPage'

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
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard" element={<DashboardPage />} />
            <Route path="retailers" element={<RetailersPage />} />
            <Route path="assets" element={<AssetsPage />} />
            <Route path="box-sales" element={<BoxSalesPage />} />
            <Route path="finance" element={<FinancePage />} />
            <Route path="finance/outstanding" element={<OutstandingPage />} />
            <Route path="recharges" element={<RechargePage />} />
            <Route path="notifications" element={<NotificationsPage />} />
            <Route path="reports" element={<ReportsPage />} />
            <Route path="admin" element={<AdminPage />} />
            <Route path="audit" element={<AuditPage />} />
            <Route path="users" element={<UsersPage />} />
            <Route path="tenant/profile" element={<TenantProfilePage />} />
            <Route path="me/change-password" element={<ChangePasswordPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
