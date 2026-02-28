import {
  FormControl,
  FormLabel,
  FormGroup,
  FormControlLabel,
  Checkbox,
  RadioGroup,
  Radio,
  Chip,
  Typography,
  Box,
} from '@mui/material'
import type { MenuModifierGroupItem } from '../types/Menu'

interface ModifierGroupProps {
  group: MenuModifierGroupItem
  selectedIds: string[]
  onChange: (ids: string[]) => void
}

function formatPriceAdj(adj: number): string {
  if (adj > 0) return ` (+$${adj.toFixed(2)})`
  if (adj < 0) return ` (-$${Math.abs(adj).toFixed(2)})`
  return ''
}

export default function ModifierGroup({ group, selectedIds, onChange }: ModifierGroupProps) {
  const isRadio = group.maxSelections === 1
  const isMaxReached = group.maxSelections !== null && selectedIds.length >= group.maxSelections

  const handleCheckboxChange = (modId: string, checked: boolean) => {
    if (checked) {
      onChange([...selectedIds, modId])
    } else {
      onChange(selectedIds.filter((id) => id !== modId))
    }
  }

  return (
    <FormControl component="fieldset" sx={{ mb: 2 }}>
      <Box display="flex" alignItems="center" gap={1} mb={0.5}>
        <FormLabel component="legend">{group.name}</FormLabel>
        {group.isRequired && (
          <Chip label="Requerido" size="small" color="primary" />
        )}
        {group.maxSelections && group.maxSelections > 1 && (
          <Typography variant="caption" color="text.secondary">
            Máx. {group.maxSelections}
          </Typography>
        )}
      </Box>

      {isRadio ? (
        <RadioGroup
          value={selectedIds[0] ?? ''}
          onChange={(e) => onChange([e.target.value])}
        >
          {group.modifiers.map((mod) => (
            <FormControlLabel
              key={mod.id}
              value={mod.id}
              control={<Radio />}
              label={`${mod.name}${formatPriceAdj(mod.priceAdjustment)}`}
            />
          ))}
        </RadioGroup>
      ) : (
        <FormGroup>
          {group.modifiers.map((mod) => {
            const isChecked = selectedIds.includes(mod.id)
            const isDisabled = !isChecked && isMaxReached
            return (
              <FormControlLabel
                key={mod.id}
                control={
                  <Checkbox
                    checked={isChecked}
                    disabled={isDisabled}
                    onChange={(e) => handleCheckboxChange(mod.id, e.target.checked)}
                  />
                }
                label={`${mod.name}${formatPriceAdj(mod.priceAdjustment)}`}
              />
            )
          })}
        </FormGroup>
      )}
    </FormControl>
  )
}
