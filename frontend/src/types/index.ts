// ── Auth ──────────────────────────────────────────────────────────────────────
export interface LoginRequest { username: string; password: string }
export interface LoginResponse { token: string; tenantCode: string; username: string; roles: string[] }

// ── Pagination ────────────────────────────────────────────────────────────────
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

// ── Retailer ──────────────────────────────────────────────────────────────────
export interface Retailer {
  id: number
  retailerCode: string
  retailerName: string
  mobile: string | null
  alternateMobile: string | null
  email: string | null
  address: string | null
  city: string | null
  state: string | null
  pinCode: string | null
  gstNumber: string | null
  panNumber: string | null
  status: string
  joiningDate: string | null
  createdAt: string
}
export interface RetailerRequest {
  retailerCode: string
  retailerName: string
  mobile?: string
  email?: string
  address?: string
  city?: string
  state?: string
  pinCode?: string
}

// ── Asset ─────────────────────────────────────────────────────────────────────
export type AssetStatus = 'AVAILABLE' | 'ALLOCATED' | 'SOLD' | 'ACTIVATED' | 'RETURNED' | 'DAMAGED' | 'LOST'
export interface Asset {
  id: number
  serialNumber: string
  boxNumber: string | null
  model: string | null
  manufacturer: string | null
  batch: string | null
  purchaseDate: string | null
  purchaseCost: number | null
  status: AssetStatus
  retailerId: number | null
  retailerCode: string | null
  retailerName: string | null
  taggingDate: string | null
  saleDate: string | null
  activationDate: string | null
  returnDate: string | null
  createdAt: string
}
export interface AssetRequest {
  serialNumber: string
  boxNumber?: string
  model?: string
  manufacturer?: string
  batch?: string
  purchaseDate?: string
  purchaseCost?: number
}

// ── Box Sale ──────────────────────────────────────────────────────────────────
export type PaymentStatus = 'PENDING' | 'PAID' | 'PARTIAL'
export interface SaleItem {
  id: number
  assetId: number | null
  serialNumber: string | null
  quantity: number
  unitPrice: number
  totalPrice: number
}
export interface BoxSale {
  id: number
  retailerId: number
  retailerCode: string
  retailerName: string
  transactionDate: string
  totalAmount: number
  paymentStatus: PaymentStatus
  reference: string | null
  remarks: string | null
  items: SaleItem[]
  createdAt: string
}
export interface BoxSaleRequest {
  retailerCode: string
  transactionDate: string
  items: { serialNumber?: string; quantity: number; unitPrice: number }[]
  remarks?: string
}

// ── Finance ───────────────────────────────────────────────────────────────────
export type TransactionType =
  | 'BOX_SALE' | 'PAYMENT_RECEIVED' | 'RECHARGE' | 'REFUND'
  | 'CREDIT' | 'DEBIT' | 'ADJUSTMENT' | 'REVERSAL' | 'OTHER'
export type TransactionStatus = 'POSTED' | 'PENDING' | 'REVERSED' | 'CANCELLED'
export type PaymentMethod = 'CASH' | 'BANK_TRANSFER' | 'CHEQUE' | 'UPI' | 'NEFT' | 'RTGS' | 'IMPS' | 'OTHER'
export type TransactionSource = 'MANUAL' | 'UPLOAD' | 'SYSTEM'

export interface FinancialTransaction {
  id: number
  retailerId: number
  retailerCode: string
  retailerName: string
  transactionType: TransactionType
  transactionStatus: TransactionStatus
  transactionDate: string
  amount: number
  paymentMethod: PaymentMethod | null
  reference: string
  paymentReference: string | null
  description: string | null
  remarks: string | null
  source: TransactionSource
  createdAt: string
}
export interface FinanceRequest {
  retailerId: number
  transactionType: TransactionType
  transactionDate: string
  amount: number
  paymentMethod?: PaymentMethod
  reference?: string
  paymentReference?: string
  description?: string
  remarks?: string
}
export interface AdjustRequest { amount: number; description: string }

export interface RetailerFinanceSummary {
  retailerId: number
  retailerCode: string
  retailerName: string
  totalBoxSales: number
  totalDue: number
  totalReceived: number
  outstanding: number
  totalRecharge: number
}
export interface FinanceSummary {
  totalDue: number
  totalReceived: number
  outstanding: number
  totalRecharge: number
  transactionCount: number
}

// ── Recharge ──────────────────────────────────────────────────────────────────
export type RechargeType = 'REGULAR' | 'MONTHLY' | 'QUARTERLY' | 'ANNUAL' | 'PROMOTIONAL' | 'MANUAL' | 'OTHER'
export type RechargeStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'REVERSED' | 'CANCELLED'
export type RechargeSource = 'MANUAL' | 'UPLOAD' | 'SYSTEM'

export interface RechargeTransaction {
  id: number
  retailerId: number
  retailerCode: string
  retailerName: string
  assetId: number | null
  assetSerial: string | null
  reference: string
  externalReference: string | null
  rechargeDate: string
  amount: number
  rechargeType: RechargeType
  rechargeStatus: RechargeStatus
  paymentMethod: PaymentMethod | null
  paymentReference: string | null
  servicePeriod: string | null
  description: string | null
  source: RechargeSource
  reversedById: number | null
  reversalOfId: number | null
  createdBy: string | null
  createdAt: string
}
export interface CreateRechargeRequest {
  retailerId: number
  assetId?: number
  rechargeDate: string
  amount: number
  rechargeType: RechargeType
  paymentMethod?: PaymentMethod
  paymentReference?: string
  externalReference?: string
  servicePeriod?: string
  description?: string
  remarks?: string
  reference?: string
}
export interface RechargeSummary {
  totalCount: number
  totalAmount: number
  successAmount: number
  failedAmount: number
  reversedAmount: number
  dateFrom: string | null
  dateTo: string | null
}
export interface RetailerRechargeSummary {
  retailerId: number
  retailerCode: string
  retailerName: string
  rechargeCount: number
  totalRecharge: number
  successRecharge: number
  lastRechargeDate: string | null
  lastRechargeAmount: number | null
}

// ── Notification ─────────────────────────────────────────────────────────────
export type NotificationEventType =
  | 'FINANCE_TRANSACTION_CREATED' | 'FINANCE_TRANSACTION_REVERSED' | 'FINANCE_TRANSACTION_ADJUSTED'
  | 'FINANCE_UPLOAD_COMPLETED' | 'RECHARGE_CREATED' | 'RECHARGE_REVERSED' | 'RECHARGE_UPLOAD_COMPLETED'
export type NotificationChannel = 'EMAIL' | 'WHATSAPP'
export type NotificationStatus = 'PENDING' | 'PROCESSING' | 'SENT' | 'FAILED' | 'RETRYING' | 'CANCELLED'

export interface NotificationConfig {
  id: number
  eventType: NotificationEventType
  channel: NotificationChannel
  enabled: boolean
  recipients: string | null
  updatedAt: string
}
export interface SaveNotificationConfigRequest {
  eventType: NotificationEventType
  channel: NotificationChannel
  enabled: boolean
  recipients: string
}
export interface NotificationDelivery {
  id: number
  outboxEventId: number
  eventType: string
  channel: NotificationChannel
  recipient: string
  status: NotificationStatus
  attempts: number
  sentAt: string | null
  errorMessage: string | null
  createdAt: string
}

// ── Dashboard ────────────────────────────────────────────────────────────────
export interface MonthlyTrend {
  year: number
  month: number
  boxSales: number
  received: number
  recharge: number
  outstanding: number
}
export interface TopRetailer {
  retailerId: number
  retailerCode: string
  retailerName: string
  amount: number
}
export interface Dashboard {
  totalBoxSales: number
  totalReceived: number
  totalOutstanding: number
  totalRecharge: number
  transactionCount: number
  totalAssets: number
  availableAssets: number
  allocatedAssets: number
  soldAssets: number
  activatedAssets: number
  returnedAssets: number
  damagedAssets: number
  lostAssets: number
  totalRetailers: number
  activeRetailers: number
  inactiveRetailers: number
  retailersWithOutstanding: number
  monthlyTrend: MonthlyTrend[]
  topByReceived: TopRetailer[]
  topByOutstanding: TopRetailer[]
  financialYearStart: number
  financialYearEnd: number
}

// ── Reports ──────────────────────────────────────────────────────────────────
export interface RetailerReport {
  retailerId: number
  retailerCode: string
  retailerName: string
  boxSales: number
  received: number
  outstanding: number
  recharge: number
}
export interface PeriodReport {
  dateFrom: string | null
  dateTo: string | null
  boxSales: number
  received: number
  outstanding: number
  recharge: number
  transactionCount: number
}

export interface UploadResult {
  totalRows: number
  successCount: number
  failureCount: number
  errors: string[]
  totalAmountProcessed?: number
}

// ── User Management ───────────────────────────────────────────────────────────────────
export interface UserDto {
  id: number
  username: string
  email: string
  fullName: string | null
  phone: string | null
  status: string
  roles: string[]
  createdAt: string
}
export interface CreateUserRequest {
  username: string
  email: string
  password: string
  fullName?: string
  phone?: string
  roles: string[]
}
export interface UpdateUserRequest {
  fullName?: string
  phone?: string
  roles: string[]
}

// ── Tenant Profile ────────────────────────────────────────────────────────────
export interface TenantProfile {
  tenantCode: string
  name: string
  email: string | null
  phone: string | null
  status: string
}
export interface UpdateTenantProfileRequest {
  name: string
  email?: string
  phone?: string
}

// ── Subscription ──────────────────────────────────────────────────────────────
export type SubscriptionStatus = 'ACTIVE' | 'ACTIVE_WITH_EXPIRY' | 'EXPIRY_WARNING' | 'GRACE_PERIOD' | 'EXPIRED'
export interface SubscriptionStatusDto {
  subscriptionStatus: SubscriptionStatus
  subscriptionExpiry: string | null
  daysUntilExpiry: number
  gracePeriodDays: number
  graceDaysRemaining: number
}
export interface PlatformTenantDto {
  id: number
  tenantCode: string
  name: string
  email: string | null
  phone: string | null
  schemaName: string
  status: string
  subscriptionStatus: SubscriptionStatus
  subscriptionExpiry: string | null
  daysUntilExpiry: number
  gracePeriodDays: number
  graceDaysRemaining: number
  createdAt: string
  updatedAt: string
}

// ── Audit ───────────────────────────────────────────────────────────────────────────
export interface AuditLog {
  id: number
  entityType: string
  entityId: string
  action: string
  performedBy: string
  details: string | null
  ipAddress: string | null
  createdAt: string
}
