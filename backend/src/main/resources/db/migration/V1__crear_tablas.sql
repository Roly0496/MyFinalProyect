-- =====================================================================
-- Horneo - Esquema inicial de la base de datos
-- =====================================================================

-- ---------------------------------------------------------------------
-- Usuarios de la aplicación (administradores y empleados)
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(100)  NOT NULL,
    username       VARCHAR(50)   NOT NULL,
    password_hash  VARCHAR(100)  NOT NULL,
    rol            ENUM('ADMIN', 'EMPLEADO') NOT NULL,
    activo         BOOLEAN       NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_username UNIQUE (username)
);

-- ---------------------------------------------------------------------
-- Materias primas del almacén (harina, mantequilla, huevos...)
-- Las cantidades se guardan en la unidad base: gramos, mililitros o unidades
-- ---------------------------------------------------------------------
CREATE TABLE materia_prima (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    nombre           VARCHAR(100)   NOT NULL,
    unidad           ENUM('GRAMO', 'MILILITRO', 'UNIDAD') NOT NULL,
    stock_actual     DECIMAL(12, 2) NOT NULL DEFAULT 0,
    stock_minimo     DECIMAL(12, 2) NOT NULL DEFAULT 0,
    precio_unitario  DECIMAL(12, 6) NOT NULL DEFAULT 0,
    activo           BOOLEAN        NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_materia_prima PRIMARY KEY (id),
    CONSTRAINT uk_materia_prima_nombre UNIQUE (nombre),
    CONSTRAINT ck_materia_prima_stock_actual CHECK (stock_actual >= 0),
    CONSTRAINT ck_materia_prima_stock_minimo CHECK (stock_minimo >= 0),
    CONSTRAINT ck_materia_prima_precio CHECK (precio_unitario >= 0)
);

-- ---------------------------------------------------------------------
-- Productos que se venden:
--   ELABORADO: se fabrica en el obrador a partir de una receta (pan, croissant...)
--   REVENTA:   se compra ya hecho a un proveedor (zumos, agua...)
-- ---------------------------------------------------------------------
CREATE TABLE producto (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(100)   NOT NULL,
    tipo           ENUM('ELABORADO', 'REVENTA') NOT NULL,
    precio_venta   DECIMAL(10, 2) NOT NULL,
    precio_compra  DECIMAL(10, 2) NULL,
    stock_actual   INT            NOT NULL DEFAULT 0,
    activo         BOOLEAN        NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_producto PRIMARY KEY (id),
    CONSTRAINT uk_producto_nombre UNIQUE (nombre),
    CONSTRAINT ck_producto_precio CHECK (precio_venta >= 0),
    CONSTRAINT ck_producto_precio_compra
        CHECK (tipo = 'ELABORADO' OR precio_compra IS NOT NULL),
    CONSTRAINT ck_producto_precio_compra_positivo
        CHECK (precio_compra IS NULL OR precio_compra >= 0),
    CONSTRAINT ck_producto_stock CHECK (stock_actual >= 0)
);

-- ---------------------------------------------------------------------
-- Receta: cantidad de cada materia prima necesaria para 1 unidad de producto
-- ---------------------------------------------------------------------
CREATE TABLE ingrediente (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    producto_id       BIGINT         NOT NULL,
    materia_prima_id  BIGINT         NOT NULL,
    cantidad          DECIMAL(12, 2) NOT NULL,

    CONSTRAINT pk_ingrediente PRIMARY KEY (id),
    CONSTRAINT uk_ingrediente_producto_materia UNIQUE (producto_id, materia_prima_id),
    CONSTRAINT ck_ingrediente_cantidad CHECK (cantidad > 0),
    CONSTRAINT fk_ingrediente_producto
        FOREIGN KEY (producto_id) REFERENCES producto (id) ON DELETE CASCADE,
    CONSTRAINT fk_ingrediente_materia_prima
        FOREIGN KEY (materia_prima_id) REFERENCES materia_prima (id)
);

-- ---------------------------------------------------------------------
-- Producciones: cada vez que se fabrica un lote de un producto elaborado
-- ---------------------------------------------------------------------
CREATE TABLE produccion (
    id           BIGINT    NOT NULL AUTO_INCREMENT,
    fecha        DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    producto_id  BIGINT    NOT NULL,
    cantidad     INT       NOT NULL,
    usuario_id   BIGINT    NOT NULL,

    CONSTRAINT pk_produccion PRIMARY KEY (id),
    CONSTRAINT ck_produccion_cantidad CHECK (cantidad > 0),
    CONSTRAINT fk_produccion_producto
        FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT fk_produccion_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    INDEX idx_produccion_fecha (fecha)
);

-- ---------------------------------------------------------------------
-- Ventas: cabecera (quién, cuándo y cuánto en total)
-- ---------------------------------------------------------------------
CREATE TABLE venta (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    fecha       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_id  BIGINT         NOT NULL,
    total       DECIMAL(10, 2) NOT NULL,

    CONSTRAINT pk_venta PRIMARY KEY (id),
    CONSTRAINT ck_venta_total CHECK (total >= 0),
    CONSTRAINT fk_venta_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    INDEX idx_venta_fecha (fecha)
);

-- ---------------------------------------------------------------------
-- Líneas de cada venta: qué productos, cuántos y a qué precio
-- El precio se copia en el momento de la venta para conservar el histórico
-- ---------------------------------------------------------------------
CREATE TABLE detalle_venta (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    venta_id         BIGINT         NOT NULL,
    producto_id      BIGINT         NOT NULL,
    cantidad         INT            NOT NULL,
    precio_unitario  DECIMAL(10, 2) NOT NULL,

    CONSTRAINT pk_detalle_venta PRIMARY KEY (id),
    CONSTRAINT uk_detalle_venta_venta_producto UNIQUE (venta_id, producto_id),
    CONSTRAINT ck_detalle_venta_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_detalle_venta_precio CHECK (precio_unitario >= 0),
    CONSTRAINT fk_detalle_venta_venta
        FOREIGN KEY (venta_id) REFERENCES venta (id) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_venta_producto
        FOREIGN KEY (producto_id) REFERENCES producto (id)
);

-- ---------------------------------------------------------------------
-- Movimientos de stock: historial de todo lo que entra y sale.
-- Cada movimiento afecta a una materia prima O a un producto (nunca a los dos).
-- La cantidad lleva signo: positiva si el stock sube, negativa si baja.
-- ---------------------------------------------------------------------
CREATE TABLE movimiento (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    fecha             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo              ENUM('ENTRADA', 'PRODUCCION', 'VENTA', 'MERMA', 'AJUSTE') NOT NULL,
    materia_prima_id  BIGINT         NULL,
    producto_id       BIGINT         NULL,
    cantidad          DECIMAL(12, 2) NOT NULL,
    motivo            VARCHAR(255)   NULL,
    usuario_id        BIGINT         NOT NULL,
    produccion_id     BIGINT         NULL,
    venta_id          BIGINT         NULL,

    CONSTRAINT pk_movimiento PRIMARY KEY (id),

    -- Exactamente uno de los dos: materia prima o producto
    CONSTRAINT ck_movimiento_elemento
        CHECK ((materia_prima_id IS NULL) <> (producto_id IS NULL)),

    CONSTRAINT ck_movimiento_cantidad CHECK (cantidad <> 0),

    -- Las entradas suman stock; las ventas y mermas lo restan
    CONSTRAINT ck_movimiento_signo
        CHECK ((tipo <> 'ENTRADA' OR cantidad > 0)
           AND (tipo NOT IN ('VENTA', 'MERMA') OR cantidad < 0)),

    -- Los movimientos de producción y venta deben indicar qué operación los generó
    CONSTRAINT ck_movimiento_origen
        CHECK ((tipo <> 'PRODUCCION' OR produccion_id IS NOT NULL)
           AND (tipo <> 'VENTA' OR venta_id IS NOT NULL)),

    CONSTRAINT fk_movimiento_materia_prima
        FOREIGN KEY (materia_prima_id) REFERENCES materia_prima (id),
    CONSTRAINT fk_movimiento_producto
        FOREIGN KEY (producto_id) REFERENCES producto (id),
    CONSTRAINT fk_movimiento_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_movimiento_produccion
        FOREIGN KEY (produccion_id) REFERENCES produccion (id),
    CONSTRAINT fk_movimiento_venta
        FOREIGN KEY (venta_id) REFERENCES venta (id),
    INDEX idx_movimiento_fecha (fecha)
);
