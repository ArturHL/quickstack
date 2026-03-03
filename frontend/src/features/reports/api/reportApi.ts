import axiosInstance from '../../../utils/axiosInstance'
import type { DailySummaryResponse } from '../types/Report'

export const reportApi = {
  getDailySummary: (branchId: string, date?: string): Promise<DailySummaryResponse> =>
    axiosInstance
      .get<{ data: DailySummaryResponse }>('/api/v1/reports/daily-summary', {
        params: { branchId, ...(date ? { date } : {}) },
      })
      .then((r) => r.data.data),
}
