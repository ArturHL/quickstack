import { useState } from 'react'
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import { Add, Delete } from '@mui/icons-material'
import { useProductsQuery } from '../hooks/useProductsQuery'
import type { ComboResponse } from '../types/Product'

interface ComboItem {
  productId: string
  productName: string
  quantity: number
}

interface ComboFormProps {
  open: boolean
  onClose: () => void
  onSubmit: (data: {
    name: string
    description?: string
    price: number
    items: { productId: string; quantity: number }[]
  }) => void
  isPending: boolean
  initial?: ComboResponse | null
}

export default function ComboForm({ open, onClose, onSubmit, isPending, initial }: ComboFormProps) {
  const isEdit = !!initial
  const { data: productsPage } = useProductsQuery({ size: 100 })
  const products = productsPage?.content ?? []

  const [name, setName] = useState(initial?.name ?? '')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [price, setPrice] = useState(initial ? String(initial.price) : '')
  const [items, setItems] = useState<ComboItem[]>(
    initial?.items.map((i) => ({
      productId: i.productId,
      productName: i.productName,
      quantity: i.quantity,
    })) ?? []
  )
  const [selectedProductId, setSelectedProductId] = useState('')
  const [selectedQty, setSelectedQty] = useState('1')
  const [errors, setErrors] = useState<Record<string, string>>({})

  const handleAddItem = () => {
    if (!selectedProductId) return
    const product = products.find((p) => p.id === selectedProductId)
    if (!product) return
    const already = items.find((i) => i.productId === selectedProductId)
    if (already) return
    setItems((prev) => [
      ...prev,
      { productId: product.id, productName: product.name, quantity: parseInt(selectedQty) || 1 },
    ])
    setSelectedProductId('')
    setSelectedQty('1')
  }

  const handleRemoveItem = (productId: string) => {
    setItems((prev) => prev.filter((i) => i.productId !== productId))
  }

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {}
    if (!name.trim()) newErrors.name = 'El nombre es requerido'
    const p = parseFloat(price)
    if (isNaN(p) || p < 0) newErrors.price = 'El precio debe ser mayor o igual a 0'
    if (items.length === 0) newErrors.items = 'Agrega al menos un producto al combo'
    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = () => {
    if (!validate()) return
    onSubmit({
      name: name.trim(),
      description: description.trim() || undefined,
      price: parseFloat(price),
      items: items.map(({ productId, quantity }) => ({ productId, quantity })),
    })
  }

  const availableProducts = products.filter(
    (p) => !items.some((i) => i.productId === p.id) && p.productType !== 'COMBO'
  )

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{isEdit ? 'Editar Combo' : 'Nuevo Combo'}</DialogTitle>
      <DialogContent>
        <TextField
          label="Nombre *"
          value={name}
          onChange={(e) => { setName(e.target.value); setErrors((prev) => ({ ...prev, name: '' })) }}
          error={!!errors.name}
          helperText={errors.name}
          fullWidth
          margin="normal"
          inputProps={{ 'aria-label': 'nombre combo' }}
        />

        <TextField
          label="Descripción"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          fullWidth
          margin="normal"
          multiline
          rows={2}
        />

        <TextField
          label="Precio *"
          type="number"
          value={price}
          onChange={(e) => { setPrice(e.target.value); setErrors((prev) => ({ ...prev, price: '' })) }}
          error={!!errors.price}
          helperText={errors.price}
          fullWidth
          margin="normal"
          inputProps={{ min: 0, step: 0.01, 'aria-label': 'precio combo' }}
        />

        <Divider sx={{ my: 2 }} />

        <Typography variant="subtitle2" gutterBottom>
          Componentes del combo
        </Typography>

        {/* Add product row */}
        <Box display="flex" gap={1} alignItems="flex-start" mb={1}>
          <FormControl size="small" sx={{ flex: 1 }}>
            <InputLabel id="product-select-label">Producto</InputLabel>
            <Select
              labelId="product-select-label"
              label="Producto"
              value={selectedProductId}
              onChange={(e) => setSelectedProductId(e.target.value)}
              inputProps={{ 'aria-label': 'seleccionar producto' }}
            >
              {availableProducts.map((p) => (
                <MenuItem key={p.id} value={p.id}>{p.name}</MenuItem>
              ))}
            </Select>
          </FormControl>
          <TextField
            label="Cant."
            type="number"
            value={selectedQty}
            onChange={(e) => setSelectedQty(e.target.value)}
            size="small"
            sx={{ width: 80 }}
            inputProps={{ min: 1, 'aria-label': 'cantidad producto' }}
          />
          <Button
            variant="outlined"
            size="small"
            startIcon={<Add />}
            onClick={handleAddItem}
            disabled={!selectedProductId}
            aria-label="agregar producto al combo"
            sx={{ mt: 0.5 }}
          >
            Agregar
          </Button>
        </Box>

        {errors.items && (
          <Typography color="error" variant="caption">{errors.items}</Typography>
        )}

        {/* Items table */}
        {items.length > 0 && (
          <Table size="small" sx={{ mt: 1 }}>
            <TableHead>
              <TableRow>
                <TableCell>Producto</TableCell>
                <TableCell align="center">Cantidad</TableCell>
                <TableCell align="center">Eliminar</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {items.map((item) => (
                <TableRow key={item.productId}>
                  <TableCell>{item.productName}</TableCell>
                  <TableCell align="center">{item.quantity}</TableCell>
                  <TableCell align="center">
                    <Tooltip title="Quitar">
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => handleRemoveItem(item.productId)}
                        aria-label={`quitar ${item.productName} del combo`}
                      >
                        <Delete fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancelar</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={isPending}>
          {isEdit ? 'Guardar' : 'Crear'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
