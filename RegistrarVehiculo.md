# UC-CV-06: Registro y Administracion de Vehiculos

| Actividad | Estado | Descripcion tecnica | Evidencia |
| --- | --- | --- | --- |
| Catalogo Marca/Modelo | [Realizado] | Entidades normalizadas con modelo asociado a marca y metadatos tecnicos. | `Marca.java`, `Modelo.java` |
| Carga CSV | [Realizado] | Carga unica desde `resources/data/vehiculos_normalizado.csv` cuando modelo esta vacio. | `DataInitializer#loadVehicleCatalog` |
| Catalogo de estatus | [Realizado] | CRUD admin, valores semilla y relacion por `status_id`. | `EstatusController`, `EstatusFacade` |
| Estatus de cliente | [Realizado] | Soft-delete y suspension actualizan el estatus relacionado sin borrado fisico. | `Customer`, `CustomerFacade` |
| Registro de vehiculo | [Realizado] | Persiste VIN, placa, ficha tecnica, cliente y estatus Activo. | `Vehiculo.java`, `VehiculoFacade` |
| Validacion VIN y placa | [Realizado] | Bean Validation y normalizacion a mayusculas antes de persistir. | `VehiculoDtos.Request` |
| Cliente activo requerido | [Realizado] | La fachada valida estatus Activo antes de permitir el registro. | `VehiculoFacade#create` |
| Autocomplete Marca/Modelo | [Realizado] | Endpoints de catalogo y cascada en modal Vue con sugerencias tecnicas. | `VehiculoController`, `App.vue` |
| Alta manual Marca/Modelo | [Realizado] | Acepta nueva marca/modelo cuando no existe en catalogo. | `VehiculoFacade#resolveBrand`, `resolveModel` |
| Consulta de ficha por identificador | [Realizado] | Consulta un vehiculo usando exactamente uno de los identificadores: ID, VIN o placa. | `GET /api/v1/vehiculos/ficha?id={id}`, `?vin={vin}` o `?placa={placa}` |
| Alcance taller-cliente | [Realizado] | La ficha incluye el cliente propietario y el taller asignado al cliente mediante la relacion existente `Vehiculo -> Customer -> Taller`. | `VehiculoDtos.DetailResponse`, `VehiculoFacade#detail` |
| Control de ambiguedad de placa | [Realizado] | Si una placa tiene mas de una coincidencia, la API rechaza la consulta y solicita VIN o ID. | `VehiculoRepository#findAllByPlaca` |
| Autorizacion de consulta | [Realizado] | Solo perfiles con `PERM_CLIENTS_READ` o `PERM_CLIENTS_WRITE` pueden consultar la ficha. | `VehiculoController#detail` |
| Pruebas unitarias UC-CV-07 | [Realizado] | Se validan consulta por VIN, alcance cliente-taller, identificador exclusivo, placa ambigua y anotacion de permisos. | `src/test/java/com/refaccionaria/auth/.../Vehiculo*Test.java` |
| UC-CV-08 Actualizar Vehiculo | [Realizado] | La ficha permite abrir un modal de edicion; conserva VIN y cliente, valida los datos del vehiculo y guarda los cambios autorizados. | `PUT /api/v1/vehiculos/{id}`, `VehiculoFacade#update`, `App.vue` |
| UC-CV-09 Desactivar Vehiculo | [Realizado] | El modal Estado exige seleccionar un estatus activo y confirmar la operacion antes de guardar. | `PUT /api/v1/vehiculos/{id}/estatus`, `VehiculoFacade#updateStatus`, `App.vue` |
| Auditoria de vehiculo | [Realizado] | Cada actualizacion o cambio de estado guarda actor, fecha y snapshots JSON antes/despues en una tabla persistente. | `VehiculoAuditoria`, `VehiculoAuditoriaService`, `vehiculo_auditoria` |
| VIN inmutable | [Realizado] | El DTO de actualizacion no acepta VIN y el modal solo lo muestra como referencia de solo lectura. | `VehiculoDtos.UpdateRequest`, modal Editar |
| Campos obligatorios | [Realizado] | Alta y edicion rechazan campos requeridos nulos o vacios en navegador y mediante Bean Validation en la API. | `VehiculoDtos.Request`, `VehiculoDtos.UpdateRequest` |
| Pruebas HTTP automatizadas | [Pendiente] | Faltan pruebas de integracion autenticadas para VIN duplicado y permisos. | Siguiente iteracion |
