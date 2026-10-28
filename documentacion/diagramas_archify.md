# Diagramas de proceso con Archify

El repositorio incluye el renderer open source de Archify en `archify/`. Las fuentes workflow y sus páginas HTML están en `diagramas/archify/`.

## Generar y validar los diagramas

Requisitos: Node.js compatible con el proyecto y PowerShell o una terminal en la raíz del repositorio.

```powershell
npm run diagrams:build
```

El script `scripts/render-diagrams.mjs` encuentra cada `*.workflow.json`, genera el `.html` correspondiente y ejecuta el comprobador de salida de Archify. La consola muestra el resultado por archivo, incluyendo errores y advertencias de composición. Un estado distinto de cero indica que al menos una fuente necesita corrección.

## Catálogo de flujos

| Fuente | Contenido |
|---|---|
| `flujo_cuentas.workflow.json` | Registro público, validación, hash de contraseña y persistencia de usuario |
| `flujo_login.workflow.json` | Validación de credenciales, verificación BCrypt, emisión JWT y manejo de errores |
| `flujo_recuperacion.workflow.json` | Solicitud, token temporal, verificación y cambio de contraseña |
| `flujo_roles.workflow.json` | Validación, Factory Method, creación y consulta en modal |
| `gestion_roles.workflow.json` | Listado, cambio de nombre, detección de duplicados y suspensión lógica |
| `alta_taller.workflow.json` | Autorización, validaciones, idempotencia, foto, Factory Method y persistencia |
| `catalogo_postal.workflow.json` | Consulta por CP e inversa por estado, municipio y colonia mediante proxy |
| `clientes.workflow.json` | Registro, autorización, validación, duplicados, asociación junction y respuesta |
| `clientes_admin.workflow.json` | Filtros, paginación del servidor y del navegador, actualización y suspensión |
| `registro_vehiculos.workflow.json` | Autorización, cliente activo, regex, VIN/placa, fotos, Factory Method y transacción |
| `administracion_vehiculos.workflow.json` | Paginación/orden servidor, detalle y fotos protegidas, desactivación lógica |

Los archivos `*.workflow.html` son artefactos generados, no se editan directamente. Para cambiar un flujo, edita su fuente JSON y vuelve a ejecutar el comando.

## Límite externo de la consulta postal

El diagrama muestra el proceso de integración, pero las consultas reales requieren `POSTALKIT_API_KEY` configurada en el `.env` del entorno de ejecución. Sin esa credencial, el backend responde con servicio no disponible. Referencia oficial: [API de PostalKit](https://api.postalkit.mx/reference).

## Referencia del renderer

Archify se integra desde su repositorio open source: [tt-a1i/archify](https://github.com/tt-a1i/archify). El renderer ejecutado es el workflow renderer incluido en el checkout del repositorio.
