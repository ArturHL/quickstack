import { useQuery } from '@tanstack/react-query'
import { reportApi } from '../api/reportApi'

export const useDailySummaryQuery = (branchId: string | null, date?: string) => {
  return useQuery({
    queryKey: ['reports', 'daily-summary', branchId, date ?? 'today'],
    queryFn: () => reportApi.getDailySummary(branchId!, date),
    enabled: !!branchId,
    staleTime: 2 * 60 * 1000,
  })
}
