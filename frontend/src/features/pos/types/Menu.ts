export type ProductType = 'SIMPLE' | 'VARIANT'

export interface MenuModifierItem {
  id: string
  name: string
  priceAdjustment: number
  isDefault: boolean
  sortOrder: number
}

export interface MenuModifierGroupItem {
  id: string
  name: string
  minSelections: number
  maxSelections: number | null
  isRequired: boolean
  modifiers: MenuModifierItem[]
}

export interface MenuVariantItem {
  id: string
  name: string
  priceAdjustment: number
  effectivePrice: number
  isDefault: boolean
  sortOrder: number
}

export interface MenuProductItem {
  id: string
  name: string
  basePrice: number
  imageUrl: string | null
  isAvailable: boolean
  productType: ProductType
  variants: MenuVariantItem[]
  modifierGroups: MenuModifierGroupItem[]
}

export interface MenuCategoryItem {
  id: string
  name: string
  sortOrder: number
  imageUrl: string | null
  products: MenuProductItem[]
}

export interface ComboProductEntry {
  productId: string
  productName: string
  quantity: number
}

export interface MenuComboItem {
  id: string
  name: string
  description: string | null
  imageUrl: string | null
  price: number
  sortOrder: number
  items: ComboProductEntry[]
}

export interface MenuResponse {
  categories: MenuCategoryItem[]
  combos: MenuComboItem[]
}
