import { useMutation, useQueryClient } from '@tanstack/react-query'
import { userAdminApi, type UserCreateRequest } from '../api/userApi'

export const useCreateUserMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: UserCreateRequest) => userAdminApi.createUser(body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users-admin'] })
    },
  })
}
