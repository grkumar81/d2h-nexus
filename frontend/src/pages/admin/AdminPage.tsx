import { Navigate } from 'react-router-dom'

/**
 * AdminPage is superseded by UsersPage (/users).
 * Redirect to keep any existing bookmarks working.
 */
export default function AdminPage() {
  return <Navigate to="/users" replace />
}
