# Documentacion tecnica del codigo

## 1. Alcance y arquitectura

El proyecto implementa el modulo de autenticacion y el alta de clientes del **Gestor de Taller Mecanico / Refaccionaria**. La aplicacion conserva una separacion de responsabilidades:

```text
Vue 3 (vista y experiencia) -> API REST -> Controller -> Facade/Service -> Repository JPA -> MySQL
                                      |
                                      -> JWT y reglas de Spring Security
Vue 3 (direccion) -> API SEPOMEX local/publica de solo lectura
```

El frontend no accede a repositorios ni a MySQL. `CustomerController` depende de `CustomerFacade`, que concentra la orquestacion del caso de uso y delega persistencia a `CustomerRepository`.

> Estado real del frontend: Vue 3 con JavaScript y Composition API (`<script setup>`). Las peticiones se realizan con `fetch` nativo; Axios y TypeScript no estan instalados ni se usan actualmente.

## 2. Frontend

### 2.1 Archivos principales

| Ruta | Responsabilidad |
| --- | --- |
| `frontend/src/App.vue` | Vista unica de login, registro, recuperacion, sesion y alta de clientes. Mantiene el estado de los formularios y coordina los servicios HTTP. |
| `frontend/src/services/sepomex.js` | Adaptador de lectura para el catalogo SEPOMEX; concentra URL base, respaldo y transformacion de colecciones. |
| `frontend/src/style.css` | Base global: tipografia Manrope, fondo crema neutro y foco accesible. |
| `frontend/.env.example` | Ejemplo de `VITE_SEPOMEX_API_URL=http://localhost:3005`. |
| `frontend/vite.config.js` | Proxy de desarrollo para `/api` hacia Spring Boot en `http://localhost:8080`. |

### 2.2 Layout, formularios y UX

`App.vue` usa Tailwind CSS, una estructura mobile-first y contenedores `max-w-7xl`. La interfaz usa fondos `slate` para lectura prolongada y acentos Warm Sunset en rose, fuchsia y amber. Los controles importantes incluyen foco visible `focus:ring-2`, contraste de texto y estados de carga, error y exito.

El registro de cliente se mantiene en **una sola pantalla**, sin stepper, dividido en:

1. **Informacion personal:** nombre completo, nombre alternativo, edad y fecha de nacimiento.
2. **Contacto y fotografia:** telefonos, correos y carga mediante selector o arrastrar y soltar.
3. **Direccion completa:** codigo postal, estado, municipio/alcaldia, colonia y calle.

El formulario solo se muestra cuando `canRegisterCustomers` encuentra el permiso `CLIENTS_WRITE` en la sesion. La respuesta exitosa muestra `Nuevo cliente creado: <nombre>`; los mensajes del backend se presentan mediante el estado `error`.

### 2.3 Composition API y estado

| Elemento | Funcion |
| --- | --- |
| `token`, `user`, `mode`, `loading`, `error`, `success` | Estado de sesion, vista de autenticacion y retroalimentacion de operaciones. El JWT se guarda en `localStorage` bajo `auth_token`. |
| `loginForm`, `registerForm`, `forgotForm`, `resetForm` | Objetos reactivos de los flujos de autenticacion. |
| `customerForm` | Datos de cliente y archivo `photo` enviados como `FormData`. |
| `api(path, options)` | Cliente REST interno del backend. Agrega `Content-Type: application/json` salvo multipart y agrega `Authorization: Bearer` cuando hay sesion. |
| `run(action)` | Normaliza el manejo de carga, limpieza de mensajes y captura de excepciones de interfaz. |
| `selectCustomerPhoto` / `clearCustomerPhoto` | Obtienen la foto desde selector o drag and drop, generan/revocan `ObjectURL` para previsualizacion y limpian el control. |
| `submitCustomer` | Construye `FormData`, llama `POST /api/customers`, limpia el formulario al recibir respuesta y anuncia la alta. |
| `watch(() => customerForm.postalCode, applyZipCode)` | Ejecuta la consulta de SEPOMEX solo al llegar a un CP de cinco digitos. |

### 2.4 Validaciones de interfaz

El HTML restringe campos con `required`, `type=email`, `type=date`, `type=tel`, `min/max`, `maxlength`, `inputmode` y patrones para nombre, municipio, estado y CP. Estas validaciones son de experiencia de usuario; la validacion autoritativa permanece en Spring Boot.

La foto acepta visualmente JPEG, PNG y WEBP. La interfaz informa el limite de 15 MB; el servidor vuelve a validar tamano y firma binaria.

### 2.5 Autocompletado SEPOMEX

`frontend/src/services/sepomex.js` usa la URL definida por `VITE_SEPOMEX_API_URL`; si no existe usa `http://localhost:3005`. Si esa instancia no responde, intenta `https://sepomex.kurenn.dev`.

| Funcion | Peticion | Resultado usado por la vista |
| --- | --- | --- |
| `findZipCode(zipCode)` | `GET /api/v1/zip_codes?zip_code={CP}&per_page=200` | Completa `state` desde `d_estado`, `municipality` desde `d_mnpio` y llena las colonias con `d_asenta`. |
| `findStates()` | `GET /api/v1/states?per_page=200` | Llena el selector de estados. |
| `findMunicipalities(stateId)` | `GET /api/v1/states/{id}/municipalities` | Llena el selector dependiente de municipios. |
| `findNeighborhoods(state, municipality)` | `GET /api/v1/zip_codes?state={estado}&city={municipio}&per_page=200` | Llena el selector de colonias cuando la ruta se elige manualmente. |

La aplicacion permite captura manual de estado, municipio y colonia si el catalogo no puede cargarse. SEPOMEX es una consulta desde Vue; **no existe actualmente un cliente SEPOMEX dentro del backend Spring Boot**.

## 3. Backend Spring Boot

### 3.1 Capas

| Capa | Componentes | Responsabilidad |
| --- | --- | --- |
| Controllers | `AuthController`, `CustomerController`, `RoleController` | Adaptadores HTTP: reciben DTOs/model attributes y entregan peticiones a servicios o fachadas. |
| Facades y servicios | `AuthFacade`, `CustomerFacade`, `AuthService`, `JwtService`, `CustomerPhotoStorageService`, `LoginAttemptService` | Orquestacion de casos de uso, autenticacion, seguridad, reglas de duplicidad y archivos. |
| Repositories y DTOs | `UserRepository`, `RoleRepository`, `CustomerRepository`, `PasswordResetTokenRepository`; DTOs de auth y cliente | Acceso JPA parametrizado y contratos de entrada/salida. |
| Modelos | `AppUser`, `Role`, `Customer`, `PasswordResetToken` | Entidades persistidas y relaciones de seguridad. |

### 3.2 Endpoints

| Metodo y ruta | Controlador | Autorizacion | Entrada y comportamiento |
| --- | --- | --- |
| `POST /api/auth/register` | `AuthController` | Publico | Registro de cuenta cliente y respuesta de autenticacion. |
| `POST /api/auth/login` | `AuthController` | Publico | Correo y contrasena; devuelve JWT y perfil si las credenciales son validas. |
| `GET /api/auth/me` | `AuthController` | JWT | Devuelve el perfil de la sesion. |
| `POST /api/auth/forgot-password` | `AuthController` | Publico | Solicita token temporal de recuperacion. |
| `POST /api/auth/reset-password` | `AuthController` | Publico | Restablece la contrasena con token valido. |
| `POST /api/customers` | `CustomerController` | JWT + `PERM_CLIENTS_WRITE` | Recibe `multipart/form-data` con `CustomerRegistrationRequest`; responde `201 Created`. |
| `GET /api/roles` y `POST /api/roles` | `RoleController` | JWT + `PERM_SECURITY_MANAGE_ROLES` | Consulta y alta de roles/permisos. |

`CustomerController` usa `@ModelAttribute` y `MultipartFile`, no JSON, para transportar la fotografia junto con los demas campos. No hay un `ClienteController` con ese nombre: el nombre real de la clase es `CustomerController` y conserva la ruta REST en plural `/api/customers`.

### 3.3 Alta de cliente

1. Spring Security valida el JWT y la autoridad `PERM_CLIENTS_WRITE` antes del controlador.
2. `CustomerController` valida el DTO y delega en `CustomerFacade.register` con el correo del `Principal` autenticado.
3. `CustomerFacade` comprueba que la edad corresponda a la fecha de nacimiento, normaliza nombre/correos y busca duplicados.
4. `CustomerPhotoStorageService` revisa que el archivo exista, no supere 15 MB y tenga cabecera JPEG, PNG o WEBP valida. Guarda un UUID en `CUSTOMER_PHOTOS_DIR` o `uploads/customer-photos`.
5. Se obtiene el usuario creador desde `UserRepository`, se persiste `Customer` mediante `CustomerRepository.saveAndFlush` y se devuelve `CustomerResponse` sin exponer la ruta fisica de la imagen.
6. Si una restriccion de base de datos falla tras guardar la foto, la fachada intenta eliminar el archivo y devuelve conflicto de duplicidad.

`CustomerRepository` utiliza metodos derivados de Spring Data JPA para correo, telefonos y nombre normalizado + fecha de nacimiento. No construye SQL a partir de texto del usuario.

### 3.4 DTO de cliente

`CustomerRegistrationRequest` exige nombre completo, nombre alternativo, telefonos, edad, fecha pasada, correo principal, fotografia, calle, colonia, municipio, estado y CP. `workEmail` es opcional. Las anotaciones Bean Validation controlan longitudes, formatos, caracteres y CP de cinco digitos.

### 3.5 Manejo de errores

`ApiExceptionHandler` transforma:

| Condicion | HTTP | Respuesta |
| --- | --- | --- |
| Bean Validation o regla de negocio | `400` | `MessageResponse` con detalle controlado. |
| Correo, telefono o identidad duplicada | `409` | `MessageResponse` de `DuplicateCustomerException`. |
| Credenciales invalidas | `401` | Mensaje de autenticacion. |
| Error no controlado | `500` | Mensaje generico sin exponer internals. |

### 3.6 Seguridad

`SecurityConfig` define sesiones stateless, BCrypt con fuerza 12, filtro JWT antes del filtro de usuario/contrasena, CORS y autorizacion por autoridades. La restriccion de clientes se declara en `SecurityFilterChain` con:

```java
.requestMatchers(HttpMethod.POST, "/api/customers").hasAuthority("PERM_CLIENTS_WRITE")
```

El modulo base protege rutas mediante `SecurityFilterChain`; UC-CV-02 agrega anotaciones `@PreAuthorize` para separar lectura, edicion y suspension. `DataInitializer` concede `CLIENTS_WRITE` a los roles base `ADMINISTRADOR` y `RECEPCIONISTA`.

## 4. Infraestructura

`docker-compose.yml` define:

| Servicio | Uso | Puerto anfitrion |
| --- | --- | --- |
| `mysql` | Persistencia de usuarios, roles, clientes y tokens. | `3306` |
| `sepomex-api` | Catalogo local de direcciones de solo lectura. | `3005` |

La imagen actual `icalia/sepomex:latest` escucha internamente en `80`; por ello el mapeo funcional es `3005:80`. La URL expuesta al navegador continua siendo `http://localhost:3005`. Si se agrega un cliente backend en una red Docker futura, debera apuntar al puerto que exponga efectivamente el contenedor, no al puerto publicado del anfitrion.

## 5. Verificacion ejecutada

- `yarn build` en `frontend/`: compilacion de Vite completada.
- `docker compose config --quiet`: sintaxis de infraestructura valida.
- `GET http://localhost:3005/up`: servicio SEPOMEX disponible.
- Consulta de `GET /api/v1/states` y CP `64000`: datos recibidos desde el contenedor.
- Prueba visual: el CP `64000` autocompleto Nuevo Leon, Monterrey y la lista de colonias.

## 6. Limites actuales

- La pertenencia de un cliente a varios talleres de una misma empresa queda pendiente de las entidades de empresa, taller y tabla de enlace.
- La aplicacion no incluye pruebas automatizadas para el modulo de cliente o el servicio Sepomex.
- El diagrama se entrega en HTML independiente. La CLI de Archify no se encontro localmente y `npx archify --help` no obtuvo un ejecutable publicable.

## 7. Extension multitaller y UC-CV-02

### 7.1 Talleres

El modulo se compone de `Taller`, `TallerRepository`, `TallerFacade`, `TallerPhotoStorageService`,
`TallerController` y sus DTOs. Hibernate crea o actualiza la tabla `talleres` a partir de la entidad. La entidad
guarda identidad fiscal, datos de contacto, direccion, fotografia, bandera `activo` y marca de creacion.

`POST /api/v1/talleres` recibe `multipart/form-data`. La foto es obligatoria, tiene un limite efectivo de 15 MB y
se verifica contra firmas binarias JPEG, PNG o WEBP antes de persistirse. RFC y razon social se verifican antes de
guardar y tambien tienen restricciones unicas en MySQL. El RFC usa la expresion solicitada:

```text
^([A-ZÑ&]{3,4})([0-9]{2})(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])([A-Z0-9]{3})$
```

`GET /api/v1/talleres` devuelve exclusivamente talleres activos, ordenados por nombre, para los selectores de
clientes. El alta de talleres se protege con `@PreAuthorize("hasRole('ADMIN')")`.

### 7.2 Cliente ampliado

`Customer` ahora contiene RFC opcional, CURP obligatoria validada, estado (`ACTIVO` o `SUSPENDIDO`) y una relacion
`@ManyToOne` obligatoria con `Taller` mediante `taller_id`. `CustomerRegistrationRequest` recibe los mismos
campos; `CustomerUpdateRequest` es el contrato JSON para edicion, sin reemplazar la fotografia ya almacenada.

La CURP se valida con una expresion que comprueba estructura, fecha, sexo, entidad federativa y homoclave. El
frontend replica RFC/CURP y limite de foto como retroalimentacion temprana, pero Spring Boot conserva la validacion
autoritativa.

### 7.3 Endpoints UC-CV-02

| Metodo y ruta | Regla | Implementacion |
| --- | --- | --- |
| `GET /api/v1/clientes?tallerId=&page=0&size=10&sortBy=nombre&sortDir=asc` | Lectura con `CLIENTS_READ` o `CLIENTS_WRITE`. | `ClienteController` delega a `CustomerFacade.list`; `PageRequest` limita tamano a 10 y genera el `ORDER BY fullName` y `LIMIT/OFFSET` en MySQL. |
| `PUT /api/v1/clientes/{id}` | Requiere `PERM_CLIENTS_WRITE`. | Actualiza datos de cliente y permite reasignar solo a un taller activo. |
| `PUT /api/v1/clientes/{id}/suspender` | Exclusivo `@PreAuthorize("hasRole('ADMIN')")`. | Ruta de compatibilidad que cambia el estado a `SUSPENDIDO`; no elimina el historial. |
| `PUT /api/v1/clientes/{id}/estatus?statusId={id}` | Exclusivo `@PreAuthorize("hasRole('ADMIN')")`. | Actualiza `Customer.estatus` con un registro activo del catalogo y permite tanto suspender como reactivar mediante la seleccion de Activo. |

El rol legado `ADMINISTRADOR` recibe tambien la autoridad estandar `ROLE_ADMIN` al autenticarse. Esto conserva los
roles existentes y permite que la restriccion solicitada use exactamente `hasRole('ADMIN')`.

### 7.4 Frontend

`App.vue` sigue siendo el componente raiz existente. Su barra lateral ahora ofrece **Administracion: Talleres** y
**Administracion: Clientes (UC-CV-02)**. El formulario de talleres aplica validacion de RFC, correo y foto antes de
enviar `FormData`. El formulario de clientes agrega RFC, CURP y selector de taller; al elegir **Editar**, reutiliza
la misma pantalla para ejecutar `PUT` y permite reasignar el taller.

La tabla consume paginacion del servidor, filtra por taller, alterna A-Z/Z-A y muestra las acciones autorizadas por
perfil. Para administradores, **Estado** abre un modal con los estatus activos del catalogo; al confirmar, llama al
endpoint de cambio de estado. Asi, Activo permite reactivar un cliente suspendido y Suspendido conserva la suspension
logica sin eliminar registros. El diseno conserva el fondo crema `#FAF8F5`, tarjetas blancas y acentos warm sunset.

### 7.5 Verificacion adicional

- `./mvnw test -q`: compilacion y pruebas de Maven completadas.
- `yarn build`: compilacion de Vue/Vite completada.
- La actualizacion del esquema se realiza con `spring.jpa.hibernate.ddl-auto=update` al arrancar Spring Boot.

## 8. UC-CV-06: vehiculos y catalogos

`Estatus` es un catalogo configurable con valor unico, descripcion y bandera `activo`. Se relaciona mediante
`status_id` con `Customer` y `Vehiculo`; las transiciones Suspendido/Activo actualizan la referencia sin borrar
datos. `EstatusController` expone CRUD administrativo en `/api/v1/estatus` y requiere `ROLE_ADMIN`.

Al iniciar, `DataInitializer` lee `backend/src/main/resources/data/vehiculos_normalizado.csv` cuando `modelo` esta
vacio. Crea `Marca` por nombre unico y `Modelo` unico por marca; conserva combustible, cilindros y transmision como
sugerencias tecnicas para el formulario. Las rutas de consulta son `GET /api/v1/vehiculos/marcas` y
`GET /api/v1/vehiculos/marcas/{marcaId}/modelos`.

`Vehiculo` almacena VIN unico, placa normalizada a mayusculas, marca, modelo, anio, ficha tecnica, cliente y estatus.
`POST /api/v1/vehiculos` permite `ROLE_ADMIN` y `ROLE_RECEPCIONISTA`; rechaza VIN duplicado, VIN fuera de
`^[A-HJ-NPR-Z0-9]{17}$`, placas fuera de `^[A-Z0-9]{3,4}-?[A-Z0-9]{2,4}$` y clientes cuyo estatus no sea Activo.
La peticion puede incluir `nuevaMarca` y `nuevoModelo` cuando el catalogo no contenga la opcion requerida.

En UC-CV-02, recepcionista y administrador pueden editar los datos personales. La reasignacion de `tallerId` se
verifica en backend con las autoridades del JWT y el selector se deshabilita visualmente para recepcionista.

### 8.1 Cambio de estado de cliente

`EstatusFacade#listActive` y `EstatusFacade#activeById` exponen solamente opciones habilitadas del catalogo.
`CustomerFacade#updateStatus` asigna el `Estatus` seleccionado a `Customer.estatus` y conserva sincronizado el
campo legado `CustomerStatus` para las opciones Activo y Suspendido. La vista administra este flujo con
`openCustomerStatusModal` y `updateCustomerStatus` en `App.vue`; el modal no ofrece estatus inactivos y se cierra
solo despues de una actualizacion satisfactoria.
