# QuickStack POS - Roadmap del MVP

> **Última actualización:** 2026-01-26
> **Estado:** Phase 0 - En planificación

## Vision Summary

Sistema de punto de venta multi-sucursal con integración WhatsApp/IA que permita a restaurantes mexicanos gestionar pedidos, ventas y reportes básicos, validado con un restaurante piloto antes del lanzamiento comercial.

---

## Stack Tecnológico

| Componente | Tecnología | Hosting |
|------------|------------|---------|
| Frontend | React + Vite | Vercel |
| Backend | Java Spring Boot | Render (Docker) |
| Base de datos | PostgreSQL | Neon |
| Autenticación | Auth0 | - |
| Automatizaciones | n8n | Self-hosted o Cloud |

---

## Resumen de Fases

| Fase | Nombre | Objetivo | Estado |
|------|--------|----------|--------|
| 0 | Foundation | Auth0 + BD + Deploy básico | 🔄 En progreso |
| 1 | Core POS | Crear pedidos con productos simples | ⏳ Pendiente |
| 2 | Modifiers & Combos | Personalización completa del menú | ⏳ Pendiente |
| 3 | Printing | Tickets en impresora térmica | ⏳ Pendiente |
| 4 | Reporting | Dashboard de ventas día/semana/mes | ⏳ Pendiente |
| 5 | WhatsApp Bot | Pedidos con IA integrados al POS | ⏳ Pendiente |
| 6 | Polish | Bugs, UX, onboarding, validación final | ⏳ Pendiente |

---

## Critical Path

```
Phase 0 → Phase 1 → Phase 2 → Phase 3 → Phase 6
```

> Phases 4 y 5 pueden desarrollarse en paralelo si es necesario acelerar.

---

## Phase 0: Foundation & Architecture

**Goal**: Establecer la arquitectura base, decisiones técnicas fundamentales y ambiente de desarrollo.

### Decisiones Técnicas

- [ ] Multi-tenancy: BD compartida con `tenant_id`
- [ ] Monorepo vs. repos separados
- [ ] ORM: JPA/Hibernate vs. SQL nativo
- [ ] State management: Redux vs. Zustand vs. Context API

### Entregables

- [ ] Repositorios configurados
- [ ] Base de datos PostgreSQL en Neon con esquema inicial
- [ ] Backend Spring Boot con estructura base
- [ ] Frontend React+Vite con routing y estructura
- [ ] Auth0 integrado con login/logout funcional
- [ ] CI/CD pipeline básico (Vercel/Render)
- [ ] Variables de entorno y secrets management
- [ ] Documentación de arquitectura técnica

### Success Criteria

- Usuario puede hacer login con Auth0 y ver dashboard vacío
- Backend responde a health check endpoint
- Database schema permite multi-tenancy
- Deploys automáticos funcionan desde git push

---

## Phase 1: Core POS - Ventas Básicas

**Goal**: Crear y completar pedidos simples con productos básicos.

**Dependencies**: Phase 0 completado

### Entregables

- [ ] CRUD productos simples (nombre, precio, categoría)
- [ ] Pantalla de punto de venta (selección de productos, carrito)
- [ ] Crear pedido con múltiples productos y cantidades
- [ ] Calcular totales (subtotal, total)
- [ ] Registrar pago en efectivo
- [ ] Cerrar pedido y guardar en BD
- [ ] Vista de pedidos del día
- [ ] Multi-sucursal: selector de sucursal activa
- [ ] CRUD de sucursales (admin only)

### Success Criteria

- Cajero puede completar venta de 3+ productos en <60 segundos
- Sistema mantiene historial de pedidos por sucursal

### Validation Checkpoint

- **Demo con piloto**: Mostrar flujo de venta básico
- **Pregunta clave**: ¿El flujo es más rápido que su método actual?

---

## Phase 2: Modifiers & Combos

**Goal**: Agregar modificadores y combos para personalización de productos.

**Dependencies**: Phase 1 validado con piloto

### Entregables

- [ ] Modelo de datos para modificadores
- [ ] Asociar modificadores a productos
- [ ] UI para seleccionar modificadores al agregar al carrito
- [ ] Modelo de datos para combos
- [ ] CRUD de combos con selección de componentes
- [ ] Lógica de pricing para combos
- [ ] Actualizar cálculo de totales

### Success Criteria

- Cajero puede agregar producto con 2+ modificadores en <10 segundos
- Sistema soporta combos de 3+ componentes

---

## Phase 3: Printing & Receipt Generation

**Goal**: Generar e imprimir tickets de venta.

**Dependencies**: Phase 2 completado

### Decisiones Técnicas

- [ ] Estrategia de impresión (browser print API vs. backend service)
- [ ] Formato de ticket (ESC/POS vs. PDF)

### Entregables

- [ ] Diseño de template de ticket
- [ ] Generación de ticket en formato ESC/POS o PDF
- [ ] Integración con impresora térmica
- [ ] Botón "Imprimir ticket" después de cerrar pedido
- [ ] Opción de reimprimir tickets históricos
- [ ] Configuración de impresora por sucursal

### Success Criteria

- Ticket se imprime automáticamente al cerrar pedido
- Ticket incluye: fecha, hora, sucursal, items, modificadores, total

---

## Phase 4: Basic Reporting

**Goal**: Proveer reportes básicos de ventas.

**Dependencies**: Phase 3 completado, datos reales generándose

### Entregables

- [ ] Dashboard con métricas del día
- [ ] Reporte de ventas por rango de fechas
- [ ] Reporte de ventas por sucursal
- [ ] Reporte de productos más vendidos (top 10)
- [ ] Filtros básicos (sucursal, rango de fechas)
- [ ] Exportar reportes a CSV/Excel
- [ ] Gráficas simples

### Success Criteria

- Admin puede ver ventas totales del día en <5 segundos
- Exportación funciona con dataset de 1000+ pedidos

---

## Phase 5: WhatsApp Bot with AI

**Goal**: Pedidos vía WhatsApp con lenguaje natural integrados al POS.

**Dependencies**: Phase 2 completado (productos/modificadores/combos existen)

### Decisiones Técnicas

- [ ] Proveedor WhatsApp Business API (Meta Cloud API recomendado)
- [ ] Modelo de IA (OpenAI GPT-4o-mini recomendado)
- [ ] Estrategia de context management

### Entregables

- [ ] Setup WhatsApp Business API
- [ ] n8n workflow: recibir mensaje → IA → responder
- [ ] Integración IA: parsear intención del cliente
- [ ] Crear pedido en POS desde WhatsApp
- [ ] Confirmación al cliente con resumen
- [ ] Manejo de casos edge
- [ ] Configurar horarios de atención por sucursal
- [ ] Admin: ver conversaciones y pedidos de WhatsApp

### Success Criteria

- Bot entiende 80%+ de pedidos simples en lenguaje natural
- Pedidos de bot aparecen en POS marcados como "WhatsApp"
- Bot responde en <10 segundos

---

## Phase 6: Polish & Pilot Validation

**Goal**: Refinar producto y preparar para onboarding de más clientes.

**Dependencies**: Phases 1-5 completados

### Entregables

- [ ] Fixing de bugs críticos
- [ ] Mejoras de UX basadas en uso real
- [ ] Onboarding flow para nuevos restaurantes
- [ ] Documentación de usuario
- [ ] Optimizaciones de performance
- [ ] Testing de carga básico
- [ ] Backup y recovery plan
- [ ] Monitoring básico

### Success Criteria

- Piloto ha operado 100% con QuickStack durante 2+ semanas
- Cero pérdida de datos
- Tiempo de setup para nuevo restaurante <2 horas

---

## Checkpoints de Validación con Piloto

| Milestone | Métrica de Éxito | Señal de Alerta |
|-----------|-----------------|-----------------|
| Post-Phase 1 | Piloto usa POS para ≥50% de ventas | Rechazo del sistema |
| Post-Phase 2 | 100% de productos en el sistema | Workarounds manuales |
| Post-Phase 3 | Cero quejas sobre tickets | Clientes piden otros comprobantes |
| Post-Phase 4 | Dueño revisa reportes ≥3 veces/semana | Reportes ignorados |
| Post-Phase 5 | ≥20% de pedidos de WhatsApp | Cero adopción después de 2 semanas |
| Post-Phase 6 | Piloto acepta pagar ≥$500 MXN/mes | Rechazo de pago |

---

## Features Explícitamente Fuera del MVP

- Pagos con tarjeta
- Gestión de inventario
- Roles avanzados (meseros, cocineros)
- Delivery/takeout management
- Programas de lealtad
- Facturación electrónica (CFDI)
- Multi-idioma
- App móvil nativa

---

## Risk Matrix

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|---------|------------|
| Free tiers se agotan | Media | Alto | Monitoreo diario, upgrade plan ready |
| Piloto abandona proyecto | Baja | Crítico | Comunicación constante |
| Impresión no funciona | Media | Alto | Validar hardware ANTES |
| Bot de IA no es preciso | Alta | Medio | Scope reducido, fallback a humano |
| Performance issues | Media | Alto | Load testing, indexing |

---

## Changelog

### 2026-01-26
- Creación inicial del roadmap
- Definición de 6 fases del MVP
- Identificación de decisiones técnicas pendientes
