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

### Pendiente
- Botón para mostrar u ocultar la contraseña en el login (decidir antes cómo incluir los iconos).
- Resto de pantallas siguiendo los bocetos.
