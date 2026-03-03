import { useQuery } from '@tanstack/react-query'
import { reportApi } from '../api/reportApi'

export const useDailySummaryQuery = (date?: string) => {
  return useQuery({
    queryKey: ['reports', 'daily-summary', date ?? 'today'],
    queryFn: () => reportApi.getDailySummary(date),
    staleTime: 2 * 60 * 1000,
  })
}
