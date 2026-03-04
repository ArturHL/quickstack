import { useEffect } from 'react'
import { FormControl, InputLabel, MenuItem, Select, Skeleton } from '@mui/material'
import { useBranchesQuery } from '../hooks/useBranchesQuery'
import { useBranchStore } from '../../pos/stores/branchStore'
import { useAuthStore } from '../../../stores/authStore'

export default function BranchSelector() {
  const { data: branches, isLoading } = useBranchesQuery()
  const activeBranchId = useBranchStore((s) => s.activeBranchId)
  const setActiveBranchId = useBranchStore((s) => s.setActiveBranchId)
  const user = useAuthStore((s) => s.user)
  const isOwner = user?.role === 'OWNER'

  // Auto-select when only one branch (skip for OWNER — they start in global view)
  useEffect(() => {
    if (branches?.length === 1 && !activeBranchId && !isOwner) {
      setActiveBranchId(branches[0].id)
    }
  }, [branches, activeBranchId, setActiveBranchId, isOwner])

  if (isLoading) {
    return <Skeleton variant="rounded" width={180} height={32} sx={{ mx: 1 }} />
  }

  if (!branches?.length) return null

  return (
    <FormControl size="small" sx={{ minWidth: { xs: 130, sm: 180 } }}>
      <InputLabel id="branch-selector-label" sx={{ color: 'inherit' }}>
        Sucursal
      </InputLabel>
      <Select
        labelId="branch-selector-label"
        label="Sucursal"
        value={activeBranchId ?? ''}
        onChange={(e) => setActiveBranchId(e.target.value || null)}
        sx={{ color: 'inherit', '.MuiOutlinedInput-notchedOutline': { borderColor: 'rgba(255,255,255,0.5)' } }}
        inputProps={{ 'aria-label': 'selector de sucursal' }}
      >
        {isOwner && (
          <MenuItem value=""><em>Todas las sucursales</em></MenuItem>
        )}
        {branches.map((b) => (
          <MenuItem key={b.id} value={b.id}>{b.name}</MenuItem>
        ))}
      </Select>
    </FormControl>
  )
}
