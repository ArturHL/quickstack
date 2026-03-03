import axiosInstance from '../../../utils/axiosInstance'
import type { DailySummaryResponse } from '../types/Report'

export const reportApi = {
  getDailySummary: (date?: string): Promise<DailySummaryResponse> =>
    axiosInstance
      .get<{ data: DailySummaryResponse }>('/api/v1/reports/daily-summary', {
        params: date ? { date } : undefined,
      })
      .then((r) => r.data.data),
}
