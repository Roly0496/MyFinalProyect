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

### Repositorios (Spring Data JPA)
- 8 repositorios en el paquete `repository`, uno por entidad. Son interfaces que extienden
  `JpaRepository`: Spring genera la implementación al arrancar.
- Consultas derivadas del nombre del método (*query methods*): `findByNombre`,
  `existsByNombre`, `findByActivoTrueOrderByNombreAsc`, `findByFechaBetween...`, etc.
- `Optional` en las búsquedas que pueden no encontrar nada, para tratar ese caso de forma explícita.
- `@EntityGraph` en las consultas que siempre necesitan una relación (receta con sus materias
  primas, historial con su usuario): se cargan en una sola consulta con `JOIN` y se evita el
  problema N+1 (una consulta extra por cada fila).
- Historiales limitados a los 100 registros más recientes (`Top100`). La paginación queda como
  posible mejora.
- Orden por fecha y, en caso de empate, por id (`OrderByFechaDescIdDesc`): una producción genera
  varios movimientos con la misma fecha y así el orden es siempre el mismo.
- Comprobación: el test de arranque de Spring valida que todos los nombres de método son correctos.

### Servicios
- Paquete `exception` con dos excepciones propias (heredan de `RuntimeException`):
  - `RecursoNoEncontradoException` → el recurso pedido no existe (futuro error 404).
  - `ReglaNegocioException` → la operación no está permitida (nombre repetido, tipo incorrecto...).
- Inyección de dependencias por constructor, sin `@Autowired` en atributos.
- `@Transactional(readOnly = true)` en la clase y `@Transactional` solo en los métodos que modifican
  datos. Si se lanza una `RuntimeException`, se deshacen todos los cambios (*rollback*).
- *Dirty checking*: al modificar una entidad dentro de una transacción no hace falta llamar a
  `save()`; Hibernate detecta el cambio y hace el `UPDATE` al terminar.
- `MateriaPrimaService`: listar, obtener, crear, actualizar y desactivar.
  - El nombre se limpia con `trim()` y no se puede repetir. Al actualizar se ignora la propia
    materia prima, porque MySQL compara sin distinguir mayúsculas ("harina" = "Harina").
  - La unidad no se puede cambiar: dejaría sin sentido el historial y las recetas.
  - El stock no se edita a mano: empieza en 0 y solo cambia con movimientos.
  - Borrado lógico (`activo = false`): los movimientos antiguos siguen apuntando a ella.
- `ProductoService`:
  - REVENTA necesita precio de compra; ELABORADO no puede tenerlo (su coste sale de la receta).
    La primera regla también está en MySQL (`CHECK`); la segunda solo en Java.
  - El tipo de producto no se puede cambiar una vez creado.
  - Recetas: ver, añadir ingrediente, cambiar cantidad y quitar ingrediente. Solo productos
    ELABORADO, sin materias primas desactivadas ni repetidas.
  - Se comprueba que el ingrediente pertenece al producto indicado: no se confía en los ids
    que llegan desde fuera.
  - Coste = Σ cantidad × precio unitario, con `BigDecimal` (no `double`, por los errores de
    redondeo) y redondeo a 2 decimales (`HALF_UP`) solo al final.
- `MovimientoService`: único punto donde cambia el stock.
  - Método `aplicar(Movimiento)`: valida el signo según el tipo, comprueba que el stock no queda
    negativo, cambia el stock y guarda el movimiento. Producción, ventas, entradas, mermas y
    ajustes pasan siempre por él, así ninguna operación puede saltarse estas reglas.
  - Signo según el tipo (igual que el `CHECK` de MySQL): ENTRADA siempre suma, VENTA y MERMA
    siempre restan, PRODUCCION y AJUSTE pueden sumar o restar pero nunca valer 0.
  - Bloqueo pesimista: antes de cambiar el stock se relee la fila con
    `entityManager.refresh(..., PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`). Evita que dos
    operaciones simultáneas (por ejemplo, dos ventas del último croissant) dejen el stock mal
    (*lost update*). Se descarta poner `@Lock` en el repositorio: si la entidad ya estaba cargada
    en memoria, Hibernate no vuelve a leer el stock. Se prefiere al bloqueo optimista (`@Version`)
    porque es mejor esperar unos milisegundos que obligar a repetir la venta.
  - Entradas: de materias primas o de productos de REVENTA (los ELABORADO suben con producción).
  - Mermas: el usuario indica la cantidad en positivo y el servicio le cambia el signo. Motivo
    obligatorio.
  - Ajustes tras un recuento: el usuario indica el stock real y el servicio calcula la diferencia
    (se guarda la diferencia, no el valor final). La diferencia se calcula después de bloquear la
    fila. Motivo obligatorio.
  - Por ahora los métodos reciben el `Usuario`; cuando exista el login lo obtendrá el controlador.
  - Pendiente: en producción y en ventas, bloquear las filas siempre en el mismo orden (por id)
    para evitar interbloqueos (*deadlocks*).
- Validaciones de formato (nombres vacíos, números negativos) pendientes para los controladores
  (`@NotBlank`, `@PositiveOrZero`). MySQL mantiene los `CHECK` como última barrera.

### Problemas encontrados
- `git add .` ejecutado desde la carpeta `android/` solo añadía esa carpeta (y un archivo de
  configuración de Android Studio que no tocaba).
  → Solución: `git restore --staged` para quitarlo y `git add ../backend/` para añadir los cambios
  correctos. Revisar siempre con `git status` antes de hacer commit.

---

## 10/10/2026

### Servicios
- `ProduccionService`: registrar una producción y consultar las producciones (las 100 últimas o
  las de un día concreto).
  - Comprobaciones previas: cantidad mayor que 0, producto activo, de tipo ELABORADO y con receta.
    La cantidad se comprueba en el propio servicio: con una cantidad negativa las materias primas
    subirían y el producto bajaría, y la regla del signo no lo detectaría.
  - Se guarda primero la producción, porque cada movimiento debe apuntar a ella.
  - La receta es para 1 unidad: por cada ingrediente se resta cantidad × unidades producidas.
    Después se suma el producto.
  - Los ingredientes se ordenan por el id de su materia prima antes de bloquearlos, y el producto
    se bloquea el último. Así dos producciones nunca se bloquean en orden cruzado (*deadlock*).
  - Todo en una sola transacción: si falta stock de cualquier ingrediente, se deshace la
    producción entera. Tras un *rollback* el `AUTO_INCREMENT` no retrocede (quedan huecos en la
    numeración, es normal).
- `VentaService`: registrar una venta con varias líneas, consultar ventas, ver las líneas de una
  venta y calcular la caja del día.
  - Cada línea se recibe como un `record` (`Linea(productoId, cantidad)`): una clase inmutable
    solo para llevar datos. No se usan `record` en las entidades porque JPA necesita modificarlas.
  - Se valida que haya al menos una línea, que todas indiquen producto, que las cantidades sean
    mayores que 0 y que no se repita un producto (se detecta con un `Set`).
  - Las líneas se ordenan por id de producto antes de bloquear (mismo motivo que en producción).
  - El total se calcula antes de crear la venta, porque la entidad `Venta` no se puede modificar.
  - Cada línea guarda una copia del precio de venta: si el precio cambia después, los tickets
    antiguos conservan el precio al que se vendió.
  - Si falta stock de un producto, se deshace la venta completa (no se vende "medio ticket").
- `UsuarioService`: listar, crear, actualizar, cambiar contraseña, activar, desactivar y login.
  - Contraseñas con BCrypt mediante la librería `spring-security-crypto`. Se descarta Spring
    Security completo por ahora: es más complejo y el MVP solo necesita un login sencillo.
    Pasar a Spring Security con JWT queda como posible mejora.
  - BCrypt es un *hash* irreversible, con sal (la misma contraseña da resultados distintos) y
    lento a propósito para dificultar los ataques de fuerza bruta. En el login no se descifra
    nada: se vuelve a calcular el *hash* y se compara (`matches`).
  - Clase `SeguridadConfig` con `@Configuration` y `@Bean` para que Spring gestione el
    `PasswordEncoder` (una clase de librería a la que no se le pueden poner anotaciones).
  - Nueva excepción `CredencialesIncorrectasException` (futuro error 401). Usuario inexistente,
    desactivado o contraseña incorrecta dan el mismo mensaje, para no revelar qué usuarios existen.
  - Contraseña de al menos 8 caracteres. El nombre de usuario no se puede cambiar.
  - Siempre debe quedar al menos un administrador activo: no se puede desactivar ni pasar a
    EMPLEADO al último.

### Administrador inicial
- Problema: para crear usuarios hay que ser administrador, pero al principio la tabla `usuario`
  está vacía.
- Se descarta una migración Flyway `V2` con el administrador: su contraseña inicial quedaría en
  el repositorio.
- Se elige una clase `AdminInicial` que implementa `ApplicationRunner` (se ejecuta una vez al
  arrancar). Si no hay ningún administrador activo, crea uno con el usuario y la contraseña de
  `horneo.admin.username` y `horneo.admin.password`, que están en `secrets.properties` (fuera de
  Git, como la contraseña de MySQL).
  - Si ya hay administrador, no hace nada. Si falta la contraseña, muestra un aviso en el log y la
    aplicación sigue arrancando.
  - Reutiliza `UsuarioService.crear`, así se aplican las mismas reglas (longitud mínima, BCrypt).
  - Las propiedades se leen con `@Value`. Los mensajes se escriben con un *logger* (SLF4J) en vez
    de `System.out.println`.
- Comprobación: el primer arranque crea el usuario `admin`; el segundo no crea ninguno más.

### Pruebas de integración
- Clase `CicloPrincipalTest` con 8 pruebas que usan los servicios reales contra MySQL:
  entradas, producción, producción sin stock, coste de la receta, venta con varias líneas, venta
  sin stock, ajuste por recuento y login.
- `@SpringBootTest` arranca la aplicación completa. `@Transactional` en la clase de prueba deshace
  todos los cambios al terminar cada prueba: la base de datos queda como estaba (comprobado con
  una consulta: 0 registros de prueba). Los nombres de prueba empiezan por `TEST`.
- `@BeforeEach` prepara el mismo escenario antes de cada prueba (empleado, harina, mantequilla y
  croissant con receta), para que las pruebas no dependan unas de otras.
- Comprobaciones con AssertJ (`assertThat`, `assertThatThrownBy`). Los `BigDecimal` se comparan
  con `isEqualByComparingTo`, porque `equals` considera distintos `7.6` y `7.60`.
- Limitación: al ir cada prueba en una sola transacción, no se puede comprobar el *rollback* real
  de una producción fallida. Para ello haría falta una base de datos solo para pruebas
  (posible mejora: Testcontainers con Docker).

### Problemas encontrados
- Las pruebas detectaron un error en `MovimientoService`: dos entradas seguidas de 1000 g y 500 g
  daban un stock de 500 en vez de 1500.
  - Causa: con el *dirty checking*, el primer cambio de stock solo estaba en memoria. El
    `refresh` del bloqueo pesimista volvía a leer la fila de MySQL (todavía con el valor antiguo)
    y descartaba ese cambio.
  - En la aplicación real no llegaba a ocurrir (cada operación es su propia transacción y no se
    repiten filas), pero era un error escondido que podía aparecer con cualquier cambio futuro.
  - Solución: método `bloquearYReleer`, que hace `flush()` (escribe en MySQL los cambios
    pendientes, sin confirmarlos) antes del `refresh` con bloqueo. Se usa en los 4 sitios donde
    se bloquea una fila.
  - Commits separados: primero la corrección (`fix:`) y después las pruebas (`test:`).

### Pendiente
- Controladores REST con DTOs, validación de datos de entrada (`@NotBlank`, `@Positive`...) y
  gestión centralizada de errores (`@RestControllerAdvice`: 404, 409, 401).
- Posible mejora: base de datos de pruebas con Testcontainers (Docker) para probar el *rollback*
  real.
