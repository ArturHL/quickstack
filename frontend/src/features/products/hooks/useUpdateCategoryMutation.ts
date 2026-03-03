import { useMutation, useQueryClient } from '@tanstack/react-query'
import { categoryApi, type CategoryUpdateRequest } from '../api/categoryApi'

export const useUpdateCategoryMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: CategoryUpdateRequest }) =>
      categoryApi.updateCategory(id, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories'] })
      queryClient.invalidateQueries({ queryKey: ['menu'] })
    },
  })
}
