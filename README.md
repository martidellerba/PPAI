# PPAI - CU 36: Seguimiento de bolsines

Aplicacion web Java/Spring Boot para consultar en un mapa las ubicaciones GPS de bolsines enviados por la comision medica del empleado autenticado. El caso de uso usa `GpsTrackerPort`; `Xtr4500lAdapter` adapta la respuesta de `Xtr4500lClient` al dominio y valida identificador, coordenadas y fecha/hora antes de exponerla.

## Arquitectura

- `domain/model`: bolsines, comisiones, empleados, estados, ubicaciones y datos de notificacion.
- `application/port`: contratos de persistencia y GPS.
- `application/usecase`: consulta y notificacion de seguimiento.
- `infrastructure/persistence`: adapters Spring Data JPA y entidades PostgreSQL.
- `infrastructure/gps/xtr4500l`: cliente REST, Adapter XTR-4500L y DTOs externos.
- `infrastructure/web`: API REST, inicio/cierre de sesion, configuracion de mapa e interfaz.

La consulta restringe el repositorio al estado `ENVIADO` y al codigo de comision de origen del empleado en sesion. Los filtros de precinto y destino de la interfaz solo operan sobre esa respuesta. Cada bolsín tiene un botón propio para enviar una notificacion interna a la bandeja de su comision de destino; luego se muestra `Notificación enviada`. Los usuarios que inicien sesión con esa comisión pueden verla en `Notificaciones`. No requiere correo ni credenciales de un proveedor externo. Seleccionar una fila solo muestra su detalle y ubicación.

## Requisitos locales

- Java 17 o superior y Maven.
- H2 para desarrollo (se crea una base local persistente en `data/` sin contraseña).
- PostgreSQL solo para otros entornos.
- API mock de GPS provista en `dsi-api-bolsines`.
- Acceso de red al proveedor cartografico elegido y al GPS.

## Configuracion

No guardar credenciales ni claves reales en archivos versionados. El perfil local `dev` usa una base H2 persistente en `data/` con usuario `sa` y contraseña vacía. Para otros entornos, PPAI lee PostgreSQL desde `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`. La clave XTR local predeterminada es `dev-xtr-local-key`; para reemplazarla, configurar `GPS_XTR_API_KEY` con el mismo valor en PPAI y en el mock.

El mapa utiliza Google Maps cuando se configura `GOOGLE_MAPS_API_KEY`. La clave se entrega al navegador para cargar el mapa, por lo que debe restringirse por referer HTTP y por las APIs habilitadas en Google Cloud. Sin esa variable, el perfil local conserva el mapa Leaflet/OpenStreetMap existente.

La notificacion es interna: al usar el botón, queda disponible en la bandeja de los usuarios de la comisión de destino y no se duplica al volver a tocarlo, incluso desde otra sesión. No requiere Microsoft Graph, credenciales de correo ni la direccion de un gerente. No se envia correo.

## Ejecucion de desarrollo

No hace falta instalar/configurar PostgreSQL ni ingresar una contraseña para probar localmente. En una terminal, arrancar el mock GPS:

```powershell
cd dsi-api-bolsines
.\mvnw.cmd spring-boot:run
```

En otra terminal, desde la raíz del proyecto, iniciar PPAI:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Abrir `http://localhost:8081/`. El perfil `dev` crea dos cuentas si no existen: `ana@example.com` / `demo1234` (comisión `CM-CBA-01`) y `luis@example.com` / `demo1234` (comisión `CM-ROS-02`). Para cambiar las cuentas de prueba se usan `PPAI_DEV_USER_EMAIL`, `PPAI_DEV_USER_PASSWORD`, `PPAI_DEV_USER2_EMAIL` y `PPAI_DEV_USER2_PASSWORD`. No habilitar el perfil `dev` en un ambiente productivo. Los datos se conservan entre ejecuciones en `data/`. La ubicación mostrada siempre proviene del GPS mock: la aplicación no genera coordenadas de respaldo.

Las cuentas se almacenan en `usuario_sistema`: email en minúsculas, nombre, apellido, hash BCrypt de la contraseña y código de comisión existente. No hay registro público; el administrador de la base debe crear las cuentas y guardar solo hashes (nunca contraseñas en texto plano). En PostgreSQL se puede habilitar `pgcrypto` y generar un hash compatible con BCrypt mediante `crypt(:password, gen_salt('bf', 12))` en una inserción parametrizada. Cada usuario queda asociado a su propia comisión y sesión.

## API del caso de uso

- `GET /api/seguimiento/sesion`: devuelve el empleado y su comision en sesion (`204` si no hay sesion; la respuesta no se almacena en cache).
- `POST /api/sesion`: inicia sesión con `{ "email": "...", "password": "..." }`; las credenciales inválidas devuelven `401`.
- `DELETE /api/sesion`: cierra la sesión actual.
- `POST /api/seguimiento/consultas/{consultaId}`: registra el token de cancelación para una consulta asociada a la sesión actual.
- `GET /api/seguimiento/consultar-ubicacion?consultaId={consultaId}`: consulta bolsines enviados del origen permitido y sus ubicaciones GPS (`401` sin sesión, `409` si se canceló o no está activa, `502` si falla o devuelve datos inválidos el GPS). `/api/seguimiento/bolsines` se mantiene como alias compatible.
- `POST /api/seguimiento/consultas/{consultaId}/cancelacion`: cancela la consulta activa, abortando la llamada HTTP XTR en curso y evitando iniciar las ubicaciones pendientes.
- `POST /api/seguimiento/bolsines/{numeroBolsin}/notificacion`: envía la notificacion a la bandeja de la comisión destino una sola vez (`202` al enviarla, `200` si ya existe).
- `GET /api/seguimiento/notificaciones`: lista notificaciones recibidas en la bandeja de la comisión asignada a la sesión.
- `GET /api/config/maps`: publica la clave de mapa configurada; responde vacia cuando no hay clave.

## Pruebas

```powershell
mvn test
```

Las pruebas usan H2/mock MVC y dobles de GPS y persistencia; no requieren credenciales externas. Cubren el Adapter XTR, restricciones de consulta, sesiones independientes por usuario, cancelación de consultas y entrega única de notificaciones a la bandeja de destino.
