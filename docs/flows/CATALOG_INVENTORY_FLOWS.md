# Flujos de Catálogo e Inventario

Este documento ilustra la interacción para la gestión del catálogo de productos (creación de categorías, productos, modificadores) y su impacto básico en el inventario.

## 1. Creación de un Producto en el Catálogo

Flujo clásico REST para la capa de administración o Backoffice.

```mermaid
%%{init: {"themeVariables": {"fontFamily": "arial"}}}%%
sequenceDiagram
    actor Admin as Administrador (React)
    participant API as productApi.ts (Axios)
    participant Ctrl as ProductController.java
    participant Svc as ProductService.java
    participant CRepo as CategoryRepository.java
    participant PRepo as ProductRepository.java
    participant DB as PostgreSQL (Neon)

    Admin->>API: Formulario de Nuevo Producto
    API->>Ctrl: POST /api/v1/products {name, price, category_id}
    Ctrl->>Svc: createProduct(ProductDTO)
    
    Svc->>CRepo: findById(category_id)
    CRepo->>DB: SELECT * FROM categories WHERE id=? AND tenant_id=?
    DB-->>CRepo: Category Entity
    CRepo-->>Svc: Category Entity (o lanza NotFoundException)
    
    Svc->>Svc: Valida reglas de negocio (precios, sku único)
    Svc->>Svc: Inicializa Product Entity (vincula Categoría)
    
    Svc->>PRepo: save(Product)
    PRepo->>DB: INSERT INTO products (...)
    DB-->>PRepo: Entidad Persistida
    PRepo-->>Svc: Producto Creado
    
    Svc-->>Ctrl: ProductResponse DTO
    Ctrl-->>API: 201 Created
    API-->>Admin: Muestra Toast de Éxito / Actualiza Tabla
```

## 2. Consulta del Menú (Cajero o Kiosko)

Demuestra cómo se trae la estructura completa del menú jerarquizado para mostrar en el Punto de Venta (Categorías -> Productos).

```mermaid
%%{init: {"themeVariables": {"fontFamily": "arial"}}}%%
sequenceDiagram
    participant UI as MenuList.tsx (React)
    participant Query as useCategoriesQuery.ts (TanStack Query)
    participant API as catalogApi.ts (Axios)
    participant Ctrl as CatalogController.java
    participant Svc as CatalogService.java
    participant Repo as CategoryRepository.java
    participant DB as PostgreSQL (Neon)

    UI->>Query: render() lanza hook
    Query->>API: GET /api/v1/catalog/menu
    API->>Ctrl: Petición HTTP GET
    
    Ctrl->>Svc: getFullMenu()
    Svc->>Repo: findAllWithProducts()
    Note right of Repo: `@Query("SELECT c FROM Category c JOIN FETCH c.products")`
    Repo->>DB: JOIN SQL en base de datos
    DB-->>Repo: Resultados Jerarquizados
    Repo-->>Svc: Entidades JPA
    Svc->>Svc: Mapeo a MenuDTO (Oculta campos sensibles)
    
    Svc-->>Ctrl: MenuResponse
    Ctrl-->>API: 200 OK
    API-->>Query: Data parseada
    Query-->>UI: Re-render con listado del menú
```
