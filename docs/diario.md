# Diario del proyecto - Horneo

Registro de lo que se hace cada día, decisiones tomadas y problemas encontrados.
Sirve como base para redactar la memoria.

---

## 06/10/2026

### Organización del proyecto
- Se reinicia el proyecto desde la plantilla de Android Studio para construirlo desde cero.
- Se reorganiza el repositorio en un único repositorio (monorepo) con dos carpetas:
  - `android/` → aplicación móvil (Android Studio)
  - `backend/` → API REST (IntelliJ IDEA)
- Motivo: un solo historial de Git para todo el proyecto, más fácil de entregar y de seguir.

### Reglas del negocio acordadas
1. Unidades fijas: gramos, mililitros y unidades. Se guarda siempre en la unidad base.
2. No se puede producir si falta algún ingrediente.
3. El stock nunca puede ser negativo.
4. Las recetas se pueden modificar; el historial no cambia porque los movimientos guardan lo gastado.
5. Las pérdidas se registran como movimiento de tipo MERMA.
6. Las producciones y ventas no se anulan (se corrige con un AJUSTE). Anular → línea futura.
7. Coste de un producto = suma de (cantidad de cada ingrediente × precio unitario).
8. Una venta puede tener varios productos (cabecera + detalle).

### Decisión: productos ELABORADO y REVENTA
- Idea propia: una panadería también vende productos que no fabrica (zumos, agua, bollería comprada).
- ELABORADO: tiene receta, su stock sube con una PRODUCCIÓN.
- REVENTA: sin receta, su stock sube con una ENTRADA (compra), tiene precio de compra obligatorio.

### Tecnologías elegidas
- Backend: Java 21 + Spring Boot 4.1.1 (proyecto Maven, creado con Spring Initializr).
- Base de datos: MySQL. Se descarta PostgreSQL porque MySQL ya se conoce del ciclo.
- Flyway para crear las tablas con SQL escrito a mano y versionado (migraciones).
- Hibernate en modo `validate`: no crea tablas, solo comprueba que las clases coinciden con ellas.
- No se usa Lombok: se escriben getters y setters a mano para entender todo el código.

### Base de datos
- Se crea la base de datos `horneo` (utf8mb4) y un usuario `horneo` solo con permisos sobre ella
  (principio de mínimo privilegio). No se usa `root` desde la aplicación.
- La contraseña va en `secrets.properties`, que está en `.gitignore` (no se sube a GitHub).
  Se sube una plantilla `secrets.properties.example`.
- Migración V1: 8 tablas (usuario, materia_prima, producto, ingrediente, produccion, venta,
  detalle_venta, movimiento), 12 claves foráneas, 16 CHECK, 5 UNIQUE.
- Prueba: insertar una materia prima con stock negativo → MySQL lo rechaza
  (`Check constraint 'ck_materia_prima_stock_actual' is violated`).

### Entidades JPA
- 8 entidades + 4 enums (Unidad, TipoProducto, Rol, TipoMovimiento) en el paquete `entity`.
- Relaciones `@ManyToOne` con carga LAZY, en una sola dirección.
- Produccion, Venta y DetalleVenta sin setters: el historial no se modifica.
- Movimiento se crea con métodos de fábrica (`deMateriaPrima` / `deProducto`) para que no pueda
  tener a la vez materia prima y producto.

### Problemas encontrados
- `git push` desde la terminal fallaba (no pedía usuario/contraseña).
  → Solución: instalar GitHub CLI (`gh auth login` + `gh auth setup-git`).
- Aviso de Flyway: MySQL 26.7 más nuevo que la versión verificada (9.4). No afecta.

---

## 07/10/2026

### Documentación
- Se crea la carpeta `docs/` con este diario y los diagramas.
- Diagrama E/R generado a partir del script `V1__crear_tablas.sql` (`docs/diagramas/modelo-er.md`).
