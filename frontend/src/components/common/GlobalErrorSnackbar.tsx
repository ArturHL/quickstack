import { useEffect, useState } from 'react'
import { Snackbar, Alert } from '@mui/material'
import type { AxiosError } from 'axios'

interface ErrorDetail {
  error: AxiosError
}

export default function GlobalErrorSnackbar() {
  const [open, setOpen] = useState(false)
  const [message, setMessage] = useState('')

  useEffect(() => {
    const handleGlobalQueryError = (event: Event) => {
      const customEvent = event as CustomEvent<ErrorDetail>
      const error = customEvent.detail.error
      const axiosErr = error as AxiosError

      let errorMessage = 'Error del servidor. Intenta de nuevo.'

      if (axiosErr.code === 'ERR_NETWORK') {
        errorMessage = 'Sin conexión a internet.'
      } else if (axiosErr.response?.status && axiosErr.response.status >= 500) {
        errorMessage = 'Error del servidor. Intenta de nuevo.'
      }

      setMessage(errorMessage)
      setOpen(true)
    }

    window.addEventListener('global-query-error', handleGlobalQueryError)

    return () => {
      window.removeEventListener('global-query-error', handleGlobalQueryError)
    }
  }, [])

  const handleClose = (_event?: React.SyntheticEvent | Event, reason?: string) => {
    if (reason === 'clickaway') {
      return
    }
    setOpen(false)
  }

  return (
    <Snackbar
      open={open}
      autoHideDuration={5000}
      onClose={handleClose}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
    >
      <Alert onClose={handleClose} severity="error" sx={{ width: '100%' }}>
        {message}
      </Alert>
    </Snackbar>
  )
}
