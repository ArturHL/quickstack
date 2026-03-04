import { createTheme } from '@mui/material/styles'

const theme = createTheme({
  palette: {
    primary: {
      main: '#18181B',
      light: '#3F3F46',
      dark: '#09090B',
      contrastText: '#FFFFFF',
    },
    secondary: {
      main: '#78716C',
      light: '#A8A29E',
      dark: '#57534E',
      contrastText: '#FFFFFF',
    },
    background: {
      default: '#F5F5F4',
      paper: '#FFFFFF',
    },
    text: {
      primary: '#0C0A09',
      secondary: '#78716C',
      disabled: '#A8A29E',
    },
    divider: 'rgba(0, 0, 0, 0.08)',
    error: { main: '#EF4444' }, // Salsa Red
    warning: { main: '#F59E0B' },
    success: { main: '#10B981' }, // Agave Green
    info: { main: '#0369A1' },
  },
  typography: {
    fontFamily: '"Inter", "Helvetica", "Arial", sans-serif',
    h1: { fontSize: '2rem', fontWeight: 600, letterSpacing: '-0.02em', lineHeight: 1.2 },
    h2: { fontSize: '1.5rem', fontWeight: 600, letterSpacing: '-0.015em', lineHeight: 1.3 },
    h3: { fontSize: '1.25rem', fontWeight: 600, letterSpacing: '-0.01em' },
    h4: { fontSize: '1.125rem', fontWeight: 600, letterSpacing: '-0.01em' },
    h5: { fontSize: '1rem', fontWeight: 600 },
    h6: { fontSize: '0.875rem', fontWeight: 600 },
    body1: { fontSize: '0.9375rem', lineHeight: 1.6 },
    body2: { fontSize: '0.875rem', lineHeight: 1.5 },
    caption: { fontSize: '0.75rem', letterSpacing: '0.02em' },
    button: { textTransform: 'none', fontWeight: 500, fontSize: '0.875rem', letterSpacing: '0' },
    overline: {
      fontSize: '0.6875rem',
      fontWeight: 600,
      letterSpacing: '0.07em',
      textTransform: 'uppercase',
    },
  },
  shape: {
    borderRadius: 6,
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { borderRadius: 6, padding: '6px 14px' },
        contained: {
          backgroundColor: '#18181B',
          color: '#FFFFFF',
          '&:hover': { backgroundColor: '#09090B' },
          '&.Mui-disabled': { backgroundColor: '#D4D4D8', color: '#A1A1AA' },
        },
        outlined: {
          borderColor: 'rgba(0, 0, 0, 0.14)',
          color: '#0C0A09',
          '&:hover': {
            backgroundColor: 'rgba(0, 0, 0, 0.03)',
            borderColor: 'rgba(0, 0, 0, 0.22)',
          },
        },
        text: {
          color: '#0C0A09',
          '&:hover': { backgroundColor: 'rgba(0, 0, 0, 0.04)' },
        },
        sizeLarge: { padding: '10px 20px', fontSize: '0.9375rem' },
      },
    },
    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: {
          border: '1px solid rgba(0, 0, 0, 0.08)',
          borderRadius: 8,
          backgroundImage: 'none',
        },
      },
    },
    MuiPaper: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: { backgroundImage: 'none' },
        outlined: { borderColor: 'rgba(0, 0, 0, 0.08)' },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: { borderRadius: 4, fontWeight: 500, fontSize: '0.75rem' },
      },
    },
    MuiTextField: {
      defaultProps: { variant: 'outlined', size: 'small' },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          backgroundColor: '#FAFAF9',
          '& .MuiOutlinedInput-notchedOutline': { borderColor: 'rgba(0, 0, 0, 0.13)' },
          '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: 'rgba(0, 0, 0, 0.26)' },
          '&.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: '#18181B', borderWidth: 1.5 },
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: { borderColor: 'rgba(0, 0, 0, 0.07)', fontSize: '0.875rem' },
        head: {
          fontWeight: 600,
          color: '#78716C',
          fontSize: '0.6875rem',
          textTransform: 'uppercase',
          letterSpacing: '0.07em',
          backgroundColor: '#FAFAF9',
        },
      },
    },
    MuiDivider: {
      styleOverrides: {
        root: { borderColor: 'rgba(0, 0, 0, 0.08)' },
      },
    },
    MuiTab: {
      styleOverrides: {
        root: { textTransform: 'none', fontWeight: 500, fontSize: '0.875rem', minHeight: 44 },
      },
    },
    MuiTabs: {
      styleOverrides: {
        indicator: { height: 2, backgroundColor: '#18181B' },
      },
    },
    MuiAlert: {
      styleOverrides: {
        root: { borderRadius: 6, fontSize: '0.875rem' },
      },
    },
    MuiAppBar: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: {
          backgroundColor: '#FFFFFF',
          borderBottom: '1px solid rgba(0, 0, 0, 0.08)',
          color: '#0C0A09',
        },
      },
    },
    MuiSelect: {
      styleOverrides: {
        root: { backgroundColor: '#FAFAF9' },
      },
    },
    MuiMenuItem: {
      styleOverrides: {
        root: {
          fontSize: '0.875rem',
          '&.Mui-selected': { backgroundColor: 'rgba(0,0,0,0.05)' },
        },
      },
    },
    MuiSnackbar: {
      defaultProps: {
        anchorOrigin: { vertical: 'bottom', horizontal: 'center' },
      },
    },
  },
})

export default theme
