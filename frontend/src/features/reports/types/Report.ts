export interface TopProductEntry {
  productName: string
  quantitySold: number
}

export interface DailySummaryResponse {
  date: string
  totalSales: number
  orderCount: number
  averageTicket: number
  topProducts: TopProductEntry[]
}
