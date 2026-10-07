# Modelo entidad-relación - Horneo

Generado a partir de `backend/src/main/resources/db/migration/V1__crear_tablas.sql`.
GitHub dibuja el diagrama automáticamente. Para exportarlo como imagen: copiar el bloque en https://mermaid.live

```mermaid
erDiagram
    USUARIO ||--o{ PRODUCCION : registra
    USUARIO ||--o{ VENTA : registra
    USUARIO ||--o{ MOVIMIENTO : registra
    PRODUCTO ||--o{ INGREDIENTE : "tiene receta"
    MATERIA_PRIMA ||--o{ INGREDIENTE : "se usa en"
    PRODUCTO ||--o{ PRODUCCION : "se fabrica en"
    VENTA ||--|{ DETALLE_VENTA : contiene
    PRODUCTO ||--o{ DETALLE_VENTA : "se vende en"
    MATERIA_PRIMA |o--o{ MOVIMIENTO : afecta
    PRODUCTO |o--o{ MOVIMIENTO : afecta
    PRODUCCION |o--o{ MOVIMIENTO : genera
    VENTA |o--o{ MOVIMIENTO : genera

    USUARIO {
        bigint id PK
        varchar nombre
        varchar username UK
        varchar password_hash
        enum rol "ADMIN, EMPLEADO"
        boolean activo
    }
    MATERIA_PRIMA {
        bigint id PK
        varchar nombre UK
        enum unidad "GRAMO, MILILITRO, UNIDAD"
        decimal stock_actual
        decimal stock_minimo
        decimal precio_unitario
        boolean activo
    }
    PRODUCTO {
        bigint id PK
        varchar nombre UK
        enum tipo "ELABORADO, REVENTA"
        decimal precio_venta
        decimal precio_compra "solo REVENTA"
        int stock_actual
        boolean activo
    }
    INGREDIENTE {
        bigint id PK
        bigint producto_id FK
        bigint materia_prima_id FK
        decimal cantidad "por 1 unidad"
    }
    PRODUCCION {
        bigint id PK
        datetime fecha
        bigint producto_id FK
        int cantidad
        bigint usuario_id FK
    }
    VENTA {
        bigint id PK
        datetime fecha
        bigint usuario_id FK
        decimal total
    }
    DETALLE_VENTA {
        bigint id PK
        bigint venta_id FK
        bigint producto_id FK
        int cantidad
        decimal precio_unitario
    }
    MOVIMIENTO {
        bigint id PK
        datetime fecha
        enum tipo "ENTRADA, PRODUCCION, VENTA, MERMA, AJUSTE"
        bigint materia_prima_id FK "o producto"
        bigint producto_id FK "o materia prima"
        decimal cantidad "con signo"
        varchar motivo
        bigint usuario_id FK
        bigint produccion_id FK
        bigint venta_id FK
    }
```

## Cómo leer las relaciones

| Símbolo | Significado |
|---|---|
| `\|\|--o{` | uno a muchos (cero o más) |
| `\|\|--\|{` | uno a muchos (al menos uno): una venta siempre tiene al menos una línea |
| `\|o--o{` | opcional: un movimiento puede no tener materia prima (si es de producto) |
