# DepósitoClick — PoS

Trabajo Práctico Integrador · Backend de Aplicaciones 2026 · UTN FRC
400677 - Martino Lautaro Emanuel

Backend de control de stock **multi-rubro y multi-depósito**: catálogo polimórfico (cada rubro define sus
categorías y atributos), stock por lote y vencimiento con despacho FEFO, punto de venta, órdenes de compra
y reposición sugerida usando los feriados del país del depósito (API pública).

> Las decisiones de diseño, la deconstrucción del dominio, los diagramas C4 y las alternativas consideradas
> están en **[decisiones.pdf](decisiones.pdf)**.

## Componentes

| Carpeta | Qué es | Puerto (interno) |
|---|---|---|
| `frontend/` | App web React + Vite (login con Keycloak), servida por nginx | **3000** (publicado) |
| `deposito-commons/` | JAR Maven común (errores, roles de Keycloak, RestClient) | — |
| `api-gateway/` | Spring Cloud Gateway: punto de entrada, Security Filter, Swagger UI unificado | **8080** (publicado) |
| `ms-catalogo/` | Rubros, categorías con atributos dinámicos, artículos | 8081 |
| `ms-inventario/` | Depósitos, existencias por lote, movimientos (FEFO, idempotencia) | 8082 |
| `ms-compras/` | Proveedores, órdenes de compra, recepción, reposición, feriados | 8083 |
| `keycloak/` | Realm `depositoclick` (roles y usuarios) | **9090** (publicado) |
| `db/` | Script que crea una base y un usuario por microservicio en PostgreSQL | 5432 |
| `docs/diagramas/` | C1 y C2 en C4-PlantUML (+ PNG) | |
| `scripts/demo.sh` | Prueba de punta a punta de todos los procesos | |

## Levantar todo (Docker)

```bash
docker compose up -d --build
```

Solo quedan publicados el frontend (`http://localhost:3000`), el gateway (`http://localhost:8080`) y Keycloak
(`http://localhost:9090`). Si el puerto 8080 está ocupado: `GATEWAY_PORT=8088 docker compose up -d --build`
(el frontend no cambia: habla con el gateway por la red interna).

- **Aplicación web: http://localhost:3000** → redirige al login de Keycloak; entrar con un usuario de la tabla.
  El menú muestra solo lo que permite el rol.
- Swagger UI (los 3 servicios): http://localhost:8080/swagger-ui.html → *Authorize* → `keycloak (OAuth2, password)`,
  client_id `depositoclick-client`, usuario y contraseña de la tabla.
- Consola de Keycloak: http://localhost:9090 (admin / admin).

### Usuarios de prueba

| Usuario | Contraseña | Rol |
|---|---|---|
| admin1 | admin123 | ADMIN |
| compras1 | compras123 | COMPRAS |
| operario1 | operario123 | OPERARIO |
| vendedor1 | vendedor123 | VENDEDOR |

### Obtener un token y llamar a la API

```bash
TOKEN=$(curl -s -X POST http://localhost:9090/realms/depositoclick/protocol/openid-connect/token \
  -d grant_type=password -d client_id=depositoclick-client -d username=vendedor1 -d password=vendedor123 \
  | sed -E 's/.*"access_token":"([^"]+)".*/\1/')

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/articulos
```

### Demo completa

```bash
bash scripts/demo.sh            # o: GW=http://localhost:8088 bash scripts/demo.sh
```

Recorre seguridad, catálogo, stock inicial, ventas FEFO, transferencias, ajustes, feriados, órdenes de compra
(emisión + recepción parcial y total) y reposición, verificando el código HTTP de cada paso (35 pasos).
Está pensada para una base recién creada (`docker compose down -v` para empezar de cero).

## Compilar y testear sin Docker

```bash
cd deposito-commons && mvn install      # instala el JAR común en ~/.m2 (requisito de los demás)
cd ../ms-catalogo   && mvn verify
cd ../ms-inventario && mvn verify
cd ../ms-compras    && mvn verify
cd ../api-gateway   && mvn verify
```

Los tests usan H2 y JWT simulados (no necesitan Docker). Para correr un servicio suelto sin PostgreSQL:
`mvn spring-boot:run -Dspring-boot.run.profiles=h2` (necesita Keycloak en `localhost:9090`).

### Frontend en modo desarrollo (opcional)

```bash
cd frontend
npm install
npm run dev                                   # http://localhost:5173 (proxy de /api al gateway en :8080)
GATEWAY_URL=http://localhost:8088 npm run dev # si el gateway está en otro puerto
```

## Tecnologías

Java 21 · Maven · Spring Boot 3.5 · Spring Web · Spring Data JPA · Bean Validation · Spring Security
(OAuth2 Resource Server) · Spring Cloud Gateway 2025.0 (WebFlux) · RestClient · springdoc-openapi ·
Lombok · PostgreSQL 16 · H2 (tests) · Keycloak 26 · React 18 + Vite + keycloak-js · nginx · Docker Compose · APIs públicas Nager.Date y ArgentinaDatos.
