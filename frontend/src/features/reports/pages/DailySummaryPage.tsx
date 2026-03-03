import { useState } from 'react'
import {
  Box,
  Card,
  CardContent,
  CircularProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { useDailySummaryQuery } from '../hooks/useDailySummaryQuery'
import { useBranchStore } from '../../pos/stores/branchStore'

function todayISO(): string {
  return new Date().toISOString().split('T')[0]
}

interface MetricCardProps {
  label: string
  value: string
}

function MetricCard({ label, value }: MetricCardProps) {
  return (
    <Card sx={{ flex: 1, minWidth: 160 }}>
      <CardContent>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          {label}
        </Typography>
        <Typography variant="h5" fontWeight="bold">
          {value}
        </Typography>
      </CardContent>
    </Card>
  )
}

export default function DailySummaryPage() {
  const [date, setDate] = useState(todayISO())
  const activeBranchId = useBranchStore((s) => s.activeBranchId)
  const { data, isLoading, isError } = useDailySummaryQuery(activeBranchId, date)

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5">Reportes</Typography>
        <TextField
          label="Fecha"
          type="date"
          value={date}
          onChange={(e) => setDate(e.target.value)}
          size="small"
          InputLabelProps={{ shrink: true }}
          inputProps={{ 'aria-label': 'fecha reporte' }}
        />
      </Box>

      {!activeBranchId && (
        <Typography color="text.secondary">
          Selecciona una sucursal para ver el reporte.
        </Typography>
      )}

      {activeBranchId && isLoading && (
        <Box display="flex" justifyContent="center" p={4}>
          <CircularProgress />
        </Box>
      )}

      {activeBranchId && isError && (
        <Typography color="error">Error al cargar el reporte.</Typography>
      )}

      {data && (
        <>
          {/* Metric cards */}
          <Box display="flex" gap={2} mb={4} flexWrap="wrap">
            <MetricCard
              label="Total ventas"
              value={`$${data.totalSales.toFixed(2)}`}
            />
            <MetricCard
              label="Pedidos"
              value={String(data.orderCount)}
            />
            <MetricCard
              label="Ticket promedio"
              value={`$${data.averageTicket.toFixed(2)}`}
            />
          </Box>

          {/* Top products table */}
          <Typography variant="h6" mb={1}>
            Productos más vendidos
          </Typography>

          {data.topProducts.length === 0 ? (
            <Typography color="text.secondary">
              Sin ventas registradas para esta fecha.
            </Typography>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>#</TableCell>
                    <TableCell>Producto</TableCell>
                    <TableCell align="right">Cantidad vendida</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {data.topProducts.map((entry, index) => (
                    <TableRow key={entry.productName}>
                      <TableCell>{index + 1}</TableCell>
                      <TableCell>{entry.productName}</TableCell>
                      <TableCell align="right">{entry.quantitySold}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </>
      )}
    </Box>
  )
}
