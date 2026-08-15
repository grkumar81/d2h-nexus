import { useEffect, useState } from 'react'
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, Legend, ResponsiveContainer,
  PieChart, Pie, Cell,
} from 'recharts'
import { getDashboard } from '../../api/dashboard'
import type { Dashboard } from '../../types'
import styles from './Dashboard.module.css'

const MONTH_NAMES = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']
const PIE_COLORS = ['#4f8ef7','#f7a94f','#f74f4f','#4ff7a9','#a94ff7','#f7f74f','#4ff7f7']

const fmt = (n: number) =>
  n >= 10_000_000 ? `₹${(n / 10_000_000).toFixed(1)}Cr`
  : n >= 100_000 ? `₹${(n / 100_000).toFixed(1)}L`
  : `₹${n.toLocaleString('en-IN')}`

export default function DashboardPage() {
  const [data, setData] = useState<Dashboard | null>(null)
  const [fyYear, setFyYear] = useState<number | undefined>(undefined)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    setError(null)
    getDashboard(fyYear)
      .then((d) => setData(d))
      .catch(() => setError('Failed to load dashboard'))
      .finally(() => setLoading(false))
  }, [fyYear])

  const currentYear = new Date().getFullYear()
  const fyOptions = Array.from({ length: 5 }, (_, i) => currentYear - i)

  if (loading) return <div className={styles.state}>Loading dashboard…</div>
  if (error || !data) return <div className={styles.state}>{error ?? 'No data'}</div>

  const trendData = data.monthlyTrend.map((m) => ({
    name: `${MONTH_NAMES[m.month - 1]} ${m.year}`,
    'Box Sales': m.boxSales,
    'Received': m.received,
    'Recharge': m.recharge,
    'Outstanding': m.outstanding,
  }))

  const assetPieData = [
    { name: 'Available', value: data.availableAssets },
    { name: 'Allocated', value: data.allocatedAssets },
    { name: 'Sold', value: data.soldAssets },
    { name: 'Activated', value: data.activatedAssets },
    { name: 'Returned', value: data.returnedAssets },
    { name: 'Damaged', value: data.damagedAssets },
    { name: 'Lost', value: data.lostAssets },
  ].filter((d) => d.value > 0)

  return (
    <div className={styles.page}>
      {/* Header */}
      <div className={styles.header}>
        <h2>Dashboard</h2>
        <div className={styles.fySelector}>
          <label>Financial Year:</label>
          <select value={fyYear ?? ''} onChange={(e) => setFyYear(e.target.value ? Number(e.target.value) : undefined)}>
            <option value="">Current</option>
            {fyOptions.map((y) => (
              <option key={y} value={y}>FY {y}–{y + 1}</option>
            ))}
          </select>
          {data && <span className={styles.fyLabel}>FY {data.financialYearStart}–{data.financialYearEnd}</span>}
        </div>
      </div>

      {/* Financial KPIs */}
      <section>
        <h3 className={styles.sectionTitle}>Financial</h3>
        <div className={styles.kpiGrid}>
          <KpiCard label="Box Sales" value={fmt(data.totalBoxSales)} color="blue" />
          <KpiCard label="Received" value={fmt(data.totalReceived)} color="green" />
          <KpiCard label="Outstanding" value={fmt(data.totalOutstanding)} color="red" />
          <KpiCard label="Recharge" value={fmt(data.totalRecharge)} color="purple" />
          <KpiCard label="Transactions" value={data.transactionCount.toString()} color="gray" />
        </div>
      </section>

      {/* Asset KPIs */}
      <section>
        <h3 className={styles.sectionTitle}>Assets</h3>
        <div className={styles.kpiGrid}>
          <KpiCard label="Total" value={data.totalAssets.toString()} color="gray" />
          <KpiCard label="Available" value={data.availableAssets.toString()} color="green" />
          <KpiCard label="Allocated" value={data.allocatedAssets.toString()} color="blue" />
          <KpiCard label="Sold" value={data.soldAssets.toString()} color="orange" />
          <KpiCard label="Activated" value={data.activatedAssets.toString()} color="purple" />
          <KpiCard label="Returned" value={data.returnedAssets.toString()} color="gray" />
          <KpiCard label="Damaged" value={data.damagedAssets.toString()} color="red" />
          <KpiCard label="Lost" value={data.lostAssets.toString()} color="red" />
        </div>
      </section>

      {/* Retailer KPIs */}
      <section>
        <h3 className={styles.sectionTitle}>Retailers</h3>
        <div className={styles.kpiGrid}>
          <KpiCard label="Total" value={data.totalRetailers.toString()} color="gray" />
          <KpiCard label="Active" value={data.activeRetailers.toString()} color="green" />
          <KpiCard label="Inactive" value={data.inactiveRetailers.toString()} color="orange" />
          <KpiCard label="With Outstanding" value={data.retailersWithOutstanding.toString()} color="red" />
        </div>
      </section>

      {/* Charts row */}
      <div className={styles.chartsRow}>
        {/* Monthly trend */}
        <section className={styles.chartCard}>
          <h3 className={styles.sectionTitle}>Monthly Trend</h3>
          {trendData.length === 0 ? (
            <div className={styles.empty}>No data for this period</div>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={trendData} margin={{ top: 4, right: 8, left: 0, bottom: 4 }}>
                <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                <YAxis tick={{ fontSize: 11 }} tickFormatter={(v) => fmt(v)} />
                <Tooltip formatter={(v) => typeof v === 'number' ? fmt(v) : String(v)} />
                <Legend />
                <Bar dataKey="Box Sales" fill="#4f8ef7" />
                <Bar dataKey="Received" fill="#4ff7a9" />
                <Bar dataKey="Outstanding" fill="#f74f4f" />
              </BarChart>
            </ResponsiveContainer>
          )}
        </section>

        {/* Asset distribution */}
        <section className={styles.chartCard}>
          <h3 className={styles.sectionTitle}>Asset Distribution</h3>
          {assetPieData.length === 0 ? (
            <div className={styles.empty}>No assets</div>
          ) : (
            <ResponsiveContainer width="100%" height={260}>
              <PieChart>
                <Pie data={assetPieData} dataKey="value" nameKey="name" cx="50%" cy="50%"
                     outerRadius={90} label={({ name, percent }) => `${name} ${((percent ?? 0) * 100).toFixed(0)}%`}
                     labelLine={false}>
                  {assetPieData.map((_, i) => (
                    <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          )}
        </section>
      </div>

      {/* Top retailers row */}
      <div className={styles.chartsRow}>
        <section className={styles.chartCard}>
          <h3 className={styles.sectionTitle}>Top Retailers by Received</h3>
          <TopRetailerTable rows={data.topByReceived} label="Received" />
        </section>
        <section className={styles.chartCard}>
          <h3 className={styles.sectionTitle}>Top Retailers by Outstanding</h3>
          <TopRetailerTable rows={data.topByOutstanding} label="Outstanding" />
        </section>
      </div>
    </div>
  )
}

function KpiCard({ label, value, color }: { label: string; value: string; color: string }) {
  return (
    <div className={`${styles.kpiCard} ${styles[`kpi_${color}`]}`}>
      <div className={styles.kpiValue}>{value}</div>
      <div className={styles.kpiLabel}>{label}</div>
    </div>
  )
}

function TopRetailerTable({ rows, label }: { rows: { retailerCode: string; retailerName: string; amount: number }[]; label: string }) {
  if (rows.length === 0) return <div className={styles.empty}>No data</div>
  return (
    <table className={styles.table}>
      <thead>
        <tr><th>Code</th><th>Name</th><th>{label}</th></tr>
      </thead>
      <tbody>
        {rows.map((r) => (
          <tr key={r.retailerCode}>
            <td>{r.retailerCode}</td>
            <td>{r.retailerName}</td>
            <td>{fmt(r.amount)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
