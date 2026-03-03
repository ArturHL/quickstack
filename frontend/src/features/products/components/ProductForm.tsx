import { useState, useEffect } from 'react'
import {
  Box,
  Button,
  CircularProgress,
  Divider,
  FormControl,
  FormHelperText,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Typography,
} from '@mui/material'
import { Add, Delete } from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { useCategoriesQuery } from '../hooks/useCategoriesQuery'
import { useVariantsQuery } from '../hooks/useVariantsQuery'
import { useCreateProductMutation } from '../hooks/useCreateProductMutation'
import { useUpdateProductMutation } from '../hooks/useUpdateProductMutation'
import { productApi } from '../api/productApi'
import { variantApi } from '../api/variantApi'
import type { ProductType } from '../types/Product'

interface VariantInput {
  id?: string
  name: string
  effectivePrice: string
}

interface ProductFormProps {
  productId?: string
}

export default function ProductForm({ productId }: ProductFormProps) {
  const navigate = useNavigate()
  const isEdit = !!productId

  const { data: categories } = useCategoriesQuery()
  const { mutate: createProduct, isPending: isCreating } = useCreateProductMutation()
  const { mutate: updateProduct, isPending: isUpdating } = useUpdateProductMutation()

  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [sku, setSku] = useState('')
  const [basePrice, setBasePrice] = useState('')
  const [costPrice, setCostPrice] = useState('')
  const [productType, setProductType] = useState<ProductType>('SIMPLE')
  const [variants, setVariants] = useState<VariantInput[]>([{ name: '', effectivePrice: '' }])
  const [deletedVariantIds, setDeletedVariantIds] = useState<string[]>([])
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [isLoadingProduct, setIsLoadingProduct] = useState(isEdit)
  const [isSavingVariants, setIsSavingVariants] = useState(false)

  const { data: existingVariants } = useVariantsQuery(
    productId,
    isEdit && productType === 'VARIANT'
  )

  useEffect(() => {
    if (!isEdit) return
    let cancelled = false
    productApi.getProduct(productId).then((p) => {
      if (cancelled) return
      setName(p.name)
      setDescription(p.description ?? '')
      setCategoryId(p.categoryId ?? '')
      setSku(p.sku ?? '')
      setBasePrice(String(p.basePrice))
      setCostPrice(p.costPrice != null ? String(p.costPrice) : '')
      setProductType(p.productType)
      setIsLoadingProduct(false)
    })
    return () => { cancelled = true }
  }, [isEdit, productId])

  useEffect(() => {
    if (existingVariants && existingVariants.length > 0) {
      setVariants(
        existingVariants.map((v) => ({
          id: v.id,
          name: v.name,
          effectivePrice: String(v.effectivePrice),
        }))
      )
    }
  }, [existingVariants])

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {}
    if (!name.trim()) newErrors.name = 'El nombre es requerido'
    if (!categoryId) newErrors.categoryId = 'La categoría es requerida'
    if (sku && !/^[A-Z0-9_-]{1,50}$/.test(sku)) newErrors.sku = 'SKU inválido: solo mayúsculas, números, _ y -'
    const price = parseFloat(basePrice)
    if (isNaN(price) || price < 0) newErrors.basePrice = 'El precio debe ser mayor o igual a 0'
    if (productType === 'VARIANT') {
      variants.forEach((v, i) => {
        if (!v.name.trim()) newErrors[`variant-${i}-name`] = 'Nombre requerido'
        const vPrice = parseFloat(v.effectivePrice)
        if (isNaN(vPrice) || vPrice < 0) newErrors[`variant-${i}-price`] = 'Precio inválido'
      })
    }
    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleRemoveVariant = (index: number) => {
    const variant = variants[index]
    if (variant.id) {
      setDeletedVariantIds((prev) => [...prev, variant.id!])
    }
    setVariants((prev) => prev.filter((_, idx) => idx !== index))
  }

  const saveVariantChanges = async (pid: string) => {
    const ops: Promise<unknown>[] = []

    // Delete removed variants
    for (const vid of deletedVariantIds) {
      ops.push(variantApi.deleteVariant(pid, vid))
    }

    // Update or create variants
    for (const v of variants) {
      const price = parseFloat(v.effectivePrice)
      if (v.id) {
        ops.push(variantApi.updateVariant(pid, v.id, { name: v.name.trim(), effectivePrice: price }))
      } else {
        ops.push(variantApi.createVariant(pid, { name: v.name.trim(), effectivePrice: price }))
      }
    }

    await Promise.all(ops)
  }

  const handleSubmit = async () => {
    if (!validate()) return

    const body = {
      name: name.trim(),
      description: description.trim() || undefined,
      categoryId: categoryId || undefined,
      sku: sku.trim() || undefined,
      basePrice: parseFloat(basePrice),
      costPrice: costPrice ? parseFloat(costPrice) : undefined,
      productType,
    }

    if (isEdit) {
      updateProduct(
        { id: productId, body },
        {
          onSuccess: async () => {
            if (productType === 'VARIANT') {
              setIsSavingVariants(true)
              try {
                await saveVariantChanges(productId)
              } finally {
                setIsSavingVariants(false)
              }
            }
            navigate('/admin/products')
          },
        }
      )
    } else {
      const createBody = productType === 'VARIANT'
        ? {
            ...body,
            variants: variants.map((v) => ({
              name: v.name.trim(),
              effectivePrice: parseFloat(v.effectivePrice),
            })),
          }
        : body

      createProduct(createBody as Parameters<typeof createProduct>[0], {
        onSuccess: () => navigate('/admin/products'),
      })
    }
  }

  if (isLoadingProduct) {
    return (
      <Box display="flex" justifyContent="center" p={4}>
        <CircularProgress />
      </Box>
    )
  }

  const isPending = isCreating || isUpdating || isSavingVariants

  return (
    <Box maxWidth={600}>
      <Typography variant="h5" mb={3}>
        {isEdit ? 'Editar Producto' : 'Nuevo Producto'}
      </Typography>

      <Box display="flex" flexDirection="column" gap={2}>
        <TextField
          label="Nombre *"
          value={name}
          onChange={(e) => setName(e.target.value)}
          error={!!errors.name}
          helperText={errors.name}
          inputProps={{ 'aria-label': 'nombre producto' }}
        />

        <TextField
          label="Descripción"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          multiline
          rows={2}
        />

        <FormControl error={!!errors.categoryId}>
          <InputLabel id="category-label">Categoría *</InputLabel>
          <Select
            labelId="category-label"
            label="Categoría *"
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
          >
            {categories?.map((cat) => (
              <MenuItem key={cat.id} value={cat.id}>{cat.name}</MenuItem>
            ))}
          </Select>
          {errors.categoryId && <FormHelperText>{errors.categoryId}</FormHelperText>}
        </FormControl>

        <TextField
          label="SKU"
          value={sku}
          onChange={(e) => setSku(e.target.value.toUpperCase().replace(/[^A-Z0-9_-]/g, ''))}
          error={!!errors.sku}
          helperText={errors.sku ?? 'Solo letras mayúsculas, números, guiones y guiones bajos'}
          inputProps={{ 'aria-label': 'sku' }}
        />

        <Box display="flex" gap={2}>
          <TextField
            label="Precio base *"
            type="number"
            value={basePrice}
            onChange={(e) => setBasePrice(e.target.value)}
            error={!!errors.basePrice}
            helperText={errors.basePrice}
            inputProps={{ min: 0, step: 0.01, 'aria-label': 'precio base' }}
          />
          <TextField
            label="Precio costo"
            type="number"
            value={costPrice}
            onChange={(e) => setCostPrice(e.target.value)}
            inputProps={{ min: 0, step: 0.01 }}
          />
        </Box>

        <FormControl>
          <InputLabel id="type-label">Tipo de producto</InputLabel>
          <Select
            labelId="type-label"
            label="Tipo de producto"
            value={productType}
            onChange={(e) => setProductType(e.target.value as ProductType)}
            inputProps={{ 'aria-label': 'tipo de producto' }}
            disabled={isEdit}
          >
            <MenuItem value="SIMPLE">Simple</MenuItem>
            <MenuItem value="VARIANT">Con variantes</MenuItem>
            <MenuItem value="COMBO">Combo</MenuItem>
          </Select>
        </FormControl>

        {/* Variants sub-form */}
        {productType === 'VARIANT' && (
          <Box>
            <Divider sx={{ my: 1 }} />
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
              <Typography variant="subtitle1">Variantes</Typography>
              <Button
                size="small"
                startIcon={<Add />}
                onClick={() => setVariants((prev) => [...prev, { name: '', effectivePrice: '' }])}
              >
                Agregar variante
              </Button>
            </Box>
            {variants.map((v, i) => (
              <Box key={v.id ?? `new-${i}`} display="flex" gap={1} alignItems="flex-start" mb={1}>
                <TextField
                  label="Nombre variante"
                  value={v.name}
                  size="small"
                  onChange={(e) => {
                    const updated = [...variants]
                    updated[i] = { ...updated[i], name: e.target.value }
                    setVariants(updated)
                  }}
                  error={!!errors[`variant-${i}-name`]}
                  helperText={errors[`variant-${i}-name`]}
                  inputProps={{ 'aria-label': `nombre variante ${i + 1}` }}
                />
                <TextField
                  label="Precio"
                  type="number"
                  value={v.effectivePrice}
                  size="small"
                  onChange={(e) => {
                    const updated = [...variants]
                    updated[i] = { ...updated[i], effectivePrice: e.target.value }
                    setVariants(updated)
                  }}
                  error={!!errors[`variant-${i}-price`]}
                  helperText={errors[`variant-${i}-price`]}
                  inputProps={{ min: 0, step: 0.01, 'aria-label': `precio variante ${i + 1}` }}
                />
                {variants.length > 1 && (
                  <IconButton
                    size="small"
                    color="error"
                    onClick={() => handleRemoveVariant(i)}
                    aria-label={`eliminar variante ${i + 1}`}
                  >
                    <Delete fontSize="small" />
                  </IconButton>
                )}
              </Box>
            ))}
            {Object.keys(errors).some((k) => k.startsWith('variant')) && (
              <FormHelperText error>Revisa los campos de variantes</FormHelperText>
            )}
          </Box>
        )}
      </Box>

      <Box display="flex" gap={2} mt={3}>
        <Button variant="outlined" onClick={() => navigate('/admin/products')}>
          Cancelar
        </Button>
        <Button variant="contained" onClick={handleSubmit} disabled={isPending}>
          {isEdit ? 'Guardar cambios' : 'Crear producto'}
        </Button>
      </Box>
    </Box>
  )
}
