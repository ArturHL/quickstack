import { useState } from 'react'
import { Box, Tab, Tabs, Typography } from '@mui/material'
import BranchList from '../components/BranchList'
import AreaList from '../components/AreaList'
import TableList from '../components/TableList'
import { useBranchesQuery } from '../hooks/useBranchesQuery'
import { useAreasQuery } from '../hooks/useAreasQuery'

export default function BranchListPage() {
  const [selectedBranchId, setSelectedBranchId] = useState<string | null>(null)
  const [selectedAreaId, setSelectedAreaId] = useState<string | null>(null)

  const { data: branches } = useBranchesQuery()
  const { data: areas } = useAreasQuery(selectedBranchId)

  return (
    <Box>
      <Typography variant="h4" mb={3}>Gestión de Sucursales</Typography>

      <BranchList />

      {branches && branches.length > 0 && (
        <Box mt={4}>
          <Typography variant="h6" mb={2}>Áreas y Mesas</Typography>
          <Tabs
            value={selectedBranchId ?? false}
            onChange={(_e, val) => { setSelectedBranchId(val); setSelectedAreaId(null) }}
            variant="scrollable"
          >
            {branches.map((b) => (
              <Tab key={b.id} label={b.name} value={b.id} />
            ))}
          </Tabs>

          {selectedBranchId && (
            <Box mt={2}>
              <AreaList branchId={selectedBranchId} />

              {areas && areas.length > 0 && (
                <Box mt={3}>
                  <Tabs
                    value={selectedAreaId ?? false}
                    onChange={(_e, val) => setSelectedAreaId(val)}
                    variant="scrollable"
                  >
                    {areas.map((a) => (
                      <Tab key={a.id} label={a.name} value={a.id} />
                    ))}
                  </Tabs>

                  {selectedAreaId && (
                    <Box mt={2}>
                      <TableList areaId={selectedAreaId} />
                    </Box>
                  )}
                </Box>
              )}
            </Box>
          )}
        </Box>
      )}
    </Box>
  )
}
