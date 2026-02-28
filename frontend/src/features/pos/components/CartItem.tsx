import { Box, Chip, IconButton, ListItem, ListItemText, Typography } from '@mui/material'
import { Add, Delete, Remove } from '@mui/icons-material'
import type { CartItem } from '../types/Cart'

interface CartItemComponentProps {
  item: CartItem
  index: number
  onUpdateQty: (index: number, qty: number) => void
  onRemove: (index: number) => void
}

export default function CartItemComponent({ item, index, onUpdateQty, onRemove }: CartItemComponentProps) {
  return (
    <ListItem
      alignItems="flex-start"
      secondaryAction={
        <IconButton edge="end" aria-label="eliminar item" onClick={() => onRemove(index)}>
          <Delete />
        </IconButton>
      }
      sx={{ pr: 7 }}
    >
      <ListItemText
        primary={
          <Box display="flex" justifyContent="space-between" alignItems="center">
            <Typography variant="body1">
              {item.productName}
              {item.variantName ? ` — ${item.variantName}` : ''}
            </Typography>
            <Typography variant="body1" fontWeight="bold">
              ${item.lineTotal.toFixed(2)}
            </Typography>
          </Box>
        }
        secondary={
          <Box component="span">
            {item.selectedModifiers.length > 0 && (
              <Box display="flex" flexWrap="wrap" gap={0.5} mt={0.5} component="span">
                {item.selectedModifiers.map((mod) => (
                  <Chip key={mod.modifierId} label={mod.modifierName} size="small" variant="outlined" />
                ))}
              </Box>
            )}
            <Box display="flex" alignItems="center" gap={1} mt={0.5} component="span">
              <IconButton
                size="small"
                onClick={() => onUpdateQty(index, item.quantity - 1)}
                aria-label="disminuir cantidad"
                disabled={item.quantity <= 1}
              >
                <Remove fontSize="small" />
              </IconButton>
              <Typography variant="body2" aria-label="cantidad">
                {item.quantity}
              </Typography>
              <IconButton
                size="small"
                onClick={() => onUpdateQty(index, item.quantity + 1)}
                aria-label="aumentar cantidad"
              >
                <Add fontSize="small" />
              </IconButton>
            </Box>
          </Box>
        }
      />
    </ListItem>
  )
}
