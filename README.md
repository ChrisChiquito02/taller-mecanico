# Torque · Sistema de taller mecánico

Sistema de cuentas para un taller mecánico. Incluye registro, inicio de sesión, recuperación y cambio de contraseña, creación de roles para cualquier usuario autenticado, seguridad JWT/BCrypt, y MySQL dentro de Docker.

## Requisitos

- Docker Desktop con Docker Compose.
- Para desarrollo local: Java 17, Maven y Node.js 22 o superior.

## Ejecutar todo con Docker

1. Copia `.env.example` a `.env` si aún no existe. Cambia las contraseñas de la base y `JWT_SECRET` por valores propios; la clave JWT debe tener al menos 32 caracteres.
   Para usar los selectores de estado, municipio, colonia y código postal del taller, agrega una clave válida `POSTALKIT_API_KEY` en `.env` (se obtiene desde [PostalKit](https://api.postalkit.mx/reference)). Sin esa credencial, PostalKit responde `401` y el backend informa `503`; los selectores no pueden cargar catálogos.
2. Desde esta carpeta ejecuta:

   ```sh
   docker compose up --build
   ```

3. Abre <http://localhost:5173>. El API está en <http://localhost:8080> y MySQL queda disponible desde tu computadora en el puerto 3307 (internamente usa el 3306).

Si agregas o cambias `POSTALKIT_API_KEY`, reconstruye solo el backend para aplicar la variable: `docker compose up --build -d backend`.

Para detener: `docker compose down`. Los datos persisten en el volumen `mysql_data`.

## Desarrollo local

- Inicia la base con `docker compose up -d mysql` (disponible en `localhost:3307`).
- Backend: `cd backend` y `mvn spring-boot:run` (Java 17 y Maven).
- Frontend: `cd frontend`, `npm install` y `npm run dev`; Vite servirá la app en `http://localhost:5173`.

## Recuperación de contraseña

El endpoint responde de forma uniforme para no revelar si un correo existe. En este starter, el enlace temporal (30 minutos por defecto) se escribe en los registros del contenedor del backend para permitir probar el flujo. Consulta `docker compose logs -f backend`, copia el enlace y ábrelo en el navegador. Antes de producción, integra un proveedor de correo y configura sus credenciales mediante variables de entorno.

## API

- `POST /api/auth/register` `{ "name", "email", "password" }`
- `POST /api/auth/login` `{ "email", "password" }`
- `POST /api/auth/forgot-password` `{ "email" }`
- `POST /api/auth/reset-password` `{ "token", "password" }`
- `GET /api/me` requiere `Authorization: Bearer <JWT>`
- `GET /api/roles/exists?name=...` requiere autenticación y valida duplicados.
- `POST /api/roles` `{ "name": "Jefe de taller" }` requiere autenticación y crea un rol.

El registro público crea cuentas sin rol. Todas las operaciones protegidas requieren un JWT válido; cualquier usuario autenticado puede usar `/api/roles/**`.

## Creación de roles (Parte 2)

- Inicia sesión con cualquier cuenta y selecciona **Crear rol** en el portal.
- La pantalla valida el nombre con una expresión regular antes de enviar peticiones; acepta letras españolas, espacios, apóstrofes y guiones, con máximo 80 caracteres.
- La vista consulta duplicados en `/api/roles/exists` y el backend vuelve a validarlos al crear mediante `POST /api/roles`.
- Los roles nuevos se guardan como registros independientes. No se asignan a usuarios. El backend crea cada entidad mediante el Factory Method `RoleFactory`.

## Identificadores y fecha

Las entidades usan UUIDv7 como texto canónico de 36 caracteres; no hay columnas autoincrementales. Los primeros 48 bits del UUID codifican el tiempo Unix en milisegundos. Las tablas guardan además `fecha` en UTC, derivada del timestamp del mismo UUID, para poder ordenar o filtrar explícitamente por fecha.

## Estructura

- `backend/`: Spring Boot, capas `auth`, `security` y `user`.
- `frontend/`: React, Vite y vistas adaptables para autenticación y portal.
- `database/`: inicialización de MySQL.
- `docker-compose.yml`: MySQL `latest`, API y frontend.

Las claves de ejemplo son únicamente para desarrollo. Define secretos propios en `.env` antes de exponer el sistema a una red.
