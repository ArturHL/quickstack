import { useMutation, useQueryClient } from '@tanstack/react-query'
import { productApi } from '../api/productApi'
import type { ProductCreateRequest } from '../types/Product'

export const useCreateProductMutation = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (body: ProductCreateRequest) => productApi.createProduct(body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] })
    },
  })
}
