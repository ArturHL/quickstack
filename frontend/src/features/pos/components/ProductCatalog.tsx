import { useState } from 'react'
import { Box, Tab, Tabs, Grid, Alert, CircularProgress } from '@mui/material'
import { useMenuQuery } from '../hooks/useMenuQuery'
import ProductCard from './ProductCard'
import type { MenuProductItem } from '../types/Menu'

interface ProductCatalogProps {
  onProductClick?: (product: MenuProductItem) => void
}

export default function ProductCatalog({ onProductClick }: ProductCatalogProps) {
  const { data, isLoading, isError } = useMenuQuery()
  const [activeTab, setActiveTab] = useState(0)

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" p={4} role="status" aria-label="Cargando catálogo">
        <CircularProgress />
      </Box>
    )
  }

  if (isError || !data) {
    return (
      <Alert severity="error" sx={{ m: 2 }}>
        Error al cargar el catálogo. Inténtalo de nuevo.
      </Alert>
    )
  }

  const { categories } = data
  const activeCategory = categories[activeTab]

  return (
    <Box>
      <Tabs
        value={activeTab}
        onChange={(_, newVal: number) => setActiveTab(newVal)}
        variant="scrollable"
        scrollButtons="auto"
        sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}
        aria-label="Categorías del catálogo"
      >
        {categories.map((cat) => (
          <Tab key={cat.id} label={cat.name} />
        ))}
      </Tabs>

      {activeCategory && (
        <Grid container spacing={2}>
          {activeCategory.products.map((product) => (
            <Grid item key={product.id} xs={12} sm={6} md={3}>
              <ProductCard
                product={product}
                onClick={() => onProductClick?.(product)}
              />
            </Grid>
          ))}
        </Grid>
      )}
    </Box>
  )
}
