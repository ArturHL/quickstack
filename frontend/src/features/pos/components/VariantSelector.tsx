import { FormControl, FormLabel, RadioGroup, FormControlLabel, Radio, Typography } from '@mui/material'
import type { MenuVariantItem } from '../types/Menu'

interface VariantSelectorProps {
  variants: MenuVariantItem[]
  selectedId: string | null
  onChange: (id: string) => void
}

export default function VariantSelector({ variants, selectedId, onChange }: VariantSelectorProps) {
  return (
    <FormControl component="fieldset">
      <FormLabel component="legend">Variante</FormLabel>
      <RadioGroup
        value={selectedId ?? ''}
        onChange={(e) => onChange(e.target.value)}
      >
        {variants.map((variant) => (
          <FormControlLabel
            key={variant.id}
            value={variant.id}
            control={<Radio />}
            label={
              <Typography variant="body2">
                {variant.name} — ${variant.effectivePrice.toFixed(2)}
              </Typography>
            }
          />
        ))}
      </RadioGroup>
    </FormControl>
  )
}
