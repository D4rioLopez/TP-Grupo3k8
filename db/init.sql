-- Se ejecuta UNA sola vez, cuando el volumen de PostgreSQL esta vacio.
-- Estrategia: una instancia de PostgreSQL, pero una BASE y un USUARIO por microservicio.
-- Cada servicio solo tiene permisos sobre su propia base: nadie puede leer (ni escribir)
-- las tablas de otro servicio aunque compartan el mismo motor.

CREATE USER catalogo WITH PASSWORD 'catalogo123';
CREATE DATABASE catalogo_db OWNER catalogo;
REVOKE ALL ON DATABASE catalogo_db FROM PUBLIC;

CREATE USER inventario WITH PASSWORD 'inventario123';
CREATE DATABASE inventario_db OWNER inventario;
REVOKE ALL ON DATABASE inventario_db FROM PUBLIC;

CREATE USER compras WITH PASSWORD 'compras123';
CREATE DATABASE compras_db OWNER compras;
REVOKE ALL ON DATABASE compras_db FROM PUBLIC;
