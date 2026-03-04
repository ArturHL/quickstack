import { useMutation, useQueryClient } from '@tanstack/react-query'
import { userAdminApi, type UserUpdateRequest } from '../api/userApi'

export const useUpdateUserMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: UserUpdateRequest }) =>
      userAdminApi.updateUser(id, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users-admin'] })
    },
  })
}
