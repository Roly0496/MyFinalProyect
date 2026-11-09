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

### Controladores REST (primer controlador)
- Paquete `dto` con `record` para los datos que entran y salen de la API. No se devuelven las
  entidades: se controla qué campos salen (nunca el *hash* de la contraseña), se evitan errores
  con las relaciones LAZY fuera de la transacción y la API no depende de la base de datos.
- DTOs de entrada con validación (`@NotBlank`, `@NotNull`, `@Size`, `@PositiveOrZero`) y `@Valid`
  en el controlador. Tres capas de validación: DTO (formato), servicio (reglas de negocio) y
  MySQL (`CHECK` y `UNIQUE` como última barrera).
- Un DTO para crear y otro para actualizar: el de actualizar no tiene `unidad`, porque no se
  puede cambiar.
- DTO de salida con un método de fábrica `de(...)` y un campo calculado `bajoMinimo` (stock por
  debajo del mínimo) para que la app no tenga que calcularlo.
- `MateriaPrimaController` en `/api/materias-primas` siguiendo el estilo REST: `GET` listar y ver,
  `POST` crear (201), `PUT` actualizar, `DELETE` desactivar (204, borrado lógico).
- `GestorErrores` (`@RestControllerAdvice`): convierte las excepciones en respuestas con el
  formato estándar `ProblemDetail` (RFC 9457). 404 no encontrado, 409 regla de negocio,
  401 no autenticado, 403 sin permiso, 400 datos no válidos (con el error de cada campo).
  Los errores de MySQL dan un mensaje genérico para no mostrar detalles internos.
- Comprobado con peticiones HTTP reales (`curl`) que no guardan datos.

### Autenticación con JWT
- Se descartan: enviar el id del usuario en una cabecera (cualquiera podría suplantar a otro),
  sesiones guardadas en el servidor y Spring Security completo (demasiado complejo para el MVP).
- Se elige JWT con la librería jjwt 0.13.0: estándar en apps móviles y tecnología no vista en el
  ciclo.
- Un JWT tiene cabecera, contenido y firma. El contenido (id, rol, fecha de creación y de
  caducidad) se puede leer pero no modificar: la firma se calcula con una clave secreta que solo
  conoce el backend. No hace falta guardar sesiones.
- `TokenService` genera y comprueba los tokens. La clave (`horneo.jwt.secreto`, mínimo 32
  caracteres) está en `secrets.properties` y no tiene valor por defecto: sin ella la aplicación
  no arranca. Los tokens duran 12 horas.
- `POST /api/login` devuelve el token, el id, el nombre y el rol (para que la app muestre u oculte
  opciones).
- `AutenticacionInterceptor` (`HandlerInterceptor`) se ejecuta antes de cada petición a `/api/**`
  salvo el login: lee la cabecera `Authorization: Bearer ...`, comprueba el token, carga el
  usuario de la base de datos y lo deja en la petición para los controladores.
  - En cada petición se comprueba que el usuario sigue activo: un usuario desactivado pierde el
    acceso al momento aunque su token no haya caducado.
  - El rol se toma de la base de datos, no del token: si se le quita el rol de administrador a
    alguien, lo nota al instante.
- Anotación propia `@SoloAdmin` (`@Target(METHOD)`, `@Retention(RUNTIME)`) que se pone en los
  métodos de los controladores. El interceptor la lee con reflexión (`hasMethodAnnotation`) y
  responde 403 si el usuario no es administrador.
  - Diseño "permitido por defecto": lo que no lleva `@SoloAdmin` está abierto a cualquier usuario
    conectado. Riesgo: olvidar la anotación. Se cubrirá con pruebas que comprueben el 403.
- 401 = no se sabe quién eres (sin token, token falso o caducado). 403 = se sabe quién eres pero
  no tienes permiso.
- Permisos de materias primas: el empleado solo puede consultarlas.
- Comprobado con 10 casos (sin token, firma falsa, token caducado, usuario inexistente, empleado,
  administrador...). Para las pruebas se fabricaron tokens firmados con la clave, lo que demuestra
  que la clave debe mantenerse secreta.

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
- El login del administrador fallaba (401) con la contraseña correcta.
  - Causa: la contraseña tenía un carácter especial (ñ, tilde...). El archivo se guarda en UTF-8,
    pero Spring Boot lee los `.properties` en ISO-8859-1, así que el carácter se leyó mal
    (por ejemplo, `ñ` → `Ã±`) y se guardó el *hash* de esa versión estropeada.
  - Solución: escribir ese carácter con su código Unicode (`ñ` = `\u00f1`) en
    `secrets.properties`, borrar el administrador (no tenía movimientos) y dejar que
    `AdminInicial` lo vuelva a crear. Se añade un aviso en `secrets.properties.example`.

### Resto de controladores (productos, movimientos, producción y ventas)
- Permisos acordados con el criterio de "el empleado hace el trabajo diario, el administrador
  gestiona y ve la información del negocio":

  | Operación | ADMIN | EMPLEADO |
  |---|---|---|
  | Ver materias primas, productos, recetas e historiales | Sí | Sí |
  | Crear, editar o desactivar materias primas y productos; editar recetas | Sí | No |
  | Ver el coste de un producto | Sí | No |
  | Registrar entradas y mermas | Sí | Sí |
  | Ajustar el stock tras un recuento | Sí | No |
  | Producir, vender, ver producciones y tickets | Sí | Sí |
  | Caja del día | Sí | No |

  - El empleado puede registrar entradas y mermas porque cada movimiento guarda quién lo hizo y
    no se puede borrar. Los ajustes quedan para el administrador: mal usados podrían ocultar
    pérdidas.
- `ProductoController`: filtro opcional por tipo (`?tipo=ELABORADO`, `@RequestParam`), rutas
  anidadas para la receta (`/api/productos/{id}/receta/{ingredienteId}`) y el coste en una ruta
  aparte (`/api/productos/{id}/coste`) para poder protegerlo con `@SoloAdmin`.
- `MovimientoController`: primer controlador que recibe el usuario del token
  (`@RequestAttribute`). El usuario nunca se envía en el cuerpo de la petición, así nadie puede
  registrar movimientos en nombre de otro. Historiales en `/api/materias-primas/{id}/movimientos`
  y `/api/productos/{id}/movimientos`.
- `ProduccionController` y `VentaController`: filtro por día (`?fecha=2026-10-10`, formato
  ISO 8601 con `@DateTimeFormat`). La venta recibe una lista de líneas validada elemento a
  elemento (`List<@Valid LineaVentaPeticion>`) y devuelve el ticket completo (cabecera, líneas y
  subtotales). La caja del día devuelve el número de ventas y el total.
- Los DTOs de la API se convierten a los tipos del servicio (`VentaService.Linea`): el servicio no
  depende de cómo llegan los datos por HTTP.
- `GestorErrores` también responde 400 con mensaje cuando un parámetro de la URL no es válido
  (una fecha mal escrita, un tipo de producto que no existe).
- Problema evitado varias veces: los DTOs leen datos de relaciones LAZY (nombre de la materia
  prima, del producto, del usuario) después de cerrarse la transacción
  (`open-in-view=false`). Se añadieron búsquedas con `@EntityGraph`
  (`findConMateriaPrimaById`, `findConDetalleById`, `findConUsuarioById`) para traer esos datos
  en la misma consulta.
- Pruebas con la aplicación arrancada y peticiones HTTP reales (13 a 16 casos por controlador),
  usando datos temporales que se borran al terminar.
  - Se comprobó el *rollback* real: una venta de 1 croissant y 4 zumos (solo quedaban 3) falló
    por el zumo, y el stock del croissant, que ya se había restado, volvió a su valor. No se vende
    "medio ticket". Esto cubre la limitación de las pruebas de integración.

### Repositorio
- `git add ../` desde la carpeta `android/` añadió archivos de configuración de Android Studio
  (entre ellos `workspace.xml`, que es personal). Se quitaron con `git restore --staged` y se
  añadieron al `.gitignore`: `/.idea/` y `/.gradle/` en la raíz y `/.idea/inspectionProfiles` en
  `android/`. La `/` inicial hace que la regla solo afecte a esa carpeta, para no ignorar los
  archivos de `android/.idea/` que sí se suben.

---

## 11/10/2026

### API de usuarios
- `UsuarioController`: todas las operaciones son solo para el administrador (listar, crear,
  cambiar nombre o rol, poner contraseña nueva, desactivar y reactivar), salvo dos que puede usar
  cualquier usuario:
  - `GET /api/usuarios/yo`: ver mis datos.
  - `PUT /api/usuarios/yo/password`: cambiar mi contraseña. Exige la contraseña actual, para que
    nadie pueda cambiarla si se deja la sesión abierta.
- Las rutas `/yo` no llevan id: el usuario sale del token. Cuando una ruta fija (`/yo`) y una con
  variable (`/{id}`) coinciden, Spring elige la fija.
- Si la contraseña actual es incorrecta se responde 409 y no 401: la app interpretará el 401 como
  sesión caducada y cerraría la sesión por un simple error al escribir.
- Contraseñas entre 8 y 72 caracteres: BCrypt solo usa los primeros 72 bytes.
- La respuesta nunca incluye el *hash* de la contraseña.

### Pruebas de los controladores (MockMvc)
- `UsuarioControllerTest` con MockMvc: simula peticiones HTTP sin arrancar un servidor, pasando
  por el interceptor, la validación, el controlador y el gestor de errores. Los tokens se generan
  con `TokenService`.
- Comprueba que un empleado recibe 403 en las 7 operaciones de administración de usuarios
  (incluidos intentos de ascenderse a administrador o cambiar la contraseña de otro), que sin
  token se recibe 401, que `/yo` no devuelve el *hash* y que cambiar la contraseña exige la actual.
- Para comprobar que la prueba sirve, se quitó a propósito un `@SoloAdmin`: la prueba falló
  ("expected 403 but was 201"). Después se volvió a poner. Así se cubre el riesgo del diseño
  "permitido por defecto".
- Total: 14 pruebas automáticas, todas correctas.

### App Android: decisiones
- Paquete `com.horneo.app` (el de la plantilla era genérico).
- Arquitectura MVVM: pantallas en Jetpack Compose, un `ViewModel` por pantalla que guarda el
  estado, y repositorios que hablan con la API. Las pantallas no saben nada de HTTP.
- Conexión con la API: Retrofit + OkHttp, y kotlinx.serialization para convertir el JSON.
- Inyección de dependencias manual con una clase `AppContainer` (sin Hilt): para una app de este
  tamaño es suficiente y se entiende mejor qué objeto se crea y dónde.
- El token JWT se guarda con DataStore, para que la sesión siga abierta al cerrar la app.
- Navegación con Navigation Compose.
- Solo funciona con conexión (sin base de datos local). Trabajar sin conexión con Room queda como
  línea futura.
- Durante el desarrollo el backend está en el propio ordenador. Desde el emulador se llega a él con
  la dirección `10.0.2.2:8080`.
- Antes de hacer más pantallas hay que hacer una maqueta de diseño (colores, fuente, logo,
  accesibilidad), como pide la rúbrica.

### App Android: primeros pasos
- Paso 1: cambio del paquete de la plantilla a `com.horneo.app`.
- Paso 2: dependencias en el catálogo de versiones (`libs.versions.toml`): Retrofit, OkHttp,
  kotlinx.serialization, DataStore, Navigation Compose y Lifecycle. También:
  - Permiso `INTERNET`.
  - `network_security_config.xml`: Android solo permite HTTPS por defecto. Se permite HTTP
    únicamente hacia `10.0.2.2`, no hacia cualquier dirección.
- Paso 3: capa de datos del login.
  - `data/remote`: DTOs del login, interfaz `HorneoApi` (Retrofit), `ErroresApi` (saca el mensaje
    del `ProblemDetail` que devuelve el backend para mostrarlo al usuario) y un interceptor que
    añade la cabecera `Authorization: Bearer <token>` a cada petición.
  - `data/local/SesionStore`: guarda en DataStore el token, el id, el nombre y el rol.
  - `data/repository/AuthRepository`: login y cierre de sesión. Devuelve `Result`, así quien lo
    usa tiene que tratar el caso de error.
  - `AppContainer` y `HorneoApplication`: crean estos objetos una sola vez para toda la app.
- Se probó con una prueba temporal contra el backend arrancado: login correcto, contraseña
  incorrecta (401 con su mensaje), error de validación y servidor apagado. Todos muestran un
  mensaje claro.
- Los modelos (DTOs) existen en los dos lados (Java y Kotlin) porque lo que se comparte es el
  contrato JSON, no el código. Así cada parte puede cambiar por dentro sin romper la otra. Línea
  futura: generar los modelos automáticamente con OpenAPI.

### Estado
- Backend terminado: 6 controladores, login con JWT, permisos por rol, gestión de errores y
  14 pruebas.
- App Android: arquitectura decidida y capa de datos del login hecha.

### Pendiente
- Pantallas de login e inicio.
- Posible mejora: base de datos de pruebas con Testcontainers (Docker).

---

## 14/10/2026

### Pantallas de login e inicio
- `LoginViewModel`: guarda el estado de la pantalla en un `StateFlow<LoginUiState>` (usuario,
  contraseña, cargando, error). El botón solo se activa si los dos campos tienen texto y no hay
  una petición en curso. El error se borra en cuanto el usuario vuelve a escribir.
- `LoginScreen`: solo dibuja el estado y avisa al `ViewModel` de lo que hace el usuario.
- Los `ViewModel` se crean con una `Factory` que toma el repositorio del `AppContainer`.
- `MainActivity` decide qué pantalla mostrar según la sesión guardada en DataStore:
  - Cargando (todavía se está leyendo): indicador de progreso.
  - Sin sesión: pantalla de login.
  - Con sesión: pantalla de inicio.

  Al hacer login no se navega a mano: se guarda la sesión, DataStore avisa del cambio y la pantalla
  cambia sola. Al cerrar sesión pasa lo mismo al revés.
- `InicioScreen` (provisional): saluda al usuario, tiene un botón para comprobar la conexión
  (`GET /api/usuarios/yo`, que confirma que el servidor acepta el token) y otro para cerrar sesión.

### Problemas encontrados
- En el emulador con Android 17 (API 37) todas las peticiones al backend fallaban por tiempo de
  espera agotado (`SocketTimeoutException`), aunque el backend estaba funcionando y respondía con
  `curl`. Causa: desde Android 17 las apps necesitan el permiso `ACCESS_LOCAL_NETWORK` (grupo
  "Dispositivos cercanos") para conectarse a la red local, y `10.0.2.2` cuenta como red local.
  - Solución: el permiso se declara solo en `app/src/debug/AndroidManifest.xml`, que solo se usa en
    la versión de desarrollo. La versión final usará un servidor en Internet y no lo necesita. Se
    comprobó en los manifests generados: la versión de desarrollo lo tiene y la final no.
  - Se concede una vez en el emulador o el móvil: Ajustes > Aplicaciones > Horneo > Permisos >
    Dispositivos cercanos (o con `adb shell pm grant`).

### Pruebas
- Probado en el emulador con un usuario temporal (borrado después):
  - Contraseña incorrecta: aparece el mensaje de error.
  - Login correcto: la petición lleva la cabecera `Bearer` (comprobado en el registro de OkHttp).
  - La sesión sigue abierta después de forzar el cierre de la app.
  - Cerrar sesión vuelve al login.

### Demo de la defensa
- La demo se hará en local: backend, MySQL y emulador en el portátil (o un móvil real por USB con
  `adb reverse`). Como plan B, un vídeo explicado de 2-3 minutos. No hace falta pagar un servidor
  (pendiente de confirmar con el tutor).
- En la memoria se compararán las opciones de despliegue. Está previsto Docker Compose como
  tecnología nueva.

### Pendiente
- Maqueta de diseño (colores, fuente, logo, accesibilidad) antes de seguir con las pantallas.
- Resto de pantallas: materias primas, productos y recetas, movimientos, producción, ventas,
  usuarios.

## 29/10/2026

### Diseño: paleta de colores
- Se compararon tres paletas con los roles de color de Material 3 (`primary`, `onPrimary`,
  `surface`…), en modo claro y oscuro, y se calculó el contraste de cada combinación según WCAG 2.1
  (nivel AA: 4,5:1 para texto normal, 3:1 para texto grande):
  - A. Corteza tostada: marrón y naranja tostado, como la corteza del pan.
  - B. Trigo y oliva: verde oliva y dorado de trigo.
  - C. Frutos rojos: granate de frambuesa y miel.
- Elegida: **A, Corteza tostada**. En una app de inventario los colores tienen significado: el rojo
  indica un problema (stock bajo, error) y el verde, que todo va bien. Con la B, el verde de la marca
  se confundiría con «stock correcto». Con la C, el granate se parecería al rojo de error. El marrón
  anaranjado no choca con ninguno de los dos, así que las alertas destacan.
- Colores principales (claro / oscuro): `primary` #8C4A1A / #FFB68C, `background` #FFF8F5 /
  #1A120D, `error` #BA1A1A / #FFB4AB.
- Pendiente al pasarlo al código: desactivar `dynamicColor` en el tema. Si se deja activado, en
  Android 12 y posteriores la app coge los colores del fondo de pantalla del móvil (Material You) y no
  los de la marca.

### Diseño: tipografía
- Opciones comparadas: Roboto (la del sistema), Nunito en títulos con Roboto en el texto, y Atkinson
  Hyperlegible Next. Se probaron con caracteres que se confunden con facilidad (0 y O, 1, l e I) y
  con la letra del sistema al 100 % y al 130 %.
- Elegida: **Atkinson Hyperlegible Next**, diseñada por el Braille Institute para personas con baja
  visión. La app muestra muchas cantidades y códigos (por ejemplo, «T55», «10 ud», «1.000 g»), y una
  fuente en la que cada carácter se distingue claramente reduce los errores de lectura.
- Los tamaños se definen en `sp` con los roles de `Typography` de Material 3 (`titleLarge` para la
  barra superior, `labelLarge` para los botones…). Los `sp` respetan el tamaño de letra que elige el
  usuario en el sistema.
- La fuente se incluirá dentro de la app (archivos `.ttf` en `res/font`) y no se descargará al
  abrirla, para que funcione sin internet y no dependa de Google Play Services durante la demo.

### Diseño: logo
- El icono de una app Android es un icono adaptativo con dos capas de 108×108 dp: fondo y primer
  plano. El launcher solo enseña los 72 dp centrales y los recorta con su forma (círculo, squircle,
  cuadrado redondeado…). Por eso el dibujo tiene que caber en la zona segura, un círculo de 66 dp en el
  centro. Desde Android 13 hay además una capa monocroma para los iconos temáticos.
- Se compararon tres conceptos (boca del horno, hogaza con greñas y monograma H), con las tres
  formas de launcher, a tamaño real (48, 32 y 20 px) y en monocromo.
- Elegido: **monograma H**. Es la inicial de Horneo, con dos trazos verticales en crema y, como trazo
  horizontal, un pan dorado con tres pequeños cortes. Fondo marrón `primary` de la paleta. Es sobrio,
  como corresponde a una herramienta de trabajo, y el pan lo relaciona con el obrador.
- El mismo dibujo, junto al nombre «Horneo», se usará como logotipo en la pantalla de login.

### Repositorio
- Se ignora `.idea/deviceManager.xml`, un archivo de configuración local de Android Studio.

### Pendiente
- Diseño: boceto de las pantallas principales con la navegación entre ellas.
- Pasar la paleta y la tipografía al tema de la app (`ui/theme`).
- Resto de pantallas: materias primas, productos y recetas, movimientos, producción, ventas,
  usuarios.

## 04/11/2026

### Diseño: navegación
- Barra inferior con las cuatro tareas del día a día: **Inicio**, **Almacén**, **Producción** y
  **Ventas**. El resto de pantallas se abre desde ellas.
- Los movimientos no tienen pestaña propia. Las entradas, mermas y ajustes se registran desde la
  ficha de cada materia prima o producto, que es donde el usuario está mirando el stock.
- Un icono de cuenta en la barra superior lleva a cambiar la contraseña, cerrar sesión y, solo para el
  administrador, la gestión de usuarios.
- Las opciones de administrador (botón de añadir, ajuste, editar, coste, caja, usuarios) se
  **ocultan** al empleado en lugar de mostrarse desactivadas, porque un botón que no se puede pulsar
  confunde. La API sigue rechazando esas peticiones con 403, así que la seguridad no depende de la app.

### Diseño: bocetos de pantallas
- Se dibujaron nueve pantallas de baja fidelidad con la paleta, la fuente y el logo elegidos, y con
  los mismos campos que devuelve la API:
  1. Login: logotipo, usuario, contraseña con botón para mostrarla, «Entrar».
  2. Inicio: primero las alertas de materias primas bajo mínimo (campo `bajoMinimo`), después la
     producción del día, la caja del día (solo administrador) y atajos a «Nueva venta» y
     «Registrar producción».
  3. Almacén: pestañas «Materias primas» y «Productos», con el stock y la unidad en cada fila.
  4. Detalle de materia prima: stock en grande, botones Entrada / Merma / Ajuste e historial de
     movimientos.
  5. Registrar merma: cantidad en positivo (la app la envía en negativo), unidad fija junto al
     campo, motivo obligatorio y stock que quedará después.
  6. Detalle de producto: precio, stock y receta por unidad. Coste y margen solo para el
     administrador. Un producto de reventa muestra «Entrada» en lugar de receta.
  7. Registrar producción: antes de enviar, la app multiplica la receta por la cantidad, la compara
     con el stock y avisa de lo que falta (por ejemplo, «produce como máximo 34 ud»). El backend lo
     vuelve a comprobar, porque es quien tiene el stock real.
  8. Nueva venta: lista de productos con botones + y − grandes, pensada para el mostrador, y el
     total siempre visible.
  9. Ticket: confirmación con el detalle que devuelve `POST /api/ventas`.
- Las listas de producción y ventas y los formularios de alta y edición siguen los mismos patrones
  y no se dibujaron aparte.
- Criterios de accesibilidad aplicados en los bocetos:
  - Los avisos llevan texto además de color («Bajo mínimo», cantidades con signo + y −), para que
    se entiendan aunque no se distinga el color.
  - Los errores aparecen debajo de cada campo y dicen cómo arreglarlos.
  - Los botones del mostrador son grandes.
  - Los bocetos se probaron también con la letra del sistema al 130 %.

### Tema de la app: colores
- Un `ColorScheme` de Material 3 tiene 47 roles de color (`surfaceContainer`, `outlineVariant`,
  `inverseSurface`…), no solo los que se usaron en los bocetos. En lugar de inventarlos, se generaron
  con el algoritmo oficial de Material 3 (el mismo que usa la web Material Theme Builder) a partir del
  color semilla `#8C4A1A`, en la variante por defecto, *Tonal Spot*. Se descartó la variante
  *Fidelity* porque cambiaba el terciario a azul y ya no era la paleta elegida.
- El algoritmo ajusta el `primary` a `#8B4F26`, prácticamente igual al color semilla.
- `Color.kt` contiene los 47 roles en claro y oscuro, con los nombres que exporta Theme Builder
  (`primaryLight`, `primaryDark`…). `Theme.kt` los asigna todos en `lightColorScheme` y
  `darkColorScheme`. Si se deja uno sin asignar, Compose usa el morado por defecto.
- Se quitó el color dinámico (`dynamicColor`), así que la app mantiene siempre los colores de la
  marca.
- Se comprobó el contraste de 20 combinaciones de texto y fondo con los colores generados, y todas
  cumplen WCAG AA. La más justa es `onPrimary` sobre `primary` en modo claro, con 6,47:1.
- Verificado en el emulador, en modo claro y oscuro, con la pantalla de login.

### Tema de la app: tipografía
- Atkinson Hyperlegible Next solo se publica como fuente variable: un único archivo con un eje de
  grosor de 200 a 800. Compose solo aplica el grosor de una fuente variable desde Android 8 (API 26), y
  la app admite desde Android 7 (`minSdk` 24). Por eso se generaron con `fontTools` tres archivos
  estáticos (Regular 400, Medium 500, Bold 700) en `res/font`, de unos 48 KB cada uno.
- La licencia es SIL OFL 1.1, que permite incluir la fuente en la app y generar versiones derivadas,
  porque no tiene «nombre reservado». Una copia de la licencia va dentro de la app, en
  `assets/licencias`.
- Se comprobó con `fontTools` que la fuente incluye todos los caracteres del español (ñ, tildes, ¿,
  ¡, €). Por defecto sus cifras son proporcionales (el «1» es más estrecho que el «0»), pero tiene la
  característica `tnum` (cifras de ancho fijo). Se activa en todos los estilos para que las cantidades
  queden alineadas en columnas.
- `Type.kt` mantiene los tamaños de Material 3 (en `sp`) y solo cambia la fuente de los 15 estilos,
  con una función de extensión `conAtkinson()`.
- Verificado en el emulador: el login ya usa la fuente nueva.

### Icono de la app
- El robot verde de la plantilla se sustituyó por el monograma H, en dos formatos:
  - Icono adaptativo (Android 8 y posteriores): tres *vector drawables* de 108×108 dp en
    `res/drawable` (`ic_launcher_background`, `ic_launcher_foreground` e `ic_launcher_monochrome`),
    referenciados desde `mipmap-anydpi-v26/ic_launcher.xml` e `ic_launcher_round.xml`. Al ser
    vectores, se ven nítidos en cualquier pantalla.
  - Imágenes `.webp` en `mipmap-mdpi` … `mipmap-xxxhdpi` (de 48 a 192 px), para Android 7, que no
    tiene iconos adaptativos y que la app admite (`minSdk` 24). Se generaron con un script en Python
    (Pillow) con la misma geometría que los vectores.
- Los colores son los del tema: fondo `primary` (#8B4F26), H en `primaryContainer` (#FFDBC8) y pan en
  `inversePrimary` (#FFB68A).
- Los tres cortes del pan se dibujan como huecos (`fillType="evenOdd"`), no como líneas encima. Así
  funcionan igual en la capa de color, donde por el hueco se ve el fondo, y en la monocroma, donde el
  sistema solo usa la forma. Si fueran líneas de color, en monocromo desaparecerían.
- La plantilla usaba el primer plano también como capa monocroma. Ahora tiene su propia capa.
- Verificado en el emulador (ficha de la app en Ajustes, recorte circular). La capa monocroma no
  se pudo comprobar porque el emulador sin ventana no permite activar los iconos temáticos.

### Logotipo en el login
- Nuevo componente reutilizable `LogoHorneo` en el paquete `ui/componentes`. Dibuja el monograma
  con las mismas capas del icono (`ic_launcher_background` e `ic_launcher_foreground`), así el dibujo
  existe en un solo sitio.
- Como las capas miden 108 dp y el launcher solo enseña los 72 dp centrales, el componente dibuja las
  capas a 108/72 de su tamaño (`requiredSize`) y recorta lo que sobra con una forma redondeada
  (`clip`). Así queda igual que el icono del móvil.
- Las imágenes son decorativas (`contentDescription = null`): el logo siempre va junto al nombre
  «Horneo» en texto, y el lector de pantalla ya lee ese texto. Si las imágenes tuvieran descripción,
  lo diría dos veces.
- El login sigue el boceto 01: logo, «Horneo» en negrita y el subtítulo «Gestión del obrador» en
  `onSurfaceVariant`. El logo mantiene sus colores de marca también en modo oscuro.
- Verificado en el emulador en modo claro y oscuro.

### Iconos
- Desde la versión 1.4, Material 3 ya no incluye la librería de iconos (`material-icons-core`),
  así que la app no tenía ningún icono disponible.
- Se descartó `material-icons-extended`: añade miles de iconos que no se usarían, y Google ya no la
  mantiene y recomienda Material Symbols.
- Decisión: iconos sueltos de **Material Symbols** (estilo Outlined, 24 px), descargados del
  repositorio oficial de Google como *vector drawables* y copiados a `res/drawable` con el prefijo
  `ic_`. Solo se incluyen los que se usan. Son los mismos iconos de los bocetos. Licencia Apache 2.0
  (copia en `assets/licencias`).
- Problema encontrado: los XML de Google traen `android:tint="?attr/colorControlNormal"`, un
  atributo de la librería AppCompat, que una app hecha solo con Compose no incluye. La compilación
  falla con «resource attr/colorControlNormal not found». Solución: quitar esa línea de cada icono
  nuevo. En Compose no hace falta, porque el componente `Icon` pinta el icono con el color del tema.

### Mostrar u ocultar la contraseña
- El campo de contraseña del login tiene un botón con un ojo (`trailingIcon`) que alterna entre
  `PasswordVisualTransformation` y `VisualTransformation.None`.
- El estado «se ve / no se ve» se guarda en la propia pantalla con `rememberSaveable`, no en el
  `ViewModel`, porque es solo visual y no afecta a la lógica del login. `rememberSaveable` lo
  conserva al girar la pantalla.
- Accesibilidad: el icono es un botón, así que lleva `contentDescription` con lo que hace
  («Mostrar contraseña» / «Ocultar contraseña»). Es lo contrario que el logo, que es decorativo.
- Verificado en el emulador: el botón se encontró por su nombre accesible en el árbol de
  accesibilidad (`uiautomator`), y al pulsarlo la contraseña se ve y el nombre cambia. Con la
  contraseña visible se aprecia la fuente: `Pan0l1lI` se lee sin confundir el 0 con la O ni el 1 con
  la l o la I.

### Barra de navegación inferior
- Navigation Compose (ya añadida en el paso 2) con **rutas con tipos** (*type-safe*, disponibles
  desde la versión 2.8): cada pantalla es un `@Serializable data object` (`InicioRuta`,
  `AlmacenRuta`, `ProduccionRuta`, `VentasRuta`) en lugar de un texto, así el compilador avisa si
  una ruta está mal escrita. Están en `ui/navegacion/Destinos.kt`, junto con una `enum class Pestana`
  que reúne la ruta, el texto y el icono de cada pestaña.
- `ui/navegacion/PantallaPrincipal.kt`: `Scaffold` con una `NavigationBar` abajo y un `NavHost` en
  el centro, donde se dibuja la pantalla de la pestaña activa.
  - `currentBackStackEntryAsState()` hace que la barra se vuelva a dibujar cuando cambia la
    pantalla, para marcar la pestaña activa. Se comprueba con `hierarchy` para que la pestaña siga
    marcada cuando más adelante se abran pantallas «dentro» de ella (por ejemplo, el detalle de una
    materia prima dentro de Almacén).
  - Al cambiar de pestaña se usa `popUpTo(inicio) { saveState = true }`, `launchSingleTop` y
    `restoreState`. Así no se van apilando pantallas, «Atrás» desde cualquier pestaña vuelve a Inicio y
    cada pestaña recuerda dónde estaba el usuario.
  - Los iconos de las pestañas llevan `contentDescription = null`, porque el texto ya las nombra.
- Inicio usa la `InicioScreen` que ya existía. Almacén, Producción y Ventas muestran de momento
  `PantallaProvisional` («Próximamente»).
- `MainActivity`: con sesión iniciada se muestra `PantallaPrincipal` fuera del `Scaffold` de la
  actividad, porque dos `Scaffold` anidados dejarían dos veces el hueco de las barras del sistema.
- Nuevos iconos de Material Symbols: `ic_home`, `ic_inventory_2`, `ic_bakery_dining` e
  `ic_point_of_sale`.
- Probado en el emulador desde Android Studio, con el backend en marcha y en modo claro y oscuro:
  las cuatro pestañas cambian y se marcan, «Atrás» vuelve a Inicio y «Cerrar sesión» lleva al login.

### Pendiente
- Pantallas de cada pestaña siguiendo los bocetos: Almacén (materias primas y productos), Inicio
  definitivo, Producción y Ventas.
- Menú de cuenta (contraseña, cerrar sesión, usuarios para el administrador).

## 09/11/2026

### Almacén: datos de las materias primas
- Primera pantalla que pide datos reales al backend: `GET /api/materias-primas`. Sigue el mismo
  camino que el login: `HorneoApi` (Retrofit) → `MateriaPrimaRepository` (devuelve un `Result`) →
  `ViewModel` → pantalla. El repositorio se crea una sola vez en `AppContainer`.
- `MateriaPrimaDtos.kt`: el modelo `MateriaPrimaRespuesta` con los mismos campos que el JSON del
  backend, y la `enum class Unidad` (`GRAMO`, `MILILITRO`, `UNIDAD`) con el símbolo que se muestra
  (g, ml, ud).
- **Problema: los decimales.** kotlinx.serialization no sabe leer `BigDecimal`, y leer las
  cantidades como `Double` no es exacto (por ejemplo, 0,1 + 0,2 da 0,30000000000000004). En una
  aplicación que maneja stock y dinero eso no vale, y el backend ya usa `BigDecimal` por la misma
  razón.
- Solución: `data/remote/BigDecimalSerializer`, un serializador propio que lee el número tal y como
  viene escrito en el JSON (el texto `"12500.00"`) y crea el `BigDecimal` directamente, sin pasar
  nunca por `Double`. Al enviar, escribe el número sin comillas, que es lo que espera el backend.
- Pruebas unitarias (`MateriaPrimaDtosTest`, 4 pruebas): lee una materia prima de un JSON de
  ejemplo, conserva los decimales exactos, suma sin errores de redondeo y escribe el número sin
  comillas. Se ejecutan en el ordenador, sin emulador (`./gradlew testDebugUnitTest`).

### Almacén: lista de materias primas
- `AlmacenViewModel` expone el estado de la lista como un `StateFlow` de una `sealed interface`
  con tres casos: `Cargando`, `Error(mensaje)` y `Listo(lista)`. Así la pantalla está obligada a
  tratar los tres, y no puede quedarse en un estado a medias (por ejemplo, «cargando» y con error a
  la vez).
- `AlmacenScreen` sigue el boceto: barra superior «Almacén» y dos pestañas, Materias primas y
  Productos (`PrimaryTabRow`). La pestaña elegida se guarda con `rememberSaveable`, porque es solo
  visual (igual que el botón de la contraseña). Productos sigue siendo provisional.
- Cada estado tiene su pantalla: un indicador circular mientras carga, el mensaje de error con un
  botón **Reintentar**, un texto si la lista está vacía y, si hay datos, una `LazyColumn` (solo
  dibuja las filas que se ven, como el `RecyclerView` de las vistas clásicas).
- Cada fila (`ListItem`) muestra el nombre, el stock mínimo y, a la derecha, el stock actual.
- **Aviso de bajo mínimo:** el backend ya envía el campo `bajoMinimo`, así que la regla está en un
  solo sitio. La app solo lo muestra, con una etiqueta en los colores `errorContainer` /
  `onErrorContainer` del tema. Accesibilidad: la etiqueta lleva el texto «Bajo mínimo» además del
  color, para que se entienda aunque no se distinga el rojo.
- **Formato de las cantidades:** `ui/Formato.kt`, función `formatearCantidad`. Usa el formato
  español (punto para los miles, coma para los decimales) y quita los ceros sobrantes: 12500.00 se
  muestra como «12.500 g» y 0.10 como «0,1 g». Tiene sus pruebas unitarias (`FormatoTest`, 3 pruebas).
- Problema encontrado: la barra superior del Almacén dejaba un hueco de más arriba, porque volvía a
  reservar el espacio de la barra de estado que el `Scaffold` de `PantallaPrincipal` ya había
  reservado. Solución: `consumeWindowInsets(innerPadding)` en el `NavHost`, que avisa a las
  pantallas de dentro de que ese espacio ya está cubierto.

### Datos de ejemplo
- `backend/datos-ejemplo.sql`: datos de una panadería de ejemplo para el desarrollo y para la demo
  de la defensa. No es una migración de Flyway: se ejecuta a mano, una sola vez, con
  `mysql -u horneo -p horneo < datos-ejemplo.sql`.
- Contenido: 8 materias primas (Mantequilla y Huevos por debajo del mínimo, para que se vea el
  aviso), 6 productos (4 elaborados con su receta y 2 de reventa; la Ensaimada con stock 0) y los
  movimientos de «Inventario inicial».
- Se respeta la regla del proyecto de que el stock solo cambia mediante movimientos: cada stock
  inicial tiene su movimiento (`ENTRADA` para materias primas y reventa, `AJUSTE` para elaborados),
  para que el stock y el historial cuadren.
- Todo va dentro de una transacción (`START TRANSACTION` … `COMMIT`): si una sentencia falla, no se
  guarda nada a medias.
- Problema encontrado: la consola de base de datos de IntelliJ necesita la edición Ultimate, que es
  de pago. Se ejecutó desde la terminal con el cliente `mysql`, que además deshace la transacción
  automáticamente si algo falla.
- Problema encontrado: al ejecutar el script, MySQL respondía «contraseña incorrecta». La contraseña
  del usuario `horneo` no está en `application.properties`, sino en `backend/secrets.properties`
  (`spring.datasource.password`), que no se sube a GitHub. Con esa contraseña funcionó.
- Probado en el emulador desde Android Studio: aparecen las 8 materias primas con su stock y su
  unidad, Mantequilla y Huevos con el aviso, y las cantidades con el formato español.

### Almacén: datos de los productos
- Mismo camino que las materias primas: `ProductoDtos.kt` (`enum class TipoProducto` y
  `ProductoRespuesta`), `HorneoApi.productos()` (`GET /api/productos`), `ProductoRepository` y
  su instancia en `AppContainer`.
- Dos diferencias con las materias primas:
  - `precioCompra` es opcional (`BigDecimal?`): los productos elaborados no tienen precio de
    compra, porque su coste sale de la receta. Sin el `?`, la app fallaría al leer un elaborado.
  - `stockActual` es un `Int` y no un `BigDecimal`: los productos se cuentan por unidades enteras
    (en la base de datos es `INT`), mientras que las materias primas llevan decimales.
- Pruebas unitarias (`ProductoDtosTest`, 2 pruebas): un elaborado sin precio de compra y un
  producto de reventa con precio de compra.

### Seguridad: el empleado no recibe costes
- **Problema encontrado:** al revisar qué devolvía el backend para preparar la pantalla de productos,
  se vio que `GET /api/productos` enviaba el `precioCompra` a cualquier usuario, también al empleado.
  En un producto de reventa, el precio de compra es su coste, y se había acordado que el empleado no
  ve costes. Lo mismo pasaba con el `precioUnitario` de las materias primas: con la receta de un
  producto y esos precios, el empleado podía calcular el coste igualmente.
- Se plantearon dos opciones: esconder el dato solo en la pantalla de la app, o que el backend no
  lo enviara. Se eligió la segunda: si solo se esconde en la app, el dato sigue llegando al móvil y
  cualquiera que mire las peticiones (por ejemplo, con un proxy) lo ve. La seguridad tiene que estar
  en el servidor; la app solo decide qué se muestra.
- Solución en el backend:
  - `ProductoRespuesta` y `MateriaPrimaRespuesta` tienen un segundo método de construcción,
    `sinCostes()`, que deja el precio a `null`.
  - En `ProductoController` y `MateriaPrimaController`, los `GET` que usan los dos roles reciben el
    usuario de la petición (`@RequestAttribute`, igual que en movimientos y ventas) y un método
    privado `responder()` elige la respuesta según el rol. Así la regla está en un solo sitio.
    Crear y actualizar no se tocan, porque ya son `@SoloAdmin`.
  - En `MateriaPrimaRespuesta`, las dos versiones usan un método privado `construir()`, para que el
    cálculo de `bajoMinimo` no esté repetido.
- **Problema encontrado:** en la app, `precioUnitario` estaba declarado como no nulo. Con el cambio
  del backend, la app habría fallado al abrir el Almacén con un empleado. Se cambió a `BigDecimal?`
  en el mismo commit que el backend, porque los dos lados forman un mismo contrato.
- Pruebas:
  - Backend: `ProductoControllerTest` y `MateriaPrimaControllerTest` (MockMvc, 2 pruebas cada uno).
    El empleado recibe el precio a `null` (en un elemento y en toda la lista) y sigue recibiendo el
    resto de datos, como el stock y el aviso de bajo mínimo. El administrador sí recibe el precio.
    18 pruebas del backend en total, todas correctas.
  - Se comprobó que las pruebas sirven: al quitar el arreglo de forma temporal, fallan (el empleado
    recibía `"precioCompra":0.95`); al restaurarlo, vuelven a pasar.
  - App: nueva prueba en `MateriaPrimaDtosTest` que lee una respuesta con el precio a `null`.
    11 pruebas unitarias en total, todas correctas.
- Lección: revisar el contrato de la API antes de hacer cada pantalla sirve también para encontrar
  fallos de seguridad que no se vieron al hacer el backend.

### Almacén: lista de productos
- La segunda pestaña del Almacén sigue el boceto 03: debajo del nombre, el tipo y el precio de
  venta («Elaborado · 1,20 €»); a la derecha, el stock en unidades. Si el stock es 0, aparece la
  etiqueta «Agotado», con el mismo estilo que «Bajo mínimo».
- El precio de compra no se muestra en la lista. Según los bocetos, el coste va en el detalle del
  producto y solo para el administrador. Por eso esta pantalla no necesita saber el rol.
- **Decisión de diseño: una sola lógica para las dos listas.** Las dos pestañas tienen los mismos
  estados (cargando, error con «Reintentar», vacía y con datos). En lugar de copiar el código de
  materias primas para productos:
  - El estado pasa a ser genérico, `ListaUiState<T>` (antes `MateriasPrimasUiState`). La `<T>`
    funciona como en `List<T>` de Java: el mismo tipo vale para cualquier lista.
  - El componente `ContenidoLista` dibuja cualquier lista según su estado. Cada pestaña solo le
    pasa cómo se dibuja una fila (`FilaMateriaPrima` o `FilaProducto`).
  - La etiqueta roja pasa a ser `EtiquetaAviso(texto)`, que sirve para los dos avisos.
  - Así, cualquier cambio en cómo se muestran la carga o los errores se hace en un solo sitio.
- El `ViewModel` pide las dos listas al abrir el Almacén, para que al cambiar de pestaña los
  productos ya estén cargados. Cada pestaña tiene su propio «Reintentar».
- Precios: nueva función `formatearPrecio` en `Formato.kt`. Los precios llevan siempre dos
  decimales (1,20 €, 15,00 €), a diferencia de las cantidades, que quitan los ceros sobrantes. Con
  su prueba en `FormatoTest` (12 pruebas unitarias en total, todas correctas).
- Probado en el emulador desde Android Studio: aparecen los 6 productos de los datos de ejemplo,
  con su tipo y su precio, y la Ensaimada con «Agotado».

### Repositorio
- Problema encontrado: un `git add` con varias rutas, pegado en varias líneas, se ejecutó como
  varios comandos. Solo se añadió el primer archivo, y el commit salió con 1 archivo en vez de 5.
  Como ese commit aún no estaba subido, se completó con `git commit --amend --no-edit`.
  Desde entonces se añade cada archivo con su propio `git add` y se revisa `git status` antes de
  cada commit. `--amend` solo se usa con commits que no se han subido a GitHub.
- Mensajes de commit: `feat:` para funcionalidades nuevas, `fix:` para correcciones (los dos
  arreglos de costes) y `build:` para cambios de configuración de la compilación (`minSdk`).

### Detalle de materia prima: plan
- Siguiendo el boceto 04, se hizo en cuatro pasos con un commit cada uno: capa de datos,
  navegación, pantalla de detalle y formularios de movimientos.

### Detalle de materia prima: capa de datos
- `MovimientoDtos.kt`: `enum class TipoMovimiento` (ENTRADA, PRODUCCION, VENTA, MERMA, AJUSTE) y
  `MovimientoRespuesta`. Varios campos son opcionales (`?`): un movimiento es de una materia prima o
  de un producto, el motivo solo existe en mermas y ajustes, y `produccionId` / `ventaId` solo si el
  movimiento viene de una producción o de una venta.
- `HorneoApi`: `GET /api/materias-primas/{id}` y `GET /api/materias-primas/{id}/movimientos`.
  `@Path("id")` sustituye el `{id}` de la URL por el valor del parámetro.
- **Problema encontrado: las fechas y la versión mínima de Android.** Para manejar fechas lo normal
  es `java.time` (`LocalDateTime`), pero solo existe desde Android 8.0 (API 26) y la app tenía
  `minSdk 24` (Android 7.0). En un móvil con Android 7 la app se habría cerrado al mostrar el
  historial. Opciones:
  - A. Subir `minSdk` a 26.
  - B. Activar el *desugaring* de Java, que añade `java.time` a versiones antiguas (más
    configuración y una dependencia más).
  - C. Tratar la fecha como texto (frágil).
- Decisión: **A**. Android 7 tiene casi diez años y prácticamente ningún móvil en uso lo lleva.
  Se hizo en un commit aparte (`build:`).
- `LocalDateTimeSerializer`: misma idea que `BigDecimalSerializer`. El backend envía las fechas
  como texto ISO 8601 (`"2026-11-09T09:39:35.123456"`) y `LocalDateTime` lo lee directamente.
- Pruebas (`MovimientoDtosTest`): una merma con su fecha y su cantidad negativa, y una fecha sin
  fracciones de segundo (Spring Boot no las escribe cuando los segundos son exactos).

### Detalle de materia prima: navegación
- El Almacén pasa a ser un subgrafo de navegación (`AlmacenGrafo`) con la lista, el detalle y el
  formulario. La pestaña apunta al subgrafo, así sigue marcada en cualquiera de esas pantallas.
- Primera ruta con datos: `DetalleMateriaPrimaRuta(id, nombre)`, una `data class` en lugar de un
  `data object`. Lleva el id para pedir los datos y el nombre para que el título se vea al instante.
- `AlmacenScreen` recibe `onMateriaPrimaClick` y no sabe adónde se navega: solo avisa de qué fila se
  ha tocado y decide `PantallaPrincipal`. Así la pantalla no depende de la navegación.
- Accesibilidad: las filas son `clickable` con `onClickLabel = "Ver detalle"` (lo anuncia TalkBack)
  y la flecha de volver lleva `contentDescription = "Volver"`.
- Nuevo icono `ic_arrow_back` (Material Symbols, sin la línea `tint`; se mantiene `autoMirrored`
  para los idiomas que se escriben de derecha a izquierda).
- Probado en el emulador: se abre el detalle, la pestaña Almacén sigue marcada y se vuelve con la
  flecha y con el botón atrás del sistema.

### Detalle de materia prima: pantalla
- `DetalleMateriaPrimaViewModel` recupera el id de la ruta con `SavedStateHandle.toRoute()` y pide la
  materia prima y su historial **a la vez** con `async`, en lugar de uno detrás de otro.
- Tarjeta de stock: stock actual, aviso de bajo mínimo y mínimo. El precio solo aparece si llega
  (solo al administrador, por el arreglo de costes), así que la pantalla no necesita saber el rol.
  Se muestra por kilo, litro o unidad (`formatearPrecioUnitario`): 0,009 €/g se lee mejor como
  9,00 €/kg.
- Historial: tipo del movimiento («Producción #12», «Merma»…), fecha y hora, usuario, motivo y la
  cantidad con signo (`+1.000 g`, `-150 g`) y en color: verde si entra y rojo si sale. Así se lee de
  dos formas, también sin distinguir colores.
- **Color verde añadido:** Material 3 no tiene un color de «éxito». Se añadieron `entradaLight`
  (#1D6B3A) y `entradaDark` (#8BD79C), y `colorScheme.entrada` elige uno según lo oscuro que sea el
  fondo del tema. Contraste comprobado: 6,2:1 en claro y 10,8:1 en oscuro (WCAG AA pide 4,5:1).
- `EtiquetaAviso` se movió a `ui/componentes` para usarla en la lista y en el detalle.
- Toda la pantalla es una sola `LazyColumn` (tarjeta, botones, título e historial se desplazan
  juntos).
- Probado en el emulador con los datos de ejemplo: la tarjeta y el historial se ven bien y las
  fechas reales del backend se leen correctamente.

### Detalle de materia prima: movimientos
- Capa de datos: `MovimientoMateriaPrimaPeticion` (entrada y merma; la cantidad va siempre en
  positivo y en la merma la resta el backend) y `AjusteMateriaPrimaPeticion` (se envía el stock real
  contado y el backend calcula la diferencia). Nuevo `MovimientoRepository` con las tres llamadas
  `POST`. Pruebas que comprueban el JSON exacto que se envía, porque un nombre de campo distinto del
  backend provocaría un error de validación.
- **Decisión de diseño: un solo formulario para los tres movimientos.** Entrada, merma y ajuste
  tienen los mismos campos (cantidad y motivo); cambian los textos, si el motivo es obligatorio y la
  dirección a la que se envía. `TipoFormularioMovimiento` guarda esas diferencias y viaja en la ruta
  (`FormularioMovimientoRuta(id, nombre, tipo)`).
- Las reglas están en `ReglasMovimiento.kt`, en funciones de Kotlin sin Compose ni Android, para
  probarlas con pruebas unitarias rápidas (`ReglasMovimientoTest`, 9 pruebas):
  - Se acepta coma o punto como separador decimal.
  - «1.000» no se guarda como 1: tiene tres decimales y se rechaza («Como mucho dos decimales»), en
    lugar de guardar 1 cuando el usuario quería decir mil.
  - Las mismas reglas que el backend: cantidad mayor que 0, la merma no puede superar el stock, el
    ajuste no puede dejar el mismo stock y el motivo es obligatorio en mermas y ajustes. La app las
    repite para avisar al momento y al lado del campo; quien decide de verdad es el backend.
- Formulario (boceto 05): X para cerrar sin guardar, la unidad fija al lado del número, teclado
  numérico, el error o una ayuda debajo de cada campo, vista previa «Después de guardar» y sitio para
  el teclado (`imePadding`). El `ViewModel` vuelve a pedir la materia prima al abrirse, borra el error
  de un campo al escribir en él, evita enviar dos veces y muestra el mensaje del backend si lo rechaza.
- Botones en el detalle con distinto peso visual: Entrada (relleno, la más habitual), Merma (tonal)
  y Ajuste (con borde). El ajuste no aparece para el empleado; aun así, el backend responde 403 si un
  empleado lo intenta.
- **Problema encontrado: el historial no se actualizaba.** Después de guardar un movimiento, el
  detalle seguía mostrando el stock y el historial de antes, y la lista del Almacén también. Causa:
  cada `ViewModel` pedía los datos una sola vez, en su `init`, y al volver del formulario seguía vivo
  con los datos viejos. El movimiento sí se guardaba: al volver a abrir el detalle aparecía.
- Solución: se quitó la carga del `init` y cada pantalla usa `LifecycleResumeEffect`, que se ejecuta
  cada vez que la pantalla pasa a estar visible y activa (al abrirla y al volver de otra). Si ya hay
  datos, se siguen mostrando mientras llegan los nuevos, para que no parpadee.
- Probado en el emulador: los movimientos se guardan y, al volver, el detalle muestra el stock
  nuevo y el movimiento en el historial sin salir de la pantalla.

### Detalle de producto: decisiones
- Siguiendo el boceto 06, en tres pasos: capa de datos, pantalla de detalle y movimientos.
- Diferencias con las materias primas, vistas en el backend antes de empezar: el stock de los
  productos son unidades enteras (`Integer`), la entrada solo vale para productos de reventa (los
  elaborados suben su stock con una producción) y el coste de un elaborado sale de su receta,
  mientras que el de uno de reventa es su precio de compra.
- Decisión 1: ampliar el formulario de movimientos que ya existía para que sirva también con
  productos, en lugar de hacer otro casi igual.
- Decisión 2: la edición de recetas (el botón «+» del boceto) se deja para más adelante, junto con
  las demás pantallas de gestión del administrador (altas y edición de materias primas y productos).
  Los datos de ejemplo ya traen las recetas, y así se llega antes a Producción y Ventas, que son el
  centro de la app y lo que más se verá en la demo.

### Detalle de producto: capa de datos
- `IngredienteRespuesta` (una línea de la receta, con la unidad de la materia prima para mostrar
  «50 g» sin otra petición) y `CosteRespuesta` (coste de una unidad, solo administrador).
- `MovimientoProductoPeticion` y `AjusteProductoPeticion`: como las de materias primas, pero con
  `Int`. Una prueba comprueba que se envía `12` y no `12.00`, porque el backend espera un `Integer`.
- `HorneoApi`: producto, receta, coste e historial, y las tres llamadas `POST` de movimientos de
  productos.

### Detalle de producto: pantalla
- Las filas de la pestaña Productos se pueden tocar y abren `DetalleProductoRuta(id, nombre)`.
- Tarjeta: tipo, precio de venta y stock (con «Agotado» si es 0). Debajo, solo para el
  administrador, «Coste 0,32 € · margen 0,88 €» (el margen es el precio menos el coste: lo que se
  gana con cada unidad).
- El coste ni siquiera se pide si el usuario es empleado, porque el backend respondería 403. Por eso
  la fábrica de este `ViewModel` recibe un dato: `DetalleProductoViewModel.factory(esAdmin)`.
- Receta por unidad, solo en los elaborados.
- El historial se sacó a `Historial.kt` (una función de extensión de `LazyListScope`) para que lo
  usen los dos detalles sin copiarlo. Lo único que cambia es la unidad: en productos siempre «ud».
- Probado en el emulador: el croissant muestra su coste, su margen y su receta.

### Detalle de producto: movimientos
- `ElementoMovimiento` (MATERIA_PRIMA o PRODUCTO) viaja en la ruta del formulario. El `ViewModel`
  carga el elemento que toque y lo convierte siempre en lo mismo, `StockElemento` (stock y unidad),
  así la pantalla no necesita saber si es una materia prima o un producto.
- Productos: solo unidades enteras («Solo unidades enteras, sin decimales», también para «1.000») y
  teclado solo numérico. Los huevos también se cuentan en «ud», pero son materia prima y sí admiten
  decimales (una receta puede usar 0,1 huevos), así que la regla depende de si es un producto y no de
  la unidad. Hay una prueba para cada caso (11 pruebas de reglas, 34 pruebas unitarias en total).
- `BotonesMovimiento` se sacó a su propio archivo con dos opciones: `mostrarEntrada` (en productos,
  solo los de reventa) y `mostrarAjuste` (solo administrador).
- **Problema encontrado: la receta de los productos de reventa.** Al abrir el zumo de naranja salía
  «Zumo de naranja es de reventa: no tiene receta». Causa: la app pedía siempre la receta, y el
  backend responde 409 a la de un producto de reventa. Se había supuesto que devolvería una lista
  vacía, y en el paso anterior solo se probó con el croissant, así que el fallo no se vio.
- Solución: el `ViewModel` espera a que llegue el producto y solo pide la receta si es elaborado. Se
  hizo en un commit aparte (`fix:`), separado de los movimientos.
- Lección: comprobar cómo responde el backend en lugar de suponerlo, y probar todos los casos
  (aquí, los dos tipos de producto) y no solo el primero.
- Probado en el emulador: el zumo se abre sin receta y con Entrada, Merma y Ajuste; el croissant
  solo muestra Merma y Ajuste y rechaza los decimales.

### Producción: plan y decisiones
- Siguiendo el boceto 07, en tres pasos: capa de datos, lista del día y formulario para registrar.
- Lo que hace el backend al registrar (revisado antes de empezar): descuenta la receta × cantidad
  de cada materia prima y suma la cantidad al stock del producto, todo o nada; rechaza productos de
  reventa, productos sin receta y producciones sin stock suficiente. Lo pueden hacer los dos roles.
- Decisión sobre cómo elegir el día de la lista: flechas ‹ › (lo más rápido para hoy y ayer, que es
  el uso habitual) y, además, un calendario al tocar la fecha para saltar a cualquier día.

### Producción: capa de datos
- `ProduccionRespuesta` y `RegistrarProduccionPeticion` (solo producto y cantidad entera: el resto
  lo calcula el backend).
- `@Query("fecha")`: a diferencia de `@Path`, que pone el dato dentro de la ruta, añade un parámetro
  al final de la URL (`api/producciones?fecha=2026-11-09`). `LocalDate.toString()` ya da el formato
  ISO que espera el backend.

### Producción: lista del día
- `ListaUiState` y `ContenidoLista` se movieron del Almacén a `ui/componentes`, porque ahora también
  los usa Producción. Se añadió `margenInferior` para que el botón flotante no tape la última fila.
- Selector de día «‹ Hoy, 9 nov ›»: la flecha de la derecha se desactiva en hoy (no se puede mirar el
  futuro) y al tocar la fecha se abre el `DatePickerDialog` de Material, con los días futuros
  bloqueados. El calendario trabaja con milisegundos en UTC, así que la conversión con `LocalDate` se
  hace siempre en UTC para no equivocarse de día por la diferencia horaria.
- `formatearDia` («Hoy, 9 nov», «Ayer, 8 nov», «lun, 2 nov», con el año si es de otro año): los nombres
  de meses y días se escribieron a mano, porque Java devuelve «nov.» (con punto) en las pruebas y
  Android «nov», y así el resultado es siempre el mismo. Recibe «hoy» como parámetro para poder
  probarla con cualquier fecha.
- Si se cambia de día rápido, una respuesta lenta del día anterior podría llegar después y pisar la
  lista correcta. Para evitarlo, al cambiar de día se cancela la petición anterior (`Job.cancel()`).
- Botón flotante «Registrar producción». Producción es un subgrafo de navegación, como el Almacén.
- Problema encontrado: al descargar los iconos nuevos (`ic_add`, `ic_chevron_left`,
  `ic_chevron_right`), el comando que borraba la línea `android:tint` borró también el `>` que cerraba
  la etiqueta en `ic_add.xml`, porque en ese archivo iban en la misma línea. Se reescribió el archivo
  y se comprobó con `xmllint` que los tres iconos eran XML válido antes de compilar.
- Probado en el emulador: las flechas cambian de día, la de la derecha se desactiva en hoy y el
  calendario permite elegir un día pasado.

### Producción: registrar
- Las cuentas están en `ReglasProduccion.kt`, en funciones de Kotlin sin Compose (6 pruebas):
  - `calcularConsumo`: receta × cantidad comparada con el stock de cada materia prima (cuánto hace
    falta, cuánto hay y cuánto falta). Una materia prima que no aparece cuenta como stock 0.
  - `maximoProducible`: para cada ingrediente, stock ÷ cantidad por unidad redondeado hacia abajo;
    el máximo es el menor de todos, porque manda el ingrediente que menos alcanza. Ejemplo del
    boceto: con 850 g de mantequilla y 25 g por croissant, como máximo 34.
- Formulario (boceto 07): desplegable con los productos elaborados (`ExposedDropdownMenuBox`),
  cantidad entera y vista previa «Se consumirá». Las materias primas que no alcanzan van con fondo
  de error y el texto «faltan 150 g» (texto además de color). Si falta algo, el aviso dice cómo
  arreglarlo («No hay suficiente mantequilla. Registra una entrada o produce como máximo 34 ud.») y
  el botón se desactiva. El backend lo vuelve a comprobar, porque es quien tiene el stock real.
- El `ViewModel` pide a la vez los productos y las materias primas, y al elegir un producto pide su
  receta (cancelando la anterior si se cambia de producto).
- Botón «Producir» en el detalle de los productos elaborados: abre el formulario con el producto ya
  elegido (`RegistrarProduccionRuta(productoId)`; -1 significa «ninguno»).
- Probado en el emulador: con una cantidad grande aparecen las faltas y el aviso con el máximo; con
  una cantidad que alcanza, la producción se registra y aparece en la lista del día.
- Con esto funciona el ciclo principal de la app: materias primas → receta → producción → stock del
  producto.

### Pendiente
- Comprobar la vista del empleado (sin el botón Ajuste ni el coste) cuando exista la pantalla de
  usuarios.
- Gestión del administrador: altas y edición de materias primas y productos, y edición de recetas.
- Ventas e Inicio definitivo.
- Menú de cuenta (contraseña, cerrar sesión, usuarios para el administrador).
