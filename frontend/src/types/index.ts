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
  name: string
  contactPerson: string
  phone: string
  email: string
  address: string
  city: string
  state: string
  pincode: string
  active: boolean
  createdAt: string
}
export interface RetailerRequest {
  retailerCode: string
  name: string
  contactPerson?: string
  phone?: string
  email?: string
  address?: string
  city?: string
  state?: string
  pincode?: string
}

// ── Asset ─────────────────────────────────────────────────────────────────────
export type AssetStatus = 'AVAILABLE' | 'ASSIGNED' | 'FAULTY' | 'RETIRED'
export interface Asset {
  id: number
  serialNumber: string
  modelNumber: string
  assetType: string
  status: AssetStatus
  retailerId: number | null
  retailerCode: string | null
  retailerName: string | null
  purchaseDate: string | null
  warrantyExpiry: string | null
  remarks: string | null
  createdAt: string
}
export interface AssetRequest {
  serialNumber: string
  modelNumber: string
  assetType: string
  status?: AssetStatus
  purchaseDate?: string
  warrantyExpiry?: string
  remarks?: string
}

// ── Box Sale ──────────────────────────────────────────────────────────────────
export type SaleStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED'
export interface BoxSale {
  id: number
  retailerCode: string
  retailerName: string
  saleDate: string
  quantity: number
  unitPrice: number
  totalAmount: number
  status: SaleStatus
  invoiceNumber: string | null
  remarks: string | null
  createdAt: string
}
export interface BoxSaleRequest {
  retailerCode: string
  saleDate: string
  quantity: number
  unitPrice: number
  invoiceNumber?: string
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
  source: TransactionSource
  createdAt: string
}
export interface FinanceRequest {
  retailerCode: string
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

export interface UploadResult {
  totalRows: number
  successCount: number
  failureCount: number
  errors: string[]
  totalAmountProcessed?: number
}
