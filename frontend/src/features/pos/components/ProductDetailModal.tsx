import { useState, useEffect, useMemo } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  Divider,
  IconButton,
} from '@mui/material'
import { Add, Remove } from '@mui/icons-material'
import type { MenuProductItem } from '../types/Menu'
import type { SelectedModifier } from '../types/Cart'
import VariantSelector from './VariantSelector'
import ModifierGroup from './ModifierGroup'
import { useCartStore } from '../stores/cartStore'

interface ProductDetailModalProps {
  product: MenuProductItem | null
  open: boolean
  onClose: () => void
}

export default function ProductDetailModal({ product, open, onClose }: ProductDetailModalProps) {
  const addItem = useCartStore((state) => state.addItem)

  const [selectedVariantId, setSelectedVariantId] = useState<string | null>(null)
  const [selectedModifiers, setSelectedModifiers] = useState<Record<string, string[]>>({})
  const [quantity, setQuantity] = useState(1)
  const [errors, setErrors] = useState<Record<string, string>>({})

  // Reset state whenever a new product is opened
  useEffect(() => {
    if (product) {
      const defaultVariant = product.variants.find((v) => v.isDefault) ?? null
      setSelectedVariantId(defaultVariant?.id ?? null)

      const initialMods: Record<string, string[]> = {}
      product.modifierGroups.forEach((g) => { initialMods[g.id] = [] })
      setSelectedModifiers(initialMods)

      setQuantity(1)
      setErrors({})
    }
  }, [product])

  const selectedVariant = useMemo(() => {
    if (!product || product.productType !== 'VARIANT') return null
    return product.variants.find((v) => v.id === selectedVariantId) ?? null
  }, [product, selectedVariantId])

  const unitPrice = useMemo(() => {
    if (!product) return 0
    return product.productType === 'VARIANT'
      ? (selectedVariant?.effectivePrice ?? product.basePrice)
      : product.basePrice
  }, [product, selectedVariant])

  const modifiersTotal = useMemo(() => {
    if (!product) return 0
    const allSelectedIds = Object.values(selectedModifiers).flat()
    return allSelectedIds.reduce((sum, modId) => {
      for (const group of product.modifierGroups) {
        const mod = group.modifiers.find((m) => m.id === modId)
        if (mod) return sum + mod.priceAdjustment
      }
      return sum
    }, 0)
  }, [product, selectedModifiers])

  const lineTotal = (unitPrice + modifiersTotal) * quantity

  const validate = (): boolean => {
    if (!product) return false
    const newErrors: Record<string, string> = {}

    if (product.productType === 'VARIANT' && !selectedVariantId) {
      newErrors['variant'] = 'Selecciona una variante'
    }

    product.modifierGroups.forEach((group) => {
      const selected = selectedModifiers[group.id] ?? []
      if (selected.length < group.minSelections) {
        newErrors[group.id] = `Selecciona al menos ${group.minSelections}`
      }
    })

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleAddToCart = () => {
    if (!product || !validate()) return

    const allSelectedIds = Object.values(selectedModifiers).flat()
    const resolvedModifiers: SelectedModifier[] = allSelectedIds
      .map((modId) => {
        for (const group of product.modifierGroups) {
          const mod = group.modifiers.find((m) => m.id === modId)
          if (mod) return { modifierId: mod.id, modifierName: mod.name, priceAdjustment: mod.priceAdjustment }
        }
        return null
      })
      .filter((m): m is SelectedModifier => m !== null)

    addItem({
      productId: product.id,
      variantId: selectedVariantId ?? undefined,
      productName: product.name,
      variantName: selectedVariant?.name,
      quantity,
      unitPrice,
      selectedModifiers: resolvedModifiers,
    })

    onClose()
  }

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
      {product && (
        <>
          <DialogTitle>{product.name}</DialogTitle>

          <DialogContent dividers>
            {product.imageUrl ? (
              <Box
                component="img"
                src={product.imageUrl}
                alt={product.name}
                sx={{ width: '100%', maxHeight: 200, objectFit: 'cover', borderRadius: 1, mb: 2 }}
              />
            ) : (
              <Box
                sx={{
                  height: 100,
                  bgcolor: 'grey.100',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  borderRadius: 1,
                  mb: 2,
                }}
              >
                <Typography color="text.secondary">Sin imagen</Typography>
              </Box>
            )}

            {/* Variants */}
            {product.productType === 'VARIANT' && (
              <>
                <VariantSelector
                  variants={product.variants}
                  selectedId={selectedVariantId}
                  onChange={setSelectedVariantId}
                />
                {errors['variant'] && (
                  <Typography color="error" variant="caption" display="block">
                    {errors['variant']}
                  </Typography>
                )}
                <Divider sx={{ my: 2 }} />
              </>
            )}

            {/* Modifier groups */}
            {product.modifierGroups.map((group) => (
              <Box key={group.id}>
                <ModifierGroup
                  group={group}
                  selectedIds={selectedModifiers[group.id] ?? []}
                  onChange={(ids) =>
                    setSelectedModifiers((prev) => ({ ...prev, [group.id]: ids }))
                  }
                />
                {errors[group.id] && (
                  <Typography color="error" variant="caption" display="block" sx={{ mt: -1, mb: 1 }}>
                    {errors[group.id]}
                  </Typography>
                )}
              </Box>
            ))}

            {/* Quantity */}
            <Divider sx={{ my: 2 }} />
            <Box display="flex" alignItems="center" gap={2}>
              <Typography>Cantidad:</Typography>
              <IconButton
                size="small"
                onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                aria-label="disminuir cantidad"
              >
                <Remove />
              </IconButton>
              <Typography variant="h6" minWidth={24} textAlign="center" aria-label="cantidad">
                {quantity}
              </Typography>
              <IconButton
                size="small"
                onClick={() => setQuantity((q) => q + 1)}
                aria-label="aumentar cantidad"
              >
                <Add />
              </IconButton>
            </Box>

            {/* Line total */}
            <Box display="flex" justifyContent="flex-end" mt={2}>
              <Typography variant="h6" aria-label="total del item">
                Total: ${lineTotal.toFixed(2)}
              </Typography>
            </Box>
          </DialogContent>

          <DialogActions>
            <Button onClick={onClose}>Cancelar</Button>
            <Button onClick={handleAddToCart} variant="contained">
              Agregar al Carrito
            </Button>
          </DialogActions>
        </>
      )}
    </Dialog>
  )
}
