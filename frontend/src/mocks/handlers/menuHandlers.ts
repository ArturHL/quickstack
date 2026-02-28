import { http, HttpResponse } from 'msw'
import type { MenuResponse } from '../../features/pos/types/Menu'

const BASE = `${import.meta.env.VITE_API_BASE_URL}/api/v1`

export const mockMenuResponse: MenuResponse = {
  categories: [
    {
      id: 'cat-1',
      name: 'Bebidas',
      sortOrder: 1,
      imageUrl: null,
      products: [
        {
          id: 'prod-1',
          name: 'Café Americano',
          basePrice: 35.0,
          imageUrl: null,
          isAvailable: true,
          productType: 'SIMPLE',
          variants: [],
          modifierGroups: [],
        },
        {
          id: 'prod-2',
          name: 'Cappuccino',
          basePrice: 45.0,
          imageUrl: null,
          isAvailable: false,
          productType: 'SIMPLE',
          variants: [],
          modifierGroups: [],
        },
      ],
    },
    {
      id: 'cat-2',
      name: 'Alimentos',
      sortOrder: 2,
      imageUrl: null,
      products: [
        {
          id: 'prod-3',
          name: 'Sandwich',
          basePrice: 85.0,
          imageUrl: 'https://example.com/sandwich.jpg',
          isAvailable: true,
          productType: 'VARIANT',
          variants: [
            {
              id: 'var-1',
              name: 'Chico',
              priceAdjustment: 0,
              effectivePrice: 85.0,
              isDefault: true,
              sortOrder: 1,
            },
            {
              id: 'var-2',
              name: 'Grande',
              priceAdjustment: 20.0,
              effectivePrice: 105.0,
              isDefault: false,
              sortOrder: 2,
            },
          ],
          modifierGroups: [],
        },
      ],
    },
  ],
  combos: [],
}

export const menuHandlers = [
  http.get(`${BASE}/menu`, () =>
    HttpResponse.json({ data: mockMenuResponse }, { status: 200 })
  ),
]

export const menuErrorHandlers = {
  serverError: () =>
    http.get(`${BASE}/menu`, () =>
      HttpResponse.json({ error: 'INTERNAL_ERROR', message: 'Error interno' }, { status: 500 })
    ),
}
