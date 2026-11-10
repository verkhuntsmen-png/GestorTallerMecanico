# UC-CV-02: Administracion de las Clientes

| Actividad | Estado | Descripcion tecnica | Evidencia de cumplimiento |
| --- | --- | --- | --- |
| Entidad multitaller | [Realizado] | Se creo `Taller` con identidad fiscal, contacto, direccion, foto, activo y fecha de creacion. | `backend/src/main/java/com/refaccionaria/auth/model/Taller.java` |
| Alta y listado de talleres | [Realizado] | `TallerFacade` controla RFC/razon social duplicados, guarda foto validada y lista solo talleres activos. | `TallerController`, rutas `/api/v1/talleres` |
| Foto de taller maximo 15 MB | [Realizado] | Se valida presencia, peso y firma JPEG/PNG/WEBP en frontend y backend. | `TallerPhotoStorageService` y `selectTallerPhoto` |
| Campos fiscales del cliente | [Realizado] | Cliente admite RFC opcional y CURP obligatoria con validaciones Bean Validation. | `Customer`, `CustomerRegistrationRequest`, `CustomerUpdateRequest` |
| Asociacion cliente-taller | [Realizado] | Relacion `@ManyToOne` por `taller_id`; el alta y la edicion exigen taller activo. | `Customer#setTaller`, `CustomerFacade#findTaller` |
| Consulta paginada | [Realizado] | Endpoint con `Pageable`, limite maximo 10 y orden SQL por `fullName`. | `GET /api/v1/clientes`, `CustomerFacade#list` |
| Filtro por taller | [Realizado] | El listado acepta `tallerId` y usa `CustomerRepository.findByTallerId`. | `ClienteController`, `CustomerRepository` |
| Ordenamiento por nombre | [Realizado] | Solo se acepta `nombre`/`fullName` y direccion `asc`/`desc`. | `CustomerFacade#list`, selector A-Z/Z-A de Vue |
| Edicion y reasignacion | [Realizado] | `PUT /api/v1/clientes/{id}` actualiza datos y taller desde el mismo formulario. | `CustomerUpdateRequest`, `editCustomer`, `submitCustomer` |
| Gestion de estado mediante catalogo | [Realizado] | La accion **Estado** abre un modal y actualiza `status_id` sin borrado fisico. Permite seleccionar los estatus activos del catalogo, incluidos Activo y Suspendido. | `PUT /api/v1/clientes/{id}/estatus?statusId={id}`, `CustomerFacade#updateStatus`, `frontend/src/App.vue` |
| Reactivacion desde el mismo flujo | [Realizado] | Para reactivar se selecciona **Activo** en el modal Estado; se sustituyeron los botones fijos de suspender/reactivar por una unica accion controlada. | Modal `customerStatusModal`, `EstatusFacade#activeById` |
| Seguridad de cambio de estado | [Realizado] | El cambio de estado usa exclusivamente `@PreAuthorize("hasRole('ADMIN')")`; el rol ADMINISTRADOR recibe el alias requerido. | `ClienteController`, `UserDetailsServiceImpl` |
| Prevencion de duplicados | [Realizado] | Se conservan verificaciones por identidad, correo y telefonos para alta y edicion. | `CustomerFacade` y restricciones unicas de `customers` |
| Sidebar de administracion | [Realizado] | Navegacion de sesion incorpora Talleres y Clientes (UC-CV-02). | `frontend/src/App.vue` |
| Diseno responsivo neutral | [Realizado] | Formularios/tablas mobile-first con fondo crema, tarjetas blancas y acentos warm sunset. | `frontend/src/App.vue`, `frontend/src/style.css` |
| Migracion automatica MySQL | [Realizado] | Hibernate actualiza tablas y columnas al iniciar con `ddl-auto=update`. | `backend/src/main/resources/application.properties` |
| Pruebas de integracion HTTP autenticadas | [Pendiente] | Faltan pruebas automatizadas que creen taller/cliente y cubran permisos por rol. | Recomendado para la siguiente iteracion. |
