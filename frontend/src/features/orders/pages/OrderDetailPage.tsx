import { useParams } from 'react-router-dom'
import OrderDetail from '../components/OrderDetail'

export default function OrderDetailPage() {
    const { id } = useParams<{ id: string }>()
    if (!id) return null
    return <OrderDetail orderId={id} />
}
