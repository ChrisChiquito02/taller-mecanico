# Avance global del sistema de taller

## Estado al 21 de octubre de 2026

| Módulo o requisito | Estado | Evidencia en el proyecto |
|---|---|---|
| Spring Boot, React y MySQL en Docker Compose | ✅ Implementado | Servicios `backend`, `frontend` y `mysql`; volumen persistente `mysql_data` |
| Health check del backend | ✅ Implementado | `GET /actuator/health` habilitado y probado con estado `UP` |
| Identificadores UUID versión 7 con campo `fecha` | ✅ Implementado | Entidades de usuarios, roles, talleres, clientes, vehículos y fotos |
| Registro de usuarios | ✅ Implementado | Formulario con nombre, apellidos, teléfono, email, contraseña y confirmación |
| Inicio de sesión | ✅ Implementado | JWT, contraseña BCrypt y filtro de autenticación |
| Sesión vencida o inválida | ✅ Manejo HTTP corregido | Backend responde 401; React limpia la sesión vencida y vuelve al login |
| Recuperación de contraseña | ⚠️ Flujo con token implementado; falta transport de correo | El enlace temporal se escribe en los logs del backend porque no se configuró SMTP |
| Creación y consulta de roles | ✅ Implementado | Factory Method en backend y modal/listado en React |
| Cambio de nombre y suspensión de roles | ✅ Implementado | PUT de actualización y PATCH de suspensión lógica; no elimina registros |
| Autorización para operaciones de roles | ✅ Implementado | Protegida por la infraestructura JWT existente |
| Alta de taller | ✅ Implementado | Factory Method, API multipart, validación y formulario React |
| Estados comerciales del taller | ✅ Implementado | `activo`, `inactivo`, `suspendido` y `mantenimiento` |
| Validación de RFC y duplicados de taller | ✅ Implementado | Normalización y restricción única persistida |
| Idempotencia en alta de taller | ✅ Implementado | Cabecera `Idempotency-Key` y huella de la solicitud |
| Foto de taller | ✅ Implementado | Solo imágenes permitidas, máximo 15 MB y volumen Docker persistente |
| Relación taller-cliente | ✅ Implementado | Tabla junction `workshop_clients` |
| Consulta postal por código postal y ubicación | ⚠️ Integración implementada; necesita `POSTALKIT_API_KEY` | Proxy del backend y formularios; la clave actual está vacía |
| Registro de clientes | ✅ Implementado | Formulario completo, normalización, duplicados y asociación obligatoria con talleres existentes |
| Administración de clientes | ✅ Implementado | Búsqueda, edición, suspensión lógica, máximo de 10 por página y orden ASC/DESC en servidor |
| Autorización de operaciones de clientes | ✅ Implementado | Requiere JWT con autoridad `ADMIN` o `RECEPCIONISTA` |
| Validaciones de clientes en frontend y backend | ✅ Implementado | Expresiones regulares en React y Bean Validation más reglas de servicio |
| Diagramas Archify previos | ✅ Renderizados y comprobados | Flujos HTML previos; renderer y comprobador de Archify reportan cero errores, algunos avisos visuales |
| Documentación del código nuevo | ⚠️ Documentación por tipo, campo y método | Javadocs/JSDoc y comentarios explican contratos y bloques funcionales; no se añadió una explicación redundante después de cada instrucción elemental |
| Alta y asociación de vehículos | ✅ Implementado | VIN/placa normalizados, cliente activo obligatorio, fotos múltiples y `mechanic_id` nullable |
| Administración de vehículos | ✅ Implementado | Listado paginado, búsqueda/orden servidor, detalle protegido y baja lógica |
| Detección de vehículos duplicados | ✅ Implementado | Comprobación previa de VIN/placa y restricciones `UNIQUE` en MySQL |
| Patrones para vehículos | ✅ Implementado | Factory Method para entidad, Strategy de placa y Facade React |
| Diagramas de vehículos | ✅ Renderizados y validados | Registro y administración en `diagramas/archify/`; cero errores de validación |

## Cuentas y acceso

El catálogo de roles contiene `admin`, `mecanico` y `recepcionista`. Las cuentas autorizadas de monitoreo se conservan con las credenciales entregadas en la conversación del proyecto: `admin@tallermecanico.test` y `recepcion@tallermecanico.test`. Las contraseñas no se duplican en este archivo. El resto de las cuentas no recibe roles automáticamente.

El alta y la administración de clientes y talleres requieren una cuenta con `ADMIN` o `RECEPCIONISTA`. El registro de cliente requiere seleccionar al menos un taller existente. La base persistente parte sin talleres ni clientes, así que primero se debe registrar un taller usando una cuenta autorizada.

## Ejecución local

Desde la raíz del proyecto:

```powershell
docker compose up --build -d
docker compose ps
```

La interfaz se sirve en `http://localhost:5173`, la API en `http://localhost:8080` y MySQL publica el puerto `3307` del equipo. Las contraseñas de la base se configuran en `.env`; no se deben publicar ni copiar a documentación compartida. Los datos sobreviven al reinicio mediante `mysql_data`.

## Operaciones implementadas

| Operación | Endpoint |
|---|---|
| Listar roles | `GET /api/roles` |
| Crear rol | `POST /api/roles` |
| Renombrar rol | `PUT /api/roles/{id}` |
| Suspender o reactivar rol | `PATCH /api/roles/{id}/status` |
| Listar talleres activos para asociación | `GET /api/workshops/catalog` |
| Listar clientes paginados/ordenados | `GET /api/clients?page=0&size=10&term=&direction=DESC` |
| Crear cliente | `POST /api/clients` |
| Actualizar cliente | `PUT /api/clients/{id}` |
| Suspender cliente lógicamente | `DELETE /api/clients/{id}` |
| Listar vehículos activos | `GET /api/vehicles?page=0&size=10&term=&direction=DESC` |
| Comprobar VIN y placa | `GET /api/vehicles/duplicates?vin=...&plate=...` |
| Registrar vehículo y fotos | `POST /api/vehicles` (multipart: `data`, `photos` repetido) |
| Consultar ficha de vehículo | `GET /api/vehicles/{id}` |
| Consultar foto protegida | `GET /api/vehicles/{id}/photos/{photo_id}` |
| Desactivar vehículo lógicamente | `DELETE /api/vehicles/{id}` |
| Estados postales | `GET /api/workshops/catalog/states` |
| Municipios por estado | `GET /api/workshops/catalog/states/{state_id}/municipalities` |
| Dirección por CP | `GET /api/workshops/catalog/postal-codes/{code}` |
| CP por ubicación | `GET /api/workshops/catalog/postal-codes/search?state=...&municipality=...&colonia=...` |

## Archify

`archify/` contiene el renderer integrado. `diagramas/archify/` contiene las fuentes de los flujos del sistema y las salidas HTML generadas. El procedimiento de render y comprobación se documenta en [diagramas_archify.md](diagramas_archify.md). Los diagramas de vehículos están incorporados en esta fase.

## Verificación y límites del entorno

La compilación de producción de React termina correctamente con `npm run build`. Todos los diagramas Archify terminan con cero errores (algunos flujos previos conservan advertencias visuales). No se pudo compilar ni ejecutar el stack en Docker durante esta verificación: el daemon de Docker Desktop no estaba disponible; tampoco hay Maven instalado en el host. Por ello no se puede certificar aquí la ejecución del backend ni una prueba de extremo a extremo.

La consulta de estados, municipios y códigos postales necesita una clave real de PostalKit en `POSTALKIT_API_KEY`. Sin ella, el servicio responde `503`; la aplicación no simula resultados y el formulario de taller no puede cargar el catálogo postal. La recuperación persiste un token temporal en hash y escribe el enlace en los logs del backend; no hay transport SMTP configurado para enviarlo por email. Para verificar un alta vehicular real se requiere un taller y un cliente activo registrado; esta documentación no crea datos ficticios en la base persistente. La Strategy implementa un patrón genérico de placas mexicanas normalizadas, sin afirmar que sustituye validación específica por entidad, servicio o tipo de placa.
