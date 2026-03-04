import { useQuery } from '@tanstack/react-query'
import { userAdminApi } from '../api/userApi'

export const useUsersAdminQuery = (params: { search?: string; page?: number; size?: number } = {}) => {
  return useQuery({
    queryKey: ['users-admin', params],
    queryFn: () => userAdminApi.getUsers(params),
    staleTime: 2 * 60 * 1000,
  })
}
