import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Alert,
  Box,
  Card,
  CardActionArea,
  CardContent,
  Chip,
  CircularProgress,
  Grid,
  Tab,
  Tabs,
  Typography,
} from '@mui/material'
import { useAreasQuery } from '../hooks/useAreasQuery'
import { useTablesQuery } from '../hooks/useTablesQuery'
import { useBranchStore } from '../stores/branchStore'
import { useCartStore } from '../stores/cartStore'

export default function TableSelector() {
  const navigate = useNavigate()
  const activeBranchId = useBranchStore((s) => s.activeBranchId)
  const serviceType = useCartStore((s) => s.serviceType)
  const setServiceDetails = useCartStore((s) => s.setServiceDetails)

  const { data: areas = [], isLoading: areasLoading, isError: areasError } = useAreasQuery(activeBranchId)
  const [selectedAreaIndex, setSelectedAreaIndex] = useState(0)
  const selectedAreaId = areas[selectedAreaIndex]?.id ?? null

  const { data: tables = [], isLoading: tablesLoading } = useTablesQuery(selectedAreaId)

  if (areasLoading) {
    return (
      <Box display="flex" justifyContent="center" p={4} role="status" aria-label="Cargando áreas">
        <CircularProgress />
      </Box>
    )
  }

  if (areasError) {
    return <Alert severity="error">Error al cargar las áreas</Alert>
  }

  const handleTableSelect = (tableId: string) => {
    setServiceDetails(serviceType ?? 'DINE_IN', tableId)
    navigate('/pos/catalog')
  }

  return (
    <Box>
      <Typography variant="h5" gutterBottom>
        Seleccionar Mesa
      </Typography>

      <Tabs
        value={selectedAreaIndex}
        onChange={(_, val: number) => setSelectedAreaIndex(val)}
        sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}
        aria-label="Áreas del restaurante"
      >
        {areas.map((area) => (
          <Tab key={area.id} label={area.name} />
        ))}
      </Tabs>

      {tablesLoading ? (
        <Box display="flex" justifyContent="center" p={2} role="status" aria-label="Cargando mesas">
          <CircularProgress size={24} />
        </Box>
      ) : (
        <Grid container spacing={2}>
          {tables.map((table) => {
            const isAvailable = table.status === 'AVAILABLE'
            return (
              <Grid item xs={6} sm={4} md={3} key={table.id}>
                <Card>
                  <CardActionArea
                    onClick={() => handleTableSelect(table.id)}
                    disabled={!isAvailable}
                    aria-label={`Mesa ${table.number}`}
                  >
                    <CardContent>
                      <Typography variant="h6">Mesa {table.number}</Typography>
                      {table.name && (
                        <Typography variant="body2" color="text.secondary">
                          {table.name}
                        </Typography>
                      )}
                      <Typography variant="caption" color="text.secondary" display="block">
                        Capacidad: {table.capacity}
                      </Typography>
                      <Box mt={1}>
                        <Chip
                          label={table.status}
                          size="small"
                          color={isAvailable ? 'success' : 'default'}
                        />
                      </Box>
                    </CardContent>
                  </CardActionArea>
                </Card>
              </Grid>
            )
          })}
        </Grid>
      )}
    </Box>
  )
}
