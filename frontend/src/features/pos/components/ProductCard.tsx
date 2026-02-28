import { Card, CardActionArea, CardContent, CardMedia, Typography, Box, Chip } from '@mui/material'
import type { MenuProductItem } from '../types/Menu'

interface ProductCardProps {
  product: MenuProductItem
  onClick: () => void
}

export default function ProductCard({ product, onClick }: ProductCardProps) {
  return (
    <Card
      sx={{
        height: 200,
        position: 'relative',
        transition: 'box-shadow 0.2s',
        '&:hover': {
          boxShadow: product.isAvailable ? 6 : undefined,
        },
      }}
    >
      <CardActionArea
        onClick={onClick}
        disabled={!product.isAvailable}
        sx={{ height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'stretch' }}
      >
        {product.imageUrl ? (
          <CardMedia
            component="img"
            height="110"
            image={product.imageUrl}
            alt={product.name}
          />
        ) : (
          <Box
            sx={{
              height: 110,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              bgcolor: 'grey.100',
              flexShrink: 0,
            }}
            aria-label="sin imagen"
          >
            <Typography variant="body2" color="text.secondary">
              Sin imagen
            </Typography>
          </Box>
        )}
        <CardContent sx={{ pb: '8px !important', flexGrow: 1 }}>
          <Typography variant="subtitle2" noWrap title={product.name}>
            {product.name}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            ${product.basePrice.toFixed(2)}
          </Typography>
        </CardContent>
      </CardActionArea>

      {!product.isAvailable && (
        <Box
          sx={{
            position: 'absolute',
            inset: 0,
            bgcolor: 'rgba(0,0,0,0.45)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            pointerEvents: 'none',
          }}
        >
          <Chip label="No disponible" size="small" />
        </Box>
      )}
    </Card>
  )
}
