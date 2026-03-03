import { useMutation, useQueryClient } from '@tanstack/react-query'
import { categoryApi } from '../api/categoryApi'

export const useDeleteCategoryMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => categoryApi.deleteCategory(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['categories'] })
    },
  })
}
