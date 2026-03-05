import { useMutation, useQueryClient } from '@tanstack/react-query'
import { categoryApi, type CategoryUpdateRequest } from '../api/categoryApi'
import type { CategoryResponse } from '../types/Product'

export const useUpdateCategoryMutation = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: CategoryUpdateRequest }) =>
      categoryApi.updateCategory(id, body),
    onSuccess: (updated) => {
      // Optimistic: patch cache immediately so UI reflects changes before full refetch
      queryClient.setQueryData<CategoryResponse[]>(['categories'], (old) =>
        old ? old.map((c) => (c.id === updated.id ? updated : c)) : old
      )
      queryClient.invalidateQueries({ queryKey: ['categories'] })
      queryClient.invalidateQueries({ queryKey: ['menu'] })
    },
  })
}
