import { Component } from 'react'
import type { ErrorInfo, ReactNode } from 'react'
import { Box, Button, Container, Typography, Paper } from '@mui/material'
import { Error as ErrorIcon } from '@mui/icons-material'

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
  error: Error | null
}

export default class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    // Log error to console for debugging and future Sentry integration
    console.error('ErrorBoundary caught an error:', error, errorInfo)
  }

  handleReload = () => {
    window.location.reload()
  }

  render() {
    if (this.state.hasError) {
      return (
        <Container maxWidth="sm" sx={{ mt: 8 }}>
          <Paper elevation={3} sx={{ p: 4 }}>
            <Box
              sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                textAlign: 'center',
              }}
            >
              <ErrorIcon color="error" sx={{ fontSize: 80, mb: 2 }} />

              <Typography variant="h4" component="h1" gutterBottom>
                Ocurrió un error inesperado
              </Typography>

              <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
                Por favor, recarga la página para continuar.
              </Typography>

              <Button
                variant="contained"
                color="primary"
                onClick={this.handleReload}
                size="large"
              >
                Recargar
              </Button>

              {import.meta.env.DEV && this.state.error && (
                <Box
                  sx={{
                    mt: 4,
                    p: 2,
                    bgcolor: 'grey.100',
                    borderRadius: 1,
                    textAlign: 'left',
                    width: '100%',
                    maxHeight: 300,
                    overflow: 'auto',
                  }}
                >
                  <Typography variant="caption" component="div" sx={{ fontFamily: 'monospace' }}>
                    <strong>Error:</strong> {this.state.error.message}
                  </Typography>
                  {this.state.error.stack && (
                    <Typography
                      variant="caption"
                      component="pre"
                      sx={{ mt: 1, fontSize: '0.7rem', whiteSpace: 'pre-wrap' }}
                    >
                      {this.state.error.stack}
                    </Typography>
                  )}
                </Box>
              )}
            </Box>
          </Paper>
        </Container>
      )
    }

    return this.props.children
  }
}
