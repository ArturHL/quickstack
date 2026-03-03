import { http, HttpResponse } from 'msw'
import type { DailySummaryResponse } from '../../features/reports/types/Report'

const BASE = `${import.meta.env.VITE_API_BASE_URL}/api/v1`

const today = new Date().toISOString().split('T')[0]

export const mockDailySummary: DailySummaryResponse = {
  date: today,
  totalSales: 1250.50,
  orderCount: 18,
  averageTicket: 69.47,
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
        totalSales: 0,
        orderCount: 0,
        averageTicket: 0,
        topProducts: [],
      }
      return HttpResponse.json({ data: emptyResponse }, { status: 200 })
    }
    return HttpResponse.json({ data: mockDailySummary }, { status: 200 })
  }),
]
