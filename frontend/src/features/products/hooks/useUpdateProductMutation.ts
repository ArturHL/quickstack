import { useMutation, useQueryClient } from '@tanstack/react-query'
import { productApi } from '../api/productApi'
import type { ProductUpdateRequest } from '../types/Product'

export const useUpdateProductMutation = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: ProductUpdateRequest }) =>
      productApi.updateProduct(id, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] })
    },
  })
}
