# QuickStack Frontend

React 19 + Vite + TypeScript + MUI

## Stack

| Librería | Propósito |
|----------|-----------|
| React 19 | UI framework |
| Vite | Build tool |
| TypeScript | Type safety |
| MUI | Component library |
| Zustand | State management |
| TanStack Query | Server state + caching |
| Axios | HTTP client |
| React Router | Routing |

## Estructura (pendiente Phase 0.4)

```
frontend/src/
├── components/     # Componentes reutilizables
├── features/       # Feature modules (auth, pos, inventory)
├── hooks/          # Custom hooks
├── services/       # API clients
├── stores/         # Zustand stores
├── types/          # TypeScript types
└── utils/          # Utilidades
```

## Comandos

```bash
npm install         # Instalar dependencias
npm run dev         # Dev server (localhost:5173)
npm run build       # Build producción
npm run preview     # Preview build
npm run lint        # ESLint
npm run type-check  # TypeScript check
```

## Próximo: Phase 0.4

- Setup inicial de proyecto
- Integración con auth backend
- Login/Register forms
- Protected routes
- Token refresh automático

## Convenciones (a definir)

- Componentes: PascalCase
- Hooks: camelCase con prefix `use`
- Stores: camelCase con suffix `Store`
- API services: camelCase con suffix `Api`
