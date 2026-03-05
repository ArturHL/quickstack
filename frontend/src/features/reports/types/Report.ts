export interface TopProductEntry {
  productName: string
  quantitySold: number
}

export interface DailySummaryResponse {
  date: string
  branchId: string
  totalOrders: number
  totalSales: number
  averageTicket: number
  ordersByServiceType: Record<string, number>
  topProducts: TopProductEntry[]
}
