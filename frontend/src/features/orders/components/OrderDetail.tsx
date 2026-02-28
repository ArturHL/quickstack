import {
    Box,
    Button,
    Chip,
    CircularProgress,
    Divider,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Typography,
} from '@mui/material'
import { ArrowBack } from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { useOrderQuery } from '../hooks/useOrderQuery'
import { useOrderPaymentsQuery } from '../hooks/useOrderPaymentsQuery'
import { useCancelOrderMutation } from '../hooks/useCancelOrderMutation'
import { useMarkReadyMutation } from '../hooks/useMarkReadyMutation'
import { useAuthStore } from '../../../stores/authStore'
import { STATUS_ID_TO_NAME } from '../../../mocks/handlers/orderHandlers'
import type { OrderStatus } from '../../pos/types/Order'

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

type UserRole = 'OWNER' | 'MANAGER' | 'CASHIER' | 'WAITER'
const ROLE_RANK: Record<UserRole, number> = { WAITER: 0, CASHIER: 1, MANAGER: 2, OWNER: 3 }

function hasMinRole(userRole: string | undefined, minRole: UserRole): boolean {
    if (!userRole) return false
    return (ROLE_RANK[userRole as UserRole] ?? -1) >= ROLE_RANK[minRole]
}

interface OrderDetailProps {
    orderId: string
}

export default function OrderDetail({ orderId }: OrderDetailProps) {
    const navigate = useNavigate()
    const user = useAuthStore((s) => s.user)
    const { data: order, isLoading, isError } = useOrderQuery(orderId)
    const { data: payments } = useOrderPaymentsQuery(orderId)
    const { mutate: cancelOrder, isPending: isCancelling } = useCancelOrderMutation()
    const { mutate: markReady, isPending: isMarkingReady } = useMarkReadyMutation()

    if (isLoading) {
        return (
            <Box display="flex" justifyContent="center" py={4}>
                <CircularProgress />
            </Box>
        )
    }

    if (isError || !order) {
        return (
            <Box>
                <Typography color="error" role="alert">
                    Error al cargar la orden
                </Typography>
                <Button startIcon={<ArrowBack />} onClick={() => navigate('/orders')} sx={{ mt: 2 }}>
                    Volver a Pedidos
                </Button>
            </Box>
        )
    }

    const status = STATUS_ID_TO_NAME[order.statusId] ?? 'PENDING'
    const isManager = hasMinRole(user?.role, 'MANAGER')

    const handleCancel = () => {
        cancelOrder(orderId, { onSuccess: () => navigate('/orders') })
    }

    const handleMarkReady = () => {
        markReady(orderId)
    }

    const handleCobrar = () => {
        navigate('/pos/payment')
    }

    return (
        <Box>
            {/* Back button */}
            <Button startIcon={<ArrowBack />} onClick={() => navigate('/orders')} sx={{ mb: 2 }}>
                Volver a Pedidos
            </Button>

            {/* Header */}
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={2} flexWrap="wrap" gap={1}>
                <Box>
                    <Typography variant="h5">
                        Orden #{order.dailySequence} — {order.orderNumber}
                    </Typography>
                    <Box display="flex" gap={1} mt={0.5}>
                        <Chip label={SERVICE_LABELS[order.serviceType] ?? order.serviceType} variant="outlined" size="small" />
                        <Chip label={STATUS_LABELS[status]} color={STATUS_COLORS[status]} size="small" />
                    </Box>
                </Box>

                {/* Actions */}
                <Box display="flex" gap={1}>
                    {status === 'PENDING' && isManager && (
                        <Button
                            variant="outlined"
                            color="error"
                            onClick={handleCancel}
                            disabled={isCancelling}
                            aria-label="cancelar orden"
                        >
                            Cancelar Orden
                        </Button>
                    )}
                    {status === 'IN_PROGRESS' && isManager && (
                        <Button
                            variant="contained"
                            color="success"
                            onClick={handleMarkReady}
                            disabled={isMarkingReady}
                            aria-label="marcar como listo"
                        >
                            Marcar como Listo
                        </Button>
                    )}
                    {status === 'READY' && (
                        <Button variant="contained" onClick={handleCobrar} aria-label="cobrar">
                            Cobrar
                        </Button>
                    )}
                </Box>
            </Box>

            <Divider sx={{ mb: 2 }} />

            {/* Items Table */}
            <Typography variant="h6" mb={1}>
                Artículos
            </Typography>
            <TableContainer component={Paper} variant="outlined" sx={{ mb: 3 }}>
                <Table size="small">
                    <TableHead>
                        <TableRow>
                            <TableCell>Producto</TableCell>
                            <TableCell align="right">Cant.</TableCell>
                            <TableCell align="right">Precio Unit.</TableCell>
                            <TableCell align="right">Modificadores</TableCell>
                            <TableCell align="right">Total</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {order.items.map((item) => (
                            <TableRow key={item.id}>
                                <TableCell>
                                    {item.productName}
                                    {item.variantName && ` (${item.variantName})`}
                                    {item.modifiers.length > 0 && (
                                        <Typography variant="caption" display="block" color="text.secondary">
                                            {item.modifiers.map((m) => m.modifierName).join(', ')}
                                        </Typography>
                                    )}
                                    {item.notes && (
                                        <Typography variant="caption" display="block" color="text.secondary" fontStyle="italic">
                                            Nota: {item.notes}
                                        </Typography>
                                    )}
                                </TableCell>
                                <TableCell align="right">{item.quantity}</TableCell>
                                <TableCell align="right">${item.unitPrice.toFixed(2)}</TableCell>
                                <TableCell align="right">${item.modifiersTotal.toFixed(2)}</TableCell>
                                <TableCell align="right">${item.lineTotal.toFixed(2)}</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Totals */}
            <Box display="flex" justifyContent="flex-end" mb={3}>
                <Box minWidth={200}>
                    <Box display="flex" justifyContent="space-between">
                        <Typography>Subtotal:</Typography>
                        <Typography>${order.subtotal.toFixed(2)}</Typography>
                    </Box>
                    {order.discount > 0 && (
                        <Box display="flex" justifyContent="space-between">
                            <Typography>Descuento:</Typography>
                            <Typography color="error">-${order.discount.toFixed(2)}</Typography>
                        </Box>
                    )}
                    <Box display="flex" justifyContent="space-between">
                        <Typography>IVA ({(order.taxRate * 100).toFixed(0)}%):</Typography>
                        <Typography>${order.tax.toFixed(2)}</Typography>
                    </Box>
                    <Divider sx={{ my: 0.5 }} />
                    <Box display="flex" justifyContent="space-between">
                        <Typography variant="h6">Total:</Typography>
                        <Typography variant="h6">${order.total.toFixed(2)}</Typography>
                    </Box>
                </Box>
            </Box>

            {/* Payments */}
            {payments && payments.length > 0 && (
                <>
                    <Typography variant="h6" mb={1}>
                        Pagos
                    </Typography>
                    <TableContainer component={Paper} variant="outlined">
                        <Table size="small">
                            <TableHead>
                                <TableRow>
                                    <TableCell>Método</TableCell>
                                    <TableCell align="right">Monto</TableCell>
                                    <TableCell align="right">Recibido</TableCell>
                                    <TableCell align="right">Cambio</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {payments.map((p) => (
                                    <TableRow key={p.id}>
                                        <TableCell>{p.paymentMethod}</TableCell>
                                        <TableCell align="right">${p.amount.toFixed(2)}</TableCell>
                                        <TableCell align="right">${p.amountReceived.toFixed(2)}</TableCell>
                                        <TableCell align="right">${p.changeGiven.toFixed(2)}</TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </>
            )}
        </Box>
    )
}
