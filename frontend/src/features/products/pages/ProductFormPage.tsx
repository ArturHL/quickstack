import { useParams } from 'react-router-dom'
import { Box } from '@mui/material'
import ProductForm from '../components/ProductForm'
import ModifierGroupList from '../components/ModifierGroupList'

export default function ProductFormPage() {
  const { id } = useParams<{ id?: string }>()
  return (
    <Box>
      <ProductForm productId={id} />
      {id && <ModifierGroupList productId={id} />}
    </Box>
  )
}
