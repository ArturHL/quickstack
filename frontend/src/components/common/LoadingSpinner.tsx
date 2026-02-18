import CircularProgress from '@mui/material/CircularProgress'
import Box from '@mui/material/Box'

const LoadingSpinner = () => (
  <Box display="flex" justifyContent="center" alignItems="center" minHeight="200px">
    <CircularProgress />
  </Box>
)

export default LoadingSpinner
