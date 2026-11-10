# Laboratorio local de seguridad

Este directorio prepara pruebas autorizadas contra el perfil `security-lab`.
No ejecute Wapiti ni Tsung contra produccion, informacion real o cuentas reales.

## Servicios aislados

- API de pruebas: `http://localhost:8081/api`
- MySQL de pruebas: esquema `refaccionaria_pruebas` en el contenedor local.
- Base normal: `refaccionaria`; no se usa por el perfil de laboratorio.
- UI de pruebas: `yarn dev:security-lab` desde `frontend/`; queda en `http://localhost:5175`.
- OpenAPI del laboratorio: `http://localhost:8081/v3/api-docs` y Swagger UI: `http://localhost:8081/swagger-ui.html`.

## Credenciales y token

Las cuentas ficticias se registran durante la preparacion del laboratorio. Inicie
sesion contra el puerto `8081` y copie el JWT de la respuesta solo a una variable
temporal de terminal. No guarde tokens ni contrasenas en XML, Markdown o Git.

## Tsung

Antes de ejecutar `tsung -f tests/security/tsung-local.xml start`, reemplace el
marcador `REEMPLAZAR_CON_JWT_DE_PRUEBAS` por un JWT recien emitido. La plantilla
inicia con una solicitud por segundo durante 30 segundos y solo consulta una
ficha ficticia. Confirme que el ID del vehiculo coincide con el dato preparado.

Para una demostracion de flujo completo use
`tests/security/tsung-flujo-autenticado.xml`. Simula una sesion de lectura con
perfil autenticado, catalogo de marcas, listado paginado de clientes y ficha de
vehiculo. La carga aumenta de una a tres sesiones nuevas por segundo durante
150 segundos. No contiene solicitudes de creacion, actualizacion o eliminacion.
Genere una copia temporal e inserte el JWT sin guardarlo en el repositorio:

```bash
sed "s|REEMPLAZAR_CON_JWT_DE_PRUEBAS|$TOKEN|g" \
  tests/security/tsung-flujo-autenticado.xml > /tmp/tsung-flujo-autenticado.xml
tsung -f /tmp/tsung-flujo-autenticado.xml start
```

## Wapiti

Use siempre el puerto `8081`, alcance `folder`, JWT temporal, una intensidad
`polite` y reportes fuera del control de versiones. Evite modulos de fuerza
bruta y rutas de escritura hasta tener una copia restaurable de la base.

El perfil normal mantiene OpenAPI y Swagger UI deshabilitados. La interfaz de
Swagger del laboratorio no permite ejecutar solicitudes desde el navegador;
use el contrato para revisar rutas o como entrada controlada de Wapiti.
