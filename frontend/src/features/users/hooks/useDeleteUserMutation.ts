import { useMutation, useQueryClient } from '@tanstack/react-query'
import { userAdminApi } from '../api/userApi'

export const useDeleteUserMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => userAdminApi.deleteUser(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users-admin'] })
    },
  })
}
