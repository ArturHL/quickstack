import { http, HttpResponse } from 'msw'
import type { DailySummaryResponse } from '../../features/reports/types/Report'

const BASE = `${import.meta.env.VITE_API_BASE_URL}/api/v1`

function localDateISO(): string {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}
const today = localDateISO()

export const mockDailySummary: DailySummaryResponse = {
  date: today,
  branchId: 'branch-1',
  totalOrders: 18,
  totalSales: 1250.50,
  averageTicket: 69.47,
  ordersByServiceType: { COUNTER: 10, TAKEOUT: 8 },
  topProducts: [
    { productName: 'Café Americano', quantitySold: 12 },
    { productName: 'Sandwich Club', quantitySold: 8 },
    { productName: 'Café con Leche', quantitySold: 5 },
  ],
}

export const reportHandlers = [
  http.get(`${BASE}/reports/daily-summary`, ({ request }) => {
    const url = new URL(request.url)
    const date = url.searchParams.get('date')
    if (date && date !== today) {
      const emptyResponse: DailySummaryResponse = {
        date,
        branchId: 'branch-1',
        totalOrders: 0,
        totalSales: 0,
        averageTicket: 0,
        ordersByServiceType: {},
        topProducts: [],
      }
      return HttpResponse.json({ data: emptyResponse }, { status: 200 })
    }
    return HttpResponse.json({ data: mockDailySummary }, { status: 200 })
  }),
]
