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

## Estructura de Directorios

```text
frontend/src/
├── components/     # Componentes reutilizables (layout, common)
├── features/       # Feature modules (auth, pos, orders, products)
├── hooks/          # Custom hooks
├── pages/          # Páginas específicas por rol UX
│   ├── admin/      # Vistas de SaaS Dashboard (Owner/Manager)
│   ├── cashier/    # Vistas de Punto de Venta (Terminal)
│   ├── kitchen/    # Tablero KDS (Kanban de tickets)
│   └── waiter/     # Vistas móviles (Mesas, Toma de órdenes)
├── router/         # Configuración de enrutamiento por roles
├── services/       # API clients (Axios)
├── stores/         # Zustand global stores
├── types/          # TypeScript types
└── utils/          # Utilidades compartidas
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

## Estado Actual: Phase 2 (UX Overhaul)

- Enrutamiento basado en roles finalizado (`/waiter`, `/cashier`, `/kitchen`, `/admin`).
- Implementación de perfiles UX independientes en progreso.
- Sistema de diseño de Interfaz (Comanda Edge, Tabular Nums) aplicado.

## Convenciones (a definir)

- Componentes: PascalCase
- Hooks: camelCase con prefix `use`
- Stores: camelCase con suffix `Store`
- API services: camelCase con suffix `Api`
