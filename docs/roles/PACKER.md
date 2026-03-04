# Role: PACKER (Empacador / Armador)

> **Estado:** ⏳ Post-MVP — Este rol no se implementará en la versión inicial del producto. Requiere que el flujo de DELIVERY esté también implementado, ya que el PACKER es el puente hacia él. Se contempla en la misma fase que DELIVERY. Los datos de diseño UX aquí documentados sirven como referencia para planificación futura.

## Descripción General
El `PACKER` es el puente entre la Cocina (`KITCHEN`) y el cliente final (para llevar) o el Repartidor (`DELIVERY`). Se asegura de que la orden esté completa, con todos los complementos (salsas, cubiertos, servilletas), bien empacada y enviada a la persona correcta.

## Responsabilidades Principales
1. **Verificación de Órdenes:** Recibir los platillos recién salidos de la cocina y contrastarlos contra el ticket original de la orden.
2. **Gestión de Extras:** Asegurar la inclusión de bebidas (desde el refrigerador o barra), salsas, postres pre-hechos y utensilios según lo pedido y las políticas del restaurante.
3. **Control de Calidad Final (QA):** Sello final de que la orden es correcta antes de cerrarla o entregarla.
4. **Cambio de Estados:** Marcar la orden de "Cocina Lista" a "Empacada / Lista para Recolección" o "Lista para Repartidor".

## Flujo de Trabajo Ideal (UX Proposal)
- **Punto de Entrada:** `/packer/station`
- **Dispositivo:** Tablet montada en la estación de empaque o monitor mediano.
- **Interfaz:** *Checklist Mode* (Modo Lista de Verificación).
  - Al seleccionar una orden que viene de cocina, la pantalla muestra una estructura tipo "Checklist".
  - Secciones visuales claras que dividen: "Productos de Cocina" vs "Productos de Barra/Extras" vs "Insumos de Empaque (Salsas, etc.)".
  - Opción de escanear un código de barras en el ticket impreso para abrir rápidamente la orden en pantalla.
