import { Box, Typography } from '@mui/material'

export default function KitchenBoard() {
    return (
        <Box>
            <Typography variant="h5" mb={2}>Tablero de Cocina (KDS)</Typography>
            <Typography color="text.secondary">Aquí irán los tickets de las comandas en estilo Kanban.</Typography>
        </Box>
    )
}
