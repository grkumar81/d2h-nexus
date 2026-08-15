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
            <Route index element={<Navigate to="/finance/outstanding" replace />} />
            <Route path="retailers" element={<RetailersPage />} />
            <Route path="assets" element={<AssetsPage />} />
            <Route path="box-sales" element={<BoxSalesPage />} />
            <Route path="finance" element={<FinancePage />} />
            <Route path="finance/outstanding" element={<OutstandingPage />} />
            <Route path="recharges" element={<RechargePage />} />
            <Route path="notifications" element={<NotificationsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
