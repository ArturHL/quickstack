import { useMutation, useQueryClient } from '@tanstack/react-query'
import { categoryApi, type CategoryCreateRequest } from '../api/categoryApi'

export const useCreateCategoryMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: CategoryCreateRequest) => categoryApi.createCategory(body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories'] })
      queryClient.invalidateQueries({ queryKey: ['menu'] })
    },
  })
}
