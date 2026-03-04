import { useState, useEffect } from 'react'
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    Chip,
    CircularProgress,
    Divider,
    List,
    Snackbar,
    TextField,
    ToggleButton,
    ToggleButtonGroup,
    Typography,
} from '@mui/material'
import { CheckCircleOutline, ShoppingCartOutlined } from '@mui/icons-material'
import ProductCatalog from '../../features/pos/components/ProductCatalog'
import CartItemComponent from '../../features/pos/components/CartItem'
import PaymentForm from '../../features/pos/components/PaymentForm'
import { useCartStore, selectSubtotal, selectTax, selectTotal } from '../../features/pos/stores/cartStore'
import { useBranchStore } from '../../features/pos/stores/branchStore'
import { useCreateOrderMutation } from '../../features/pos/hooks/useCreateOrderMutation'
import { useSubmitOrderMutation } from '../../features/pos/hooks/useSubmitOrderMutation'
import { useMarkReadyMutation } from '../../features/pos/hooks/useMarkReadyMutation'
import { useRegisterPaymentMutation } from '../../features/pos/hooks/useRegisterPaymentMutation'
import { useCustomersQuery } from '../../features/pos/hooks/useCustomersQuery'
import { useCreateCustomerMutation } from '../../features/pos/hooks/useCreateCustomerMutation'
import { customerAdminApi } from '../../features/customers/api/customerApi'
import { buildOrderRequest } from '../../features/pos/utils/orderUtils'
import type { ServiceType } from '../../features/pos/types/Cart'
import type { CustomerResponse } from '../../features/pos/types/Customer'

type Phase = 'selling' | 'paying' | 'done'

// ─── CashierCartPanel ────────────────────────────────────────────────────────

interface CashierCartPanelProps {
    onOrderReady: (orderId: string, total: number) => void
}

function CashierCartPanel({ onOrderReady }: CashierCartPanelProps) {
    const [errorMsg, setErrorMsg] = useState<string | null>(null)

    // Customer state
    const [phoneInput, setPhoneInput] = useState('')
    const [debouncedPhone, setDebouncedPhone] = useState('')
    const [customerForm, setCustomerForm] = useState({
        name: '',
        addressLine1: '',
        addressLine2: '',
        city: '',
        deliveryNotes: '',
    })
    const [resolvedCustomerId, setResolvedCustomerId] = useState<string | null>(null)

    // Debounce phone input
    useEffect(() => {
        const t = setTimeout(() => setDebouncedPhone(phoneInput), 300)
        return () => clearTimeout(t)
    }, [phoneInput])

    // Search customers by phone (min 3 chars)
    const searchPhone = debouncedPhone.length >= 3 ? debouncedPhone : ''
    const { data: searchData } = useCustomersQuery(searchPhone)
    const foundCustomers = searchData?.content ?? []

    // Cart
    const items = useCartStore((s) => s.items)
    const serviceType = useCartStore((s) => s.serviceType)
    const tableId = useCartStore((s) => s.tableId)
    const clearCart = useCartStore((s) => s.clearCart)
    const updateQuantity = useCartStore((s) => s.updateQuantity)
    const removeItem = useCartStore((s) => s.removeItem)
    const setServiceDetails = useCartStore((s) => s.setServiceDetails)
    const subtotal = useCartStore(selectSubtotal)
    const tax = useCartStore(selectTax)
    const total = useCartStore(selectTotal)

    const activeBranchId = useBranchStore((s) => s.activeBranchId)

    const createOrder = useCreateOrderMutation()
    const submitOrder = useSubmitOrderMutation()
    const markReady = useMarkReadyMutation()
    const createCustomer = useCreateCustomerMutation()

    const isSending =
        createOrder.isPending ||
        submitOrder.isPending ||
        markReady.isPending ||
        createCustomer.isPending

    // Inicializar serviceType en COUNTER si no está establecido
    useEffect(() => {
        if (!serviceType) setServiceDetails('COUNTER')
    }, [serviceType, setServiceDetails])

    const handleServiceTypeChange = (_: React.SyntheticEvent, value: ServiceType | null) => {
        if (value) setServiceDetails(value)
    }

    const handleSelectCustomer = (customer: CustomerResponse) => {
        setPhoneInput(customer.phone ?? '')
        setCustomerForm({
            name: customer.name ?? '',
            addressLine1: customer.addressLine1 ?? '',
            addressLine2: customer.addressLine2 ?? '',
            city: customer.city ?? '',
            deliveryNotes: customer.deliveryNotes ?? '',
        })
        setResolvedCustomerId(customer.id)
    }

    // Teléfono válido = vacío (opcional en COUNTER) O exactamente 10 dígitos
    const phoneDigits = phoneInput.replace(/\D/g, '')
    const isPhoneValid = phoneInput.trim() === '' || phoneDigits.length === 10

    const isCustomerValid =
        customerForm.name.trim().length > 0 &&
        isPhoneValid &&
        (serviceType !== 'TAKEOUT' ||
            (phoneInput.trim().length > 0 && customerForm.addressLine1.trim().length > 0))

    const handleEnviarOrden = async () => {
        if (!serviceType || !activeBranchId || items.length === 0 || !isCustomerValid) return

        try {
            let finalCustomerId: string | null = resolvedCustomerId

            if (finalCustomerId) {
                // Cliente existente: sincronizar datos actualizados del formulario
                await customerAdminApi.updateCustomer(finalCustomerId, {
                    name: customerForm.name.trim() || undefined,
                    phone: phoneInput.trim() || undefined,
                    addressLine1: customerForm.addressLine1.trim() || undefined,
                    addressLine2: customerForm.addressLine2.trim() || undefined,
                    city: customerForm.city.trim() || undefined,
                    deliveryNotes: customerForm.deliveryNotes.trim() || undefined,
                })
            } else if (phoneInput.trim()) {
                // Teléfono sin cliente resuelto → crear cliente nuevo
                const newCustomer = await createCustomer.mutateAsync({
                    name: customerForm.name.trim() || undefined,
                    phone: phoneInput.trim(),
                    addressLine1: customerForm.addressLine1.trim() || undefined,
                    addressLine2: customerForm.addressLine2.trim() || undefined,
                    city: customerForm.city.trim() || undefined,
                    deliveryNotes: customerForm.deliveryNotes.trim() || undefined,
                })
                finalCustomerId = newCustomer.id
            }

            const baseRequest = buildOrderRequest(
                { items, serviceType, tableId, customerId: finalCustomerId },
                activeBranchId
            )

            // COUNTER sin teléfono: poner nombre en notes como referencia
            const request = {
                ...baseRequest,
                notes: !finalCustomerId && customerForm.name.trim()
                    ? customerForm.name.trim()
                    : undefined,
            }

            const order = await createOrder.mutateAsync(request)
            const submitted = await submitOrder.mutateAsync(order.id)
            await markReady.mutateAsync(submitted.id)
            onOrderReady(submitted.id, order.total)
        } catch {
            setErrorMsg('Error al enviar la orden. Intente de nuevo.')
        }
    }

    return (
        <Box display="flex" flexDirection="column" height="100%" overflow="hidden">
            {/* Service type toggle */}
            <Box px={2} pt={2} pb={1}>
                <ToggleButtonGroup
                    value={serviceType ?? 'COUNTER'}
                    exclusive
                    onChange={handleServiceTypeChange}
                    size="small"
                    fullWidth
                    aria-label="tipo de servicio"
                >
                    <ToggleButton value="COUNTER" aria-label="mostrador">Mostrador</ToggleButton>
                    <ToggleButton value="TAKEOUT" aria-label="para llevar">Para llevar</ToggleButton>
                </ToggleButtonGroup>
            </Box>

            {/* Customer section + Items — scrollable */}
            <Box flexGrow={1} overflow="auto">
                {/* Customer section */}
                <Box px={2} pb={1}>
                    <Typography
                        variant="caption"
                        color="text.secondary"
                        fontWeight={600}
                        sx={{ textTransform: 'uppercase', letterSpacing: 0.5 }}
                    >
                        Cliente
                    </Typography>

                    <TextField
                        label="Teléfono"
                        value={phoneInput}
                        onChange={(e) => setPhoneInput(e.target.value)}
                        fullWidth
                        size="small"
                        sx={{ mt: 1 }}
                        error={!isPhoneValid}
                        helperText={!isPhoneValid ? 'Debe tener 10 dígitos' : undefined}
                        inputProps={{ inputMode: 'numeric', 'aria-label': 'teléfono del cliente' }}
                    />

                    {/* Clientes encontrados */}
                    {foundCustomers.length > 0 && !resolvedCustomerId && (
                        <Box
                            mt={0.5}
                            border={1}
                            borderColor="divider"
                            borderRadius={1}
                            overflow="hidden"
                        >
                            {foundCustomers.slice(0, 3).map((c, i) => (
                                <Box
                                    key={c.id}
                                    display="flex"
                                    alignItems="center"
                                    justifyContent="space-between"
                                    px={1.5}
                                    py={0.75}
                                    sx={{
                                        borderTop: i > 0 ? 1 : 0,
                                        borderColor: 'divider',
                                        bgcolor: 'background.paper',
                                    }}
                                >
                                    <Box>
                                        <Typography variant="body2" fontWeight={600}>
                                            {c.name ?? '—'}
                                        </Typography>
                                        {c.addressLine1 && (
                                            <Typography variant="caption" color="text.secondary">
                                                {c.addressLine1}
                                            </Typography>
                                        )}
                                    </Box>
                                    <Button
                                        size="small"
                                        variant="outlined"
                                        onClick={() => handleSelectCustomer(c)}
                                        aria-label={`usar datos de ${c.name}`}
                                    >
                                        Usar
                                    </Button>
                                </Box>
                            ))}
                        </Box>
                    )}

                    {/* Indicator: cliente vinculado */}
                    {resolvedCustomerId && (
                        <Box mt={0.5}>
                            <Chip
                                icon={<CheckCircleOutline />}
                                label="Cliente vinculado"
                                color="success"
                                size="small"
                                variant="outlined"
                                onDelete={() => setResolvedCustomerId(null)}
                            />
                        </Box>
                    )}

                    <TextField
                        label="Nombre *"
                        value={customerForm.name}
                        onChange={(e) => setCustomerForm((f) => ({ ...f, name: e.target.value }))}
                        fullWidth
                        size="small"
                        sx={{ mt: 1 }}
                        error={customerForm.name.trim().length === 0}
                        inputProps={{ 'aria-label': 'nombre del cliente' }}
                    />

                    {/* Campos adicionales para TAKEOUT */}
                    {serviceType === 'TAKEOUT' && (
                        <>
                            <TextField
                                label="Calle y Núm. Ext. *"
                                value={customerForm.addressLine1}
                                onChange={(e) => setCustomerForm((f) => ({ ...f, addressLine1: e.target.value }))}
                                fullWidth
                                size="small"
                                sx={{ mt: 1 }}
                                error={customerForm.addressLine1.trim().length === 0}
                                inputProps={{ 'aria-label': 'calle y número exterior' }}
                            />
                            <Box display="flex" gap={1} mt={1}>
                                <TextField
                                    label="Núm. Int."
                                    value={customerForm.addressLine2}
                                    onChange={(e) => setCustomerForm((f) => ({ ...f, addressLine2: e.target.value }))}
                                    size="small"
                                    sx={{ flex: 1 }}
                                    inputProps={{ 'aria-label': 'número interior' }}
                                />
                                <TextField
                                    label="Ciudad"
                                    value={customerForm.city}
                                    onChange={(e) => setCustomerForm((f) => ({ ...f, city: e.target.value }))}
                                    size="small"
                                    sx={{ flex: 2 }}
                                    inputProps={{ 'aria-label': 'ciudad' }}
                                />
                            </Box>
                            <TextField
                                label="Notas del pedido"
                                value={customerForm.deliveryNotes}
                                onChange={(e) => setCustomerForm((f) => ({ ...f, deliveryNotes: e.target.value }))}
                                fullWidth
                                size="small"
                                multiline
                                rows={2}
                                sx={{ mt: 1 }}
                                placeholder="Ej: Sin cebolla, tocar el timbre..."
                                inputProps={{ 'aria-label': 'notas del pedido' }}
                            />
                        </>
                    )}
                </Box>

                <Divider />

                {/* Items list */}
                {items.length === 0 ? (
                    <Box
                        display="flex"
                        flexDirection="column"
                        alignItems="center"
                        justifyContent="center"
                        py={4}
                        gap={1}
                    >
                        <ShoppingCartOutlined sx={{ fontSize: 48, color: 'text.disabled' }} />
                        <Typography variant="body2" color="text.secondary">
                            Selecciona productos del catálogo
                        </Typography>
                    </Box>
                ) : (
                    <List disablePadding>
                        {items.map((item, i) => (
                            <CartItemComponent
                                key={i}
                                item={item}
                                index={i}
                                onUpdateQty={updateQuantity}
                                onRemove={removeItem}
                            />
                        ))}
                    </List>
                )}
            </Box>

            {/* Totals */}
            {items.length > 0 && (
                <Box px={2} pb={1}>
                    <Divider sx={{ mb: 1 }} />
                    <Card variant="outlined" className="comanda-edge comanda-edge-warning">
                        <CardContent sx={{ py: 1, '&:last-child': { pb: 1 } }}>
                            <Box display="flex" justifyContent="space-between">
                                <Typography variant="body2">Subtotal</Typography>
                                <Typography variant="body2" className="tabular-nums">
                                    ${subtotal.toFixed(2)}
                                </Typography>
                            </Box>
                            <Box display="flex" justifyContent="space-between">
                                <Typography variant="body2">IVA (16%)</Typography>
                                <Typography variant="body2" className="tabular-nums">
                                    ${tax.toFixed(2)}
                                </Typography>
                            </Box>
                            <Divider sx={{ my: 0.5 }} />
                            <Box display="flex" justifyContent="space-between">
                                <Typography variant="subtitle1" fontWeight={700}>Total</Typography>
                                <Typography
                                    variant="subtitle1"
                                    fontWeight={700}
                                    className="tabular-nums"
                                    aria-label="total"
                                >
                                    ${total.toFixed(2)}
                                </Typography>
                            </Box>
                        </CardContent>
                    </Card>
                </Box>
            )}

            {/* Actions */}
            <Box px={2} pb={2} display="flex" flexDirection="column" gap={1}>
                {items.length > 0 && (
                    <Button variant="outlined" color="error" onClick={clearCart} fullWidth size="small">
                        Limpiar Carrito
                    </Button>
                )}
                <Button
                    variant="contained"
                    size="large"
                    fullWidth
                    disabled={isSending || items.length === 0 || !activeBranchId || !isCustomerValid}
                    onClick={handleEnviarOrden}
                    startIcon={isSending ? <CircularProgress size={18} color="inherit" /> : undefined}
                >
                    {isSending ? 'Enviando...' : 'Enviar Orden'}
                </Button>
            </Box>

            <Snackbar
                open={!!errorMsg}
                autoHideDuration={4000}
                onClose={() => setErrorMsg(null)}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
            >
                <Alert severity="error" onClose={() => setErrorMsg(null)}>
                    {errorMsg}
                </Alert>
            </Snackbar>
        </Box>
    )
}

// ─── CashierPaymentPanel ─────────────────────────────────────────────────────

interface CashierPaymentPanelProps {
    orderId: string
    orderTotal: number
    onDone: () => void
}

function CashierPaymentPanel({ orderId, orderTotal, onDone }: CashierPaymentPanelProps) {
    const clearCart = useCartStore((s) => s.clearCart)
    const payment = useRegisterPaymentMutation()

    const handlePayment = async (amount: number) => {
        await payment.mutateAsync({ orderId, paymentMethod: 'CASH', amount })
        clearCart()
        onDone()
    }

    return (
        <Box display="flex" flexDirection="column" height="100%" overflow="auto" p={2}>
            <Typography variant="h6" gutterBottom>Cobrar</Typography>
            <PaymentForm
                orderTotal={orderTotal}
                onSubmit={handlePayment}
                isLoading={payment.isPending}
            />
        </Box>
    )
}

// ─── CashierConfirmation ─────────────────────────────────────────────────────

interface CashierConfirmationProps {
    onNewSale: () => void
}

function CashierConfirmation({ onNewSale }: CashierConfirmationProps) {
    return (
        <Box
            display="flex"
            flexDirection="column"
            alignItems="center"
            justifyContent="center"
            height="100%"
            gap={2}
            p={4}
        >
            <CheckCircleOutline sx={{ fontSize: 80, color: 'success.main' }} />
            <Typography variant="h6">Pedido Completado</Typography>
            <Typography variant="body2" color="text.secondary" align="center">
                El pago fue registrado exitosamente.
            </Typography>
            <Button variant="contained" size="large" onClick={onNewSale} fullWidth>
                Nueva Venta
            </Button>
        </Box>
    )
}

// ─── CashierPos (main) ───────────────────────────────────────────────────────

export default function CashierPos() {
    const [phase, setPhase] = useState<Phase>('selling')
    const [orderId, setOrderId] = useState<string | null>(null)
    const [orderTotal, setOrderTotal] = useState<number>(0)
    const [saleKey, setSaleKey] = useState(0)

    const handleOrderReady = (id: string, total: number) => {
        setOrderId(id)
        setOrderTotal(total)
        setPhase('paying')
    }

    const handlePaymentDone = () => setPhase('done')

    const handleNewSale = () => {
        setPhase('selling')
        setOrderId(null)
        setOrderTotal(0)
        setSaleKey((k) => k + 1) // remonta CashierCartPanel → resetea el formulario de cliente
    }

    return (
        <Box sx={{ display: 'flex', flexGrow: 1, height: '100%', overflow: 'hidden' }}>
            {/* Left: Product Catalog */}
            <Box sx={{ flex: 1, overflow: 'auto', borderRight: 1, borderColor: 'divider', p: 2 }}>
                <ProductCatalog onProductAdded={() => {}} />
            </Box>

            {/* Right: Dynamic panel */}
            <Box
                sx={{
                    width: 360,
                    flexShrink: 0,
                    display: 'flex',
                    flexDirection: 'column',
                    overflow: 'hidden',
                    bgcolor: 'var(--surface-card, background.paper)',
                }}
            >
                {phase === 'selling' && (
                    <CashierCartPanel key={saleKey} onOrderReady={handleOrderReady} />
                )}
                {phase === 'paying' && (
                    <CashierPaymentPanel
                        orderId={orderId!}
                        orderTotal={orderTotal}
                        onDone={handlePaymentDone}
                    />
                )}
                {phase === 'done' && (
                    <CashierConfirmation onNewSale={handleNewSale} />
                )}
            </Box>
        </Box>
    )
}
