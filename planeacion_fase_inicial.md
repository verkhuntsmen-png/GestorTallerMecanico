# Planeación - Fase Inicial

## 1. Alcance y estado de la fase

Esta primera fase establece la base de identidad, acceso y despliegue local del **Gestor de Taller Mecánico / Refaccionaria**. La solución está desacoplada en un backend REST con Spring Boot, una SPA en Vue y una instancia de MySQL mediante Docker Compose.

| Estado | Módulo o componente | Descripción técnica |
| --- | --- | --- |
| Estructurado/terminado para configuración | Proyecto backend Maven/Spring Boot | `pom.xml` define Java 21, Spring Web, Spring Security, Spring Data JPA, validación, controlador MySQL y pruebas. `mvnw` permite compilar sin una instalación global de Maven. |
| Estructurado/terminado para configuración | Persistencia MySQL | `docker-compose.yml` crea el servicio `mysql`, la base `refaccionaria`, volumen persistente y una comprobación de salud. Las entidades JPA generan/actualizan las tablas de usuarios, roles, permisos y recuperación. |
| Estructurado/terminado para configuración | Autenticación | Registro, inicio de sesión, consulta de sesión y restablecimiento de contraseña están expuestos como API REST bajo `/api/auth`. La sesión es stateless y se representa con JWT firmado. |
| Estructurado/terminado para configuración | Gestión de contraseñas | Las contraseñas no se guardan en texto plano: Spring Security las procesa con BCrypt, factor de trabajo 12 y salt aleatorio integrado. La política exige 10-128 caracteres, mayúscula, minúscula, número y símbolo. |
| Estructurado/terminado para configuración | Autorización RBAC | Un usuario puede tener varios roles y un rol varias capacidades. Las capacidades se materializan como autoridades `PERM_<CAPACIDAD>` y protegen las rutas administrativas de roles. |
| Estructurado/terminado para configuración | Administración de roles | La API permite listar, crear y actualizar roles con nombre, descripción y permisos. Se inicializan ADMINISTRADOR, CLIENTE, MECANICO y GERENTE. |
| Estructurado/terminado para configuración | Protección de acceso | Se validan entradas, se usan repositorios JPA parametrizados, JWT HMAC-SHA256, CORS restringible y un bloqueo temporal de inicio de sesión tras cinco fallos por correo/IP. |
| Estructurado/terminado para configuración | Frontend Vue/Vite/Tailwind | La SPA presenta login, registro, recuperación/restablecimiento, datos de la sesión y gestión de roles para un administrador. Vite redirige `/api` al backend durante desarrollo. |
| Pendiente antes de producción | Configuración segura y operación | Los valores por defecto, la cuenta semilla, la devolución del token de recuperación, la memoria local de intentos, `ddl-auto=update` y la etiqueta `mysql:latest` son apropiados para desarrollo, pero deben sustituirse o endurecerse antes de publicar. |

## 2. Arquitectura y flujo

```text
Navegador (Vue 3 + Tailwind)
        |  HTTPS / JSON / Authorization: Bearer <JWT>
        v
API REST Spring Boot 3
  ├─ Filtro JWT y Spring Security
  ├─ Servicios de autenticación, roles y recuperación
  ├─ JPA/Hibernate
  v
MySQL 8 (contenedor Docker y volumen mysql_data)
```

El registro asigna únicamente el rol `CLIENTE`. El administrador crea o modifica roles y capacidades mediante `/api/roles`; el filtro JWT carga las autoridades del usuario en cada solicitud protegida. El control de autorización no depende de que la pantalla oculte botones: Spring Security exige `PERM_SECURITY_MANAGE_ROLES` directamente en el servidor.

## 3. Documentación del código generado

### 3.1 Raíz e infraestructura

| Archivo | Responsabilidad |
| --- | --- |
| `pom.xml` | Descriptor Maven. Declara Spring Boot 3.5.16, Java 21 y dependencias web, seguridad, JPA, validación, MySQL y pruebas; también indica los directorios no convencionales `backend/src/...`. |
| `mvnw` | Wrapper de Maven. Descarga y reutiliza Maven 3.9.11 cuando no está instalado en el equipo. |
| `docker-compose.yml` | Levanta MySQL, expone `3306:3306`, mantiene datos en `mysql_data` y espera la respuesta de `mysqladmin ping` antes de considerar sano el servicio. |
| `backend/src/main/resources/application.properties` | Configuración de aplicación: conexión JDBC, comportamiento JPA, secreto y vencimiento del JWT, y origen CORS. Recibe valores mediante variables de entorno con valores por defecto locales. |

### 3.2 Backend: modelo y persistencia

| Archivo | Responsabilidad |
| --- | --- |
| `RefaccionariaApplication.java` | Punto de arranque de Spring Boot. |
| `model/AppUser.java` | Entidad `users`: nombre completo, correo único, hash BCrypt, estado, fecha de creación y relación muchos-a-muchos con roles. |
| `model/Role.java` | Entidad `roles`: nombre único, descripción, fecha de creación y colección de permisos en `role_permissions`. |
| `model/PasswordResetToken.java` | Entidad `password_reset_tokens`: hash del token, usuario asociado, vencimiento y marca de uso. Nunca persiste el token de texto plano. |
| `repository/UserRepository.java` | Acceso JPA para buscar o detectar usuarios por correo sin distinguir mayúsculas/minúsculas. |
| `repository/RoleRepository.java` | Acceso JPA para buscar o detectar roles por nombre sin distinguir mayúsculas/minúsculas. |
| `repository/PasswordResetTokenRepository.java` | Recupera un token mediante su hash SHA-256. |

Las consultas derivadas de Spring Data JPA son parametrizadas; por tanto, el correo y otros valores no se concatenan en sentencias SQL. Esta es la principal medida frente a inyección SQL en la capa actual.

### 3.3 Backend: contratos y servicios

| Archivo | Responsabilidad |
| --- | --- |
| `dto/AuthDtos.java` | Records de entrada/salida para registro, login, recuperación, restablecimiento, roles, usuario y mensajes. Aplica Bean Validation a longitudes, correo, obligatoriedad, contraseñas y permisos. |
| `service/AuthService.java` | Orquesta registro, login, perfil actual, recuperación y cambio de contraseña. Normaliza correo, asigna `CLIENTE`, invoca BCrypt, emite JWT, registra fallos y hace de un solo uso el token de recuperación. |
| `service/AuthMapper.java` | Convierte entidades de usuario y rol a respuestas sin exponer hashes ni campos internos. |
| `service/RoleService.java` | Lista, crea y actualiza roles. Normaliza nombres y permisos a mayúsculas con guion bajo y elimina permisos repetidos. |
| `service/TokenHashService.java` | Genera tokens de recuperación de 32 bytes con `SecureRandom`, codificados en Base64 URL, y calcula SHA-256 para almacenamiento. |
| `service/LoginAttemptService.java` | Almacena en memoria el contador por combinación correo/IP. A partir de cinco fallos bloquea por 15 minutos; un login correcto borra el contador. |
| `service/UserDetailsServiceImpl.java` | Convierte usuario, roles y permisos de base de datos a `UserDetails` y autoridades de Spring Security (`ROLE_...` y `PERM_...`). |

### 3.4 Backend: seguridad, controladores y errores

| Archivo | Responsabilidad |
| --- | --- |
| `security/JwtService.java` | Crea y valida JWT HMAC-SHA256 con sujeto, emisión y expiración. Comprueba la firma en tiempo constante antes de entregar el correo autenticado. |
| `security/JwtAuthenticationFilter.java` | Lee `Authorization: Bearer <token>`, valida el JWT y coloca la autenticación en el contexto de seguridad. |
| `config/SecurityConfig.java` | Declara BCrypt con coste 12, autenticación DAO, API sin sesión, CORS, filtro JWT y reglas de rutas. Registro/login/recuperación son públicas; `/api/roles/**` exige `PERM_SECURITY_MANAGE_ROLES`; el resto exige autenticación. |
| `config/UserDetailsServiceImplMarker.java` | Adaptador que entrega el servicio de detalles de usuario al proveedor de autenticación configurado. |
| `config/DataInitializer.java` | Inserta de forma idempotente los cuatro roles base y un administrador local de demostración al arrancar. |
| `controller/AuthController.java` | Expone el contrato REST de autenticación y delega en `AuthService`. Las entradas se validan con `@Valid`. |
| `controller/RoleController.java` | Expone listado, alta y actualización de roles. La autorización se aplica de forma centralizada en `SecurityConfig`. |
| `controller/ApiExceptionHandler.java` | Convierte errores de validación, credenciales, argumentos y errores inesperados en respuestas JSON controladas. |

### 3.5 API REST disponible

| Método y ruta | Autenticación | Función |
| --- | --- | --- |
| `POST /api/auth/register` | Pública | Crea un usuario cliente y devuelve JWT + perfil. |
| `POST /api/auth/login` | Pública | Valida credenciales y devuelve JWT + perfil; aplica limitación de intentos. |
| `GET /api/auth/me` | Bearer JWT | Devuelve el perfil del usuario autenticado. |
| `POST /api/auth/forgot-password` | Pública | Genera un token temporal de recuperación para una cuenta existente sin revelar si el correo no existe. |
| `POST /api/auth/reset-password` | Pública con token | Verifica token no usado y no vencido, cambia el hash de contraseña y marca el token como usado. |
| `GET /api/roles` | `PERM_SECURITY_MANAGE_ROLES` | Lista roles y capacidades. |
| `POST /api/roles` | `PERM_SECURITY_MANAGE_ROLES` | Crea un rol con capacidades. |
| `PUT /api/roles/{id}` | `PERM_SECURITY_MANAGE_ROLES` | Actualiza nombre, descripción y capacidades de un rol. |

### 3.6 Frontend

| Archivo | Responsabilidad |
| --- | --- |
| `frontend/package.json` | Define Vue 3, Vite, plugin Vue, integración Tailwind y comandos `yarn dev`, `yarn build` y `yarn preview`. |
| `frontend/vite.config.js` | Configura Vue/Tailwind, puerto 5173 y proxy local de `/api` a `http://localhost:8080`. |
| `frontend/index.html` | Documento HTML de entrada de Vite. |
| `frontend/src/main.js` | Monta el componente raíz `App.vue`. |
| `frontend/src/App.vue` | Implementa los formularios y estados de login, registro, recuperación, restablecimiento, sesión y administración de roles; conserva el JWT en `localStorage` y lo adjunta al encabezado Bearer. |
| `frontend/src/style.css` | Carga Tailwind y estilos globales de la interfaz con la paleta verde, naranja y gris solicitada. |
| `frontend/yarn.lock` | Fija las versiones transitivas utilizadas por Yarn para builds repetibles. |

## 4. Controles de seguridad y brechas pendientes

| Riesgo OWASP / práctica | Implementado en la fase | Configuración o mejora pendiente |
| --- | --- | --- |
| A03: Inyección | JPA/repositorios derivados parametrizan las consultas; DTOs limitan tamaño y tipo de entrada. | Mantener este patrón si se agregan consultas nativas; nunca concatenar entrada de usuario en SQL. |
| A07: Fallos de autenticación | BCrypt 12, política de contraseña, JWT con expiración, respuesta genérica a credenciales erróneas y bloqueo tras cinco fallos. | Mover el contador a Redis o a una base compartida; agregar rate limiting en proxy/WAF, auditoría y MFA para cuentas privilegiadas. |
| A01: Control de acceso roto | Autorización del servidor basada en permisos y rutas de roles protegidas. | Añadir pruebas de autorización por cada capacidad y permisos de recursos propios para órdenes. |
| A02: Fallos criptográficos | BCrypt con salt por hash, tokens de recuperación aleatorios y almacenados como SHA-256, firma JWT HMAC-SHA256. | Inyectar secretos reales desde un gestor de secretos, rotarlos y obligar HTTPS/TLS. |
| A05: Configuración insegura | CORS permite un origen configurable y las sesiones son stateless. | Cambiar valores por defecto, limitar CORS al dominio final, fijar versión de MySQL, usar perfil `prod`, desactivar `ddl-auto=update` y configurar cabeceras de seguridad (CSP, `X-Content-Type-Options`, `X-Frame-Options`). |
| A09: Registro y monitoreo insuficiente | Se devuelve un error genérico al fallar credenciales. | Añadir logs estructurados de eventos de seguridad sin contraseñas/tokens, alertas y retención definida. |

Advertencias explícitas para producción:

- `DataInitializer` crea `admin@taller.local` con una contraseña conocida de demostración. Debe eliminarse o sustituirse por un proceso de aprovisionamiento seguro antes de publicar.
- `POST /api/auth/forgot-password` devuelve el token sin cifrar exclusivamente para probar el flujo local. En producción debe enviar una URL de un solo uso por correo y responder siempre con un mensaje genérico.
- El frontend almacena el JWT en `localStorage`, por lo que una vulnerabilidad XSS podría exponerlo. Valorar cookie `HttpOnly`, `Secure`, `SameSite` y protección CSRF si se adopta autenticación basada en cookies.
- El límite de intentos actual vive en la memoria de una instancia. Se pierde al reiniciar y no coordina varias réplicas.
- La actualización de un rol debe validar también que su nuevo nombre no coincida con otro rol existente antes de guardar; actualmente la restricción única de base de datos será la última barrera.

El criterio de esta revisión toma como referencia la guía proporcionada de [buenas prácticas de seguridad para Spring Boot](https://medium.com/@shahharsh172/spring-boot-security-best-practices-a-developers-complete-guide-e91c49dfd5d3): uso de DTOs en lugar de enlazar entidades directamente, validación de entradas, consultas parametrizadas y configuración productiva con secretos externos. La aplicación ya aplica DTOs y validación; las cabeceras y el perfil productivo pertenecen al siguiente endurecimiento.

## 5. Ubicación de credenciales y conexión a base de datos

Las credenciales locales están en dos archivos y se pueden sustituir con variables de entorno:

| Archivo | Propiedad | Uso |
| --- | --- | --- |
| `docker-compose.yml` | `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD` | Crea la base y sus cuentas dentro del contenedor MySQL. |
| `backend/src/main/resources/application.properties` | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Construye la conexión JDBC del backend. Sus valores por defecto coinciden con el usuario de desarrollo creado por Docker Compose. |
| `backend/src/main/resources/application.properties` | `JWT_SECRET`, `JWT_EXPIRATION_MINUTES`, `CORS_ORIGIN` | Configura firma de sesión, vencimiento y origen web permitido. |
| `config/DataInitializer.java` | Cuenta administrativa local de demostración | Contiene la cuenta semilla únicamente para pruebas locales; no debe ser una credencial operativa en producción. |

No se deben versionar contraseñas, secretos JWT ni claves de proveedores en un entorno publicado. Para producción, crear un archivo `.env` excluido por Git o configurar secretos del proveedor de despliegue; definir allí `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` y `CORS_ORIGIN`. Generar un `JWT_SECRET` criptográficamente aleatorio de al menos 32 bytes y no reutilizar los valores locales.

## 6. Ejecución local comprobable

1. Iniciar MySQL desde la raíz: `docker compose up -d`.
2. Construir y arrancar backend: `./mvnw spring-boot:run`.
3. En otra terminal, entrar a `frontend/`, instalar dependencias con `yarn install` y ejecutar `yarn dev`.
4. Abrir `http://localhost:5173`. Durante desarrollo, Vite envía `/api` al backend en el puerto 8080.

El build backend se verifica con `./mvnw package` y el frontend con `yarn build`; el resultado web estático se genera en `frontend/dist/`.

## 7. Publicación y despliegue

### Frontend

El frontend es una SPA Vite estática. Puede publicarse en **Vercel**, **Netlify**, **Cloudflare Pages**, **GitHub Pages** o en un bucket/CDN como Amazon S3 + CloudFront. Vercel y Netlify son opciones directas para conectar este repositorio y generar despliegues por cada push; ambos reconocen proyectos Vite. En Netlify, los valores típicos son directorio base `frontend`, comando `yarn build` y directorio publicado `dist`. Referencias: [Vite en Vercel](https://vercel.com/docs/frameworks/frontend/vite) y [Vite en Netlify](https://docs.netlify.com/build/frameworks/framework-setup-guides/vite/).

Antes de desplegarlo en un dominio separado del backend hay que sustituir la dependencia del proxy de desarrollo: el proxy de `vite.config.js` solo existe localmente. Se debe configurar una URL pública de API, por ejemplo `VITE_API_BASE_URL`, y permitir exactamente el dominio publicado en `CORS_ORIGIN` del backend. La plataforma debe servir `index.html` como fallback para rutas SPA si se añaden rutas del lado cliente.

### Backend

El backend puede desplegarse como servicio Java o como contenedor en **Render**, **Railway**, **Google Cloud Run**, **AWS App Runner/ECS**, **Azure App Service** o en una VM con Nginx/reverse proxy. Render permite conectar un repositorio y ejecutar los comandos de build y arranque; Cloud Run dispone de flujo oficial para servicios Java Spring Boot. Referencias: [despliegue en Render](https://render.com/docs/your-first-deploy) y [Spring Boot en Cloud Run](https://cloud.google.com/run/docs/quickstarts/build-and-deploy/deploy-java-service).

Para el despliegue del backend se recomienda añadir un `Dockerfile` o configurar los comandos Maven del proveedor, exponer el puerto que inyecte la plataforma (`server.port=${PORT:8080}`), usar una instancia MySQL administrada o un MySQL aislado en red privada, y registrar secretos como variables protegidas. El `docker-compose.yml` actual prepara únicamente MySQL para el entorno local; no empaqueta aún la aplicación backend.

## 8. Siguientes entregables recomendados

1. Separar perfiles `dev`, `test` y `prod`; retirar secretos y datos semilla conocidos del perfil productivo.
2. Integrar correo transaccional para recuperación y eliminar la entrega del token en la respuesta HTTP.
3. Añadir pruebas unitarias e integración para login, expiración de JWT, autorización por permiso, bloqueo y recuperación.
4. Incorporar entidades y permisos de órdenes, clientes, vehículos, inventario/refacciones y reportes.
5. Añadir migraciones versionadas con Flyway o Liquibase, observabilidad, CI/CD y análisis de dependencias.
