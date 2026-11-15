# Horneo

Aplicación Android para llevar el almacén, la producción y las ventas de una panadería pequeña desde el teléfono.

Las existencias nunca se escriben a mano: solo cambian con movimientos (entrada, producción, venta, merma o ajuste), y cada uno guarda su fecha, su cantidad y quién lo hizo. Así el almacén y su historial siempre cuadran.

Proyecto final del Ciclo Superior de Desarrollo de Aplicaciones Multiplataforma (IES de Teis, Vigo), curso 2026-2027.

## Qué hace

- **Almacén:** materias primas y productos, con sus existencias y avisos cuando algo baja del mínimo.
- **Producción:** al registrar una hornada se descuentan los ingredientes según la receta. Si falta alguno, la aplicación indica cuánto falta y cuántas unidades se pueden hacer.
- **Ventas:** carrito con varios productos, total siempre a la vista y tique al cobrar.
- **Mermas y ajustes:** las pérdidas se registran con su motivo; los ajustes los hace solo el administrador.
- **Dos roles:** el administrador gestiona el catálogo y ve costes y márgenes; el empleado hace el trabajo diario.

## Estructura del repositorio

| Carpeta | Contenido |
| --- | --- |
| `backend/` | API REST con Java 21 y Spring Boot. Incluye las migraciones de la base de datos y los datos de ejemplo. |
| `android/` | Aplicación Android con Kotlin y Jetpack Compose. |
| `docs/` | Diario del proyecto y modelo entidad-relación. |

## Tecnologías

- **Base de datos:** MySQL, con el esquema versionado con Flyway.
- **Servidor:** Java 21, Spring Boot, Spring Data JPA y autenticación con JWT.
- **Aplicación:** Kotlin, Jetpack Compose con Material 3, Retrofit y kotlinx.serialization.
- **Pruebas:** JUnit en el servidor y en la aplicación; Postman para probar la API a mano.

## Puesta en marcha

Hace falta tener instalados JDK 21, MySQL 8 y Android Studio.

### 1. Base de datos

Crea la base de datos y el usuario que usa el servidor (cambia la contraseña por la tuya):

```sql
CREATE DATABASE horneo CHARACTER SET utf8mb4;
CREATE USER 'horneo'@'localhost' IDENTIFIED BY 'tu_contraseña';
GRANT ALL PRIVILEGES ON horneo.* TO 'horneo'@'localhost';
```

Las tablas no hay que crearlas: lo hace Flyway la primera vez que arranca el servidor.

### 2. Servidor

```bash
cd backend
cp secrets.properties.example secrets.properties
```

Abre `secrets.properties` y rellena la contraseña de la base de datos, la del administrador inicial (mínimo 8 caracteres) y la clave de los tokens (mínimo 32 caracteres; se puede generar con `openssl rand -base64 32`). Ese archivo no se sube al repositorio.

```bash
./mvnw spring-boot:run
```

La API queda en `http://localhost:8080`. En el primer arranque se crea el usuario administrador con los datos de `secrets.properties`.

### 3. Datos de ejemplo (opcional)

Con el servidor arrancado al menos una vez, carga un catálogo de prueba con materias primas, productos y recetas:

```bash
mysql -u horneo -p horneo < datos-ejemplo.sql
```

Se ejecuta una sola vez, sobre una base de datos sin materias primas ni productos.

### 4. Aplicación Android

Abre la carpeta `android/` en Android Studio y ejecútala en un emulador. La aplicación busca el servidor en `http://10.0.2.2:8080/`, que es la dirección con la que el emulador ve el ordenador donde corre. Entra con el usuario administrador que has definido en `secrets.properties`.

Requiere Android 8.0 (API 26) o superior.

## Pruebas

```bash
# Servidor (necesita MySQL en marcha)
cd backend && ./mvnw test

# Aplicación
cd android && ./gradlew test
```

## Documentación

- [Diario del proyecto](docs/diario.md): lo que se hizo cada día, las decisiones tomadas y los problemas encontrados.
- [Modelo entidad-relación](docs/diagramas/modelo-er.md).

## Autor

Rolando Raúl Ocaña Parra
