# Fase 02 - Registro de Nuevo Cliente

## Estado general

La Fase 02 entrega el caso de uso de alta de cliente protegido por JWT y permisos, con validacion de datos, fotografia, direccion asistida con SEPOMEX y una interfaz responsive de una sola pantalla. El alcance de empresa/taller permanece fuera de esta fase.

| Actividad | Estado actual | Evidencia tecnica |
| --- | --- | --- |
| Autenticacion de usuario operador | [Realizado] | Login JWT existente y `GET /api/auth/me` recuperan la sesion para la vista. |
| Autorizacion ADMIN / RECEPCIONISTA | [Realizado] | `DataInitializer` concede `CLIENTS_WRITE` a `ADMINISTRADOR` y `RECEPCIONISTA`; `/api/customers` exige `PERM_CLIENTS_WRITE`. |
| Patron Facade | [Realizado] | `CustomerController` delega en `CustomerFacade`; `AuthController` usa `AuthFacade`. La vista no accede directamente a repositorios. |
| Formulario en una sola pantalla | [Realizado] | `frontend/src/App.vue` organiza identidad, contacto/fotografia y direccion en grids responsive; no utiliza steppers. |
| Captura de datos personales | [Realizado] | Nombre completo, nombre alternativo, edad y fecha de nacimiento son obligatorios y se validan en frontend y DTO. |
| Captura de contacto | [Realizado] | Telefono personal, telefono de trabajo y correo principal son obligatorios; correo laboral es opcional. |
| Captura de direccion | [Realizado] | Calle, colonia, municipio/alcaldia, estado y CP se envian al backend; el CP debe tener cinco digitos. |
| Validacion de edad y fecha | [Realizado] | `CustomerFacade` compara la edad declarada con `Period.between(birthDate, now)`. |
| Subida de fotografia | [Realizado] | Se usa `multipart/form-data`, selector y drag and drop con previsualizacion. |
| Validacion de fotografia | [Realizado] | Backend limita a 15 MB, identifica las firmas JPEG/PNG/WEBP y usa UUID para el nombre de almacenamiento. |
| Prevencion de duplicados | [Realizado] | Se verifican correo personal/laboral, telefonos y nombre normalizado + fecha; restricciones MySQL cubren concurrencia. |
| Alerta de duplicados | [Realizado] | `DuplicateCustomerException` se traduce a HTTP 409 y la vista presenta el mensaje. |
| Alerta de alta exitosa | [Realizado] | Tras `201 Created`, Vue muestra `Nuevo cliente creado: <nombre>`. |
| Auditoria de creador | [Realizado] | La entidad cliente conserva el usuario autenticado que realizo el alta. |
| Autocompletado por codigo postal | [Realizado] | Al llegar a cinco digitos, SEPOMEX llena estado, municipio y lista de colonias mediante `/api/v1/zip_codes`. |
| Seleccion manual de direccion | [Realizado] | Estado -> municipio -> colonia usa `/states`, `/states/{id}/municipalities` y consulta filtrada de colonias. |
| Respaldo de SEPOMEX | [Realizado] | El frontend usa `VITE_SEPOMEX_API_URL`, local `http://localhost:3005` y respaldo `https://sepomex.kurenn.dev`; permite captura manual si falla. |
| Servicio SEPOMEX en Docker | [Realizado] | `sepomex-api` usa `icalia/sepomex:latest`, contenedor `taller_sepomex_api` y publica `3005:80`. El contenedor fue iniciado y consultado. |
| Paleta Warm Sunset y responsive | [Realizado] | Se aplican rose, fuchsia y amber en acciones/foco; layout mobile-first, grids Tailwind y fondos slate. |
| Diagrama retirado de la SPA | [Realizado] | No quedan referencias al diagrama de componentes dentro de `frontend/src`. |
| Diagrama HTML independiente | [Realizado con contingencia] | `diagrama_componentes.html` presenta Vue 3, Spring Boot, MySQL y SEPOMEX con cajas y conectores. |
| Ejecucion de Archify | [Pendiente de herramienta] | No se encontro ejecutable local y `npx archify --help` fallo porque el paquete no expone CLI. El HTML independiente se mantiene como entrega verificable. |
| Asociacion empresa - taller - cliente | [Pendiente] | Requiere entidades `Company`, `Workshop` y una relacion que garantice talleres de una misma empresa. No se implemento para respetar el alcance. |
| Pruebas automatizadas de la Fase 02 | [Pendiente] | Se realizaron verificaciones de compilacion y funcionamiento local; faltan pruebas unitarias e integracion automatizadas. |

## Archivos principales de la fase

| Archivo | Descripcion |
| --- | --- |
| `frontend/src/App.vue` | Formularios, control de permisos visual, carga de foto, direccion y retroalimentacion. |
| `frontend/src/services/sepomex.js` | Cliente de catalogo SEPOMEX con URL configurable y respaldo. |
| `frontend/.env.example` | Ejemplo de la variable `VITE_SEPOMEX_API_URL`. |
| `backend/src/main/java/com/refaccionaria/auth/controller/CustomerController.java` | Endpoint multipart `POST /api/customers`. |
| `backend/src/main/java/com/refaccionaria/auth/service/CustomerFacade.java` | Reglas de registro, duplicados, auditoria y persistencia. |
| `backend/src/main/java/com/refaccionaria/auth/service/CustomerPhotoStorageService.java` | Validacion y almacenamiento seguro de imagenes. |
| `backend/src/main/java/com/refaccionaria/auth/repository/CustomerRepository.java` | Consultas JPA para detectar colisiones de datos. |
| `backend/src/main/java/com/refaccionaria/auth/config/SecurityConfig.java` | Proteccion de rutas por autoridad. |
| `docker-compose.yml` | MySQL y servicio local SEPOMEX. |
| `diagrama_componentes.html` | Diagrama autonomo de los cuatro componentes principales. |
| `docs/DOCUMENTACION_CODIGO.md` | Documentacion completa de implementacion, contratos y limites. |

## Contrato del alta de cliente

| Metodo | Ruta | Seguridad | Entrada | Resultado |
| --- | --- | --- | --- | --- |
| `POST` | `/api/customers` | JWT y `PERM_CLIENTS_WRITE` | `multipart/form-data` con datos de cliente y `photo` | `201` creado; `400` validacion; `401/403` sin autorizacion; `409` duplicado. |

## Verificaciones realizadas

- Compilacion del frontend con `yarn build` completada correctamente.
- Sintaxis de Docker Compose validada.
- Contenedor `taller_sepomex_api` activo en el puerto `3005`.
- Endpoint de salud de SEPOMEX y consultas de estados/CP verificados localmente.
- Autocompletado visual probado con el CP `64000`: Nuevo Leon, Monterrey y colonias disponibles.
