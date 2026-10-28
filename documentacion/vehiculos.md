# Módulos de vehículos

## Flujo y reglas

El portal solo muestra las rutas de vehículos a `ADMIN` y `RECEPCIONISTA`; Spring vuelve a exigir esos roles para toda la API `/api/vehicles/**`. El formulario busca clientes mediante el catálogo paginado de clientes activos. Spring bloquea la fila del cliente mientras procesa el alta y rechaza asociaciones inexistentes o inactivas.

El VIN se recorta, transforma a mayúsculas y valida con 17 caracteres, excluyendo `I`, `O` y `Q`. La placa pasa por la Strategy `mexican_plate_normalization_strategy`: se transforma a mayúsculas, se eliminan espacios y guiones, se excluyen `I`, `Ñ`, `O` y `Q`, y se exige una clave de 5 a 8 caracteres. La regla no pretende identificar la secuencia de placas de un estado o servicio concreto: la NOM-001-SCT-2-2016 contempla series por tipo y entidad federativa. Por ello se evita afirmar que una regex genérica sustituya la validación de la autoridad local. La Strategy permite sustituir o extender el criterio cuando se defina entidad y clase de placa. Referencias: [NOM-001-SCT-2-2016 en DOF](https://sidof.segob.gob.mx/notas/5442476), [Marco normativo de especificaciones vehiculares](https://micrs.sct.gob.mx/index.php/transporte-y-medicina-preventiva/autotransporte-federal/marco-normativo/noms-especificacion-de-vehiculos).

Marca, modelo, versión y color se recortan, colapsan espacios y guardan en mayúsculas; el kilometraje puede ser nulo. La base impone unicidad de VIN y placa también contra vehículos inactivos. El formulario consulta duplicados antes del POST y el servicio repite las consultas; las restricciones únicas resuelven carreras concurrentes.

El alta guarda vehículo y cero o más fotos. Cada archivo se valida de forma independiente: solo JPEG, PNG, GIF o BMP decodificables, hasta 15 MB. Los metadatos se guardan en `vehicle_photos` y los bytes en el volumen persistente de uploads. Las fotos se sirven con JWT y solo cuando pertenecen al vehículo solicitado. La desactivación cambia `status` a `inactivo`; no elimina el registro, la relación ni las fotos.

`vehicles.mechanic_id` es anulable y referencia `users.id`. En esta fase no hay control en pantalla ni endpoint que asigne mecánico: la columna queda lista para una fase posterior.

## API

| Método | Ruta | Función |
|---|---|---|
| `GET` | `/api/vehicles?page=0&size=10&term=&direction=DESC` | Listado activo, búsqueda y orden por fecha; tamaño máximo 10 |
| `GET` | `/api/vehicles/duplicates?vin=...&plate=...` | Detectores normalizados previos al envío |
| `POST` | `/api/vehicles` | `multipart/form-data`: parte JSON `data`, partes de archivo repetidas `photos` |
| `GET` | `/api/vehicles/{id}` | Detalle del vehículo activo y sus asociaciones |
| `GET` | `/api/vehicles/{id}/photos/{photo_id}` | Imagen asociada con autorización |
| `DELETE` | `/api/vehicles/{id}` | Desactivación lógica |

## Patrones aplicados

- Backend Factory Method: `vehicle_factory` y `uuid_v7_vehicle_factory` construyen el agregado y sus datos normalizados.
- Backend Strategy: `plate_normalization_strategy` permite reemplazar la política de placa sin cambiar el caso de uso.
- Frontend Facade: `VehicleFacade.jsx` concentra pantalla, validación, búsqueda de clientes, archivos, API y resultados.

## Comentarios del código

Los tipos, campos de dominio, endpoints, métodos y funciones de interfaz nuevos llevan comentarios Javadoc o JSDoc. Las explicaciones de flujo y decisiones están ampliadas en los diagramas y secciones anteriores; los comentarios se colocan en bloques ejecutables y contratos para conservar el código legible.
