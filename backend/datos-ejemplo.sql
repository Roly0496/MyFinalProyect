-- =====================================================================
-- Horneo - Datos de ejemplo para desarrollo y para la demo
-- =====================================================================
-- No es una migración de Flyway (está fuera de db/migration): se ejecuta a mano,
-- una sola vez, sobre una base de datos sin materias primas ni productos.
--
--   mysql -u horneo -p horneo < datos-ejemplo.sql
--
-- Requisitos: que exista el usuario administrador "admin" (lo crea el backend al arrancar).
--
-- Regla del proyecto: el stock solo cambia mediante movimientos. Por eso, además de poner el
-- stock inicial, se registra el movimiento que lo justifica ("Inventario inicial"), para que
-- el stock y el historial cuadren.
--
-- Precios de las materias primas: por unidad base (euros por gramo, por mililitro o por unidad).
-- Recetas: cantidad de cada materia prima para fabricar 1 unidad de producto.
-- =====================================================================

START TRANSACTION;

-- Usuario a cuyo nombre quedan los movimientos iniciales.
-- Si no existe, @admin queda NULL y el primer movimiento falla (usuario_id es NOT NULL),
-- así que la transacción no se confirma.
SET @admin = (SELECT id FROM usuario WHERE username = 'admin');

-- ---------------------------------------------------------------------
-- Materias primas
-- ---------------------------------------------------------------------
INSERT INTO materia_prima (nombre, unidad, stock_actual, stock_minimo, precio_unitario)
VALUES ('Harina de trigo T55', 'GRAMO', 12500, 5000, 0.0008);   -- 0,80 €/kg
SET @harina = LAST_INSERT_ID();

INSERT INTO materia_prima (nombre, unidad, stock_actual, stock_minimo, precio_unitario)
VALUES ('Mantequilla', 'GRAMO', 850, 2000, 0.009);              -- 9,00 €/kg (bajo mínimo)
SET @mantequilla = LAST_INSERT_ID();

INSERT INTO materia_prima (nombre, unidad, stock_actual, stock_minimo, precio_unitario)
VALUES ('Azúcar', 'GRAMO', 4000, 1000, 0.0012);                 -- 1,20 €/kg
SET @azucar = LAST_INSERT_ID();

INSERT INTO materia_prima (nombre, unidad, stock_actual, stock_minimo, precio_unitario)
VALUES ('Leche entera', 'MILILITRO', 6000, 2000, 0.001);        -- 1,00 €/l
SET @leche = LAST_INSERT_ID();

INSERT INTO materia_prima (nombre, unidad, stock_actual, stock_minimo, precio_unitario)
VALUES ('Huevos', 'UNIDAD', 10, 30, 0.25);                      -- 0,25 €/ud (bajo mínimo)
SET @huevos = LAST_INSERT_ID();

INSERT INTO materia_prima (nombre, unidad, stock_actual, stock_minimo, precio_unitario)
VALUES ('Levadura fresca', 'GRAMO', 500, 200, 0.004);           -- 4,00 €/kg
SET @levadura = LAST_INSERT_ID();

INSERT INTO materia_prima (nombre, unidad, stock_actual, stock_minimo, precio_unitario)
VALUES ('Chocolate negro', 'GRAMO', 1500, 500, 0.012);          -- 12,00 €/kg
SET @chocolate = LAST_INSERT_ID();

INSERT INTO materia_prima (nombre, unidad, stock_actual, stock_minimo, precio_unitario)
VALUES ('Sal', 'GRAMO', 2000, 500, 0.0005);                     -- 0,50 €/kg
SET @sal = LAST_INSERT_ID();

-- ---------------------------------------------------------------------
-- Productos
-- ---------------------------------------------------------------------
INSERT INTO producto (nombre, tipo, precio_venta, precio_compra, stock_actual)
VALUES ('Croissant', 'ELABORADO', 1.20, NULL, 24);
SET @croissant = LAST_INSERT_ID();

INSERT INTO producto (nombre, tipo, precio_venta, precio_compra, stock_actual)
VALUES ('Napolitana de chocolate', 'ELABORADO', 1.40, NULL, 18);
SET @napolitana = LAST_INSERT_ID();

INSERT INTO producto (nombre, tipo, precio_venta, precio_compra, stock_actual)
VALUES ('Barra de pan', 'ELABORADO', 0.90, NULL, 40);
SET @barra = LAST_INSERT_ID();

INSERT INTO producto (nombre, tipo, precio_venta, precio_compra, stock_actual)
VALUES ('Ensaimada', 'ELABORADO', 1.60, NULL, 0);               -- agotada
SET @ensaimada = LAST_INSERT_ID();

INSERT INTO producto (nombre, tipo, precio_venta, precio_compra, stock_actual)
VALUES ('Zumo de naranja', 'REVENTA', 1.80, 0.90, 12);
SET @zumo = LAST_INSERT_ID();

INSERT INTO producto (nombre, tipo, precio_venta, precio_compra, stock_actual)
VALUES ('Agua mineral 50 cl', 'REVENTA', 1.00, 0.30, 24);
SET @agua = LAST_INSERT_ID();

-- ---------------------------------------------------------------------
-- Recetas (por 1 unidad de producto). Solo los productos elaborados tienen receta.
-- ---------------------------------------------------------------------
-- Croissant: coste 0,32 € (como en los bocetos)
INSERT INTO ingrediente (producto_id, materia_prima_id, cantidad) VALUES
    (@croissant, @harina, 50),
    (@croissant, @mantequilla, 25),
    (@croissant, @azucar, 6),
    (@croissant, @leche, 15),
    (@croissant, @huevos, 0.1),
    (@croissant, @levadura, 2);

INSERT INTO ingrediente (producto_id, materia_prima_id, cantidad) VALUES
    (@napolitana, @harina, 50),
    (@napolitana, @mantequilla, 25),
    (@napolitana, @azucar, 8),
    (@napolitana, @leche, 15),
    (@napolitana, @huevos, 0.1),
    (@napolitana, @levadura, 2),
    (@napolitana, @chocolate, 12);

INSERT INTO ingrediente (producto_id, materia_prima_id, cantidad) VALUES
    (@barra, @harina, 150),
    (@barra, @levadura, 3),
    (@barra, @sal, 3);

INSERT INTO ingrediente (producto_id, materia_prima_id, cantidad) VALUES
    (@ensaimada, @harina, 45),
    (@ensaimada, @azucar, 12),
    (@ensaimada, @mantequilla, 15),
    (@ensaimada, @huevos, 0.2),
    (@ensaimada, @leche, 10),
    (@ensaimada, @levadura, 2);

-- ---------------------------------------------------------------------
-- Movimientos que justifican el stock inicial
--   Materias primas y productos de reventa: ENTRADA (se compran).
--   Productos elaborados: AJUSTE (una PRODUCCION descontaría ingredientes que no se gastaron).
--   La ensaimada empieza a 0, así que no necesita movimiento.
-- ---------------------------------------------------------------------
INSERT INTO movimiento (tipo, materia_prima_id, producto_id, cantidad, motivo, usuario_id) VALUES
    ('ENTRADA', @harina,      NULL, 12500, 'Inventario inicial', @admin),
    ('ENTRADA', @mantequilla, NULL,   850, 'Inventario inicial', @admin),
    ('ENTRADA', @azucar,      NULL,  4000, 'Inventario inicial', @admin),
    ('ENTRADA', @leche,       NULL,  6000, 'Inventario inicial', @admin),
    ('ENTRADA', @huevos,      NULL,    10, 'Inventario inicial', @admin),
    ('ENTRADA', @levadura,    NULL,   500, 'Inventario inicial', @admin),
    ('ENTRADA', @chocolate,   NULL,  1500, 'Inventario inicial', @admin),
    ('ENTRADA', @sal,         NULL,  2000, 'Inventario inicial', @admin);

INSERT INTO movimiento (tipo, materia_prima_id, producto_id, cantidad, motivo, usuario_id) VALUES
    ('AJUSTE',  NULL, @croissant,  24, 'Inventario inicial', @admin),
    ('AJUSTE',  NULL, @napolitana, 18, 'Inventario inicial', @admin),
    ('AJUSTE',  NULL, @barra,      40, 'Inventario inicial', @admin),
    ('ENTRADA', NULL, @zumo,       12, 'Inventario inicial', @admin),
    ('ENTRADA', NULL, @agua,       24, 'Inventario inicial', @admin);

COMMIT;
