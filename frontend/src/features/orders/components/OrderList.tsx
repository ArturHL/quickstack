import { useState } from 'react'
import {
    Box,
    Card,
    CardActionArea,
    CardContent,
    Chip,
    CircularProgress,
    FormControl,
    InputLabel,
    MenuItem,
    Select,
    TextField,
    Typography,
} from '@mui/material'
import { useNavigate } from 'react-router-dom'
import { useOrdersQuery } from '../hooks/useOrdersQuery'
import type { OrderResponse, OrderStatus } from '../../pos/types/Order'
import { STATUS_ID_TO_NAME } from '../../../mocks/handlers/orderHandlers'

const STATUS_COLORS: Record<OrderStatus, 'default' | 'warning' | 'info' | 'success' | 'error'> = {
    PENDING: 'warning',
    IN_PROGRESS: 'info',
    READY: 'success',
    DELIVERED: 'default',
    COMPLETED: 'success',
    CANCELLED: 'error',
}

const STATUS_LABELS: Record<OrderStatus, string> = {
    PENDING: 'Pendiente',
    IN_PROGRESS: 'En Progreso',
    READY: 'Listo',
    DELIVERED: 'Entregado',
    COMPLETED: 'Completado',
    CANCELLED: 'Cancelado',
}

const SERVICE_LABELS: Record<string, string> = {
    COUNTER: 'Mostrador',
    DINE_IN: 'En Mesa',
    DELIVERY: 'Domicilio',
    TAKEOUT: 'Para Llevar',
}

function getStatusName(statusId: string): OrderStatus {
    return STATUS_ID_TO_NAME[statusId] ?? 'PENDING'
}

function todayISO(): string {
    return new Date().toISOString().slice(0, 10)
}

function formatTime(iso: string): string {
    return new Date(iso).toLocaleTimeString('es-MX', { hour: '2-digit', minute: '2-digit' })
}

export default function OrderList() {
    const navigate = useNavigate()
    const [date, setDate] = useState(todayISO())
    const [statusFilter, setStatusFilter] = useState<OrderStatus | ''>('')

    const { data, isLoading, isError } = useOrdersQuery({
        date,
        status: statusFilter || undefined,
    })

    const orders = data?.content ?? []

    return (
        <Box>
            <Typography variant="h5" mb={2}>
                Pedidos del Día
            </Typography>

            {/* Filters */}
            <Box display="flex" gap={2} mb={3} flexWrap="wrap">
                <TextField
                    type="date"
                    label="Fecha"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    size="small"
                    InputLabelProps={{ shrink: true }}
                    inputProps={{ 'aria-label': 'fecha' }}
                />

                <FormControl size="small" sx={{ minWidth: 160 }}>
                    <InputLabel id="status-filter-label">Estado</InputLabel>
                    <Select
                        labelId="status-filter-label"
                        label="Estado"
                        value={statusFilter}
                        onChange={(e) => setStatusFilter(e.target.value as OrderStatus | '')}
                        inputProps={{ 'aria-label': 'filtro estado' }}
                    >
                        <MenuItem value="">Todos</MenuItem>
                        <MenuItem value="PENDING">Pendiente</MenuItem>
                        <MenuItem value="IN_PROGRESS">En Progreso</MenuItem>
                        <MenuItem value="READY">Listo</MenuItem>
                        <MenuItem value="COMPLETED">Completado</MenuItem>
                        <MenuItem value="CANCELLED">Cancelado</MenuItem>
                    </Select>
                </FormControl>
            </Box>

            {/* Content */}
            {isLoading && (
                <Box display="flex" justifyContent="center" py={4}>
                    <CircularProgress />
                </Box>
            )}

            {isError && (
                <Typography color="error" role="alert">
                    Error al cargar los pedidos
                </Typography>
            )}

            {!isLoading && !isError && orders.length === 0 && (
                <Typography color="text.secondary">No hay pedidos para esta fecha</Typography>
            )}

            {!isLoading && !isError && orders.length > 0 && (
                <Box display="flex" flexDirection="column" gap={2}>
                    {orders.map((order: OrderResponse) => {
                        const status = getStatusName(order.statusId)
                        return (
                            <Card key={order.id} variant="outlined">
                                <CardActionArea onClick={() => navigate(`/orders/${order.id}`)}>
                                    <CardContent>
                                        <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                                            <Typography variant="h6" component="span">
                                                #{order.dailySequence} — {order.orderNumber}
                                            </Typography>
                                            <Chip
                                                label={STATUS_LABELS[status]}
                                                color={STATUS_COLORS[status]}
                                                size="small"
                                            />
                                        </Box>
                                        <Box display="flex" justifyContent="space-between" alignItems="center">
                                            <Box display="flex" gap={1} alignItems="center">
                                                <Chip
                                                    label={SERVICE_LABELS[order.serviceType] ?? order.serviceType}
                                                    variant="outlined"
                                                    size="small"
                                                />
                                                <Typography variant="body2" color="text.secondary">
                                                    {formatTime(order.openedAt)}
                                                </Typography>
                                            </Box>
                                            <Typography variant="h6" component="span">
                                                ${order.total.toFixed(2)}
                                            </Typography>
                                        </Box>
                                    </CardContent>
                                </CardActionArea>
                            </Card>
                        )
                    })}
                </Box>
            )}
        </Box>
    )
}
