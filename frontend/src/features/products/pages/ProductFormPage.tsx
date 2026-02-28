import { useParams } from 'react-router-dom'
import ProductForm from '../components/ProductForm'

export default function ProductFormPage() {
  const { id } = useParams<{ id?: string }>()
  return <ProductForm productId={id} />
}
