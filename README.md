# PPAI - CU 36: Seguimiento de bolsines

Aplicacion web Java/Spring Boot para consultar en un mapa las ubicaciones GPS de bolsines enviados por la comision medica del empleado autenticado. El caso de uso usa `GpsTrackerPort`; `Xtr4500lAdapter` adapta la respuesta de `Xtr4500lClient` al dominio y valida identificador, coordenadas y fecha/hora antes de exponerla.

## Arquitectura

- `domain/model`: bolsines, comisiones, empleados, estados, ubicaciones y datos de notificacion.
- `application/port`: contratos de persistencia, GPS, gerente de comision y notificacion.
- `application/usecase`: consulta y notificacion de seguimiento.
- `infrastructure/persistence`: adapters Spring Data JPA y entidades PostgreSQL.
- `infrastructure/gps/xtr4500l`: cliente REST, Adapter XTR-4500L y DTOs externos.
- `infrastructure/notification`: envio de correo con Microsoft Graph.
- `infrastructure/web`: API REST, sesion de desarrollo, configuracion de mapa e interfaz.

La consulta restringe el repositorio al estado `ENVIADO` y al codigo de comision de origen del empleado en sesion. Los filtros de precinto y destino de la interfaz solo operan sobre esa respuesta. Al seleccionar un bolsín, el usuario debe confirmar el correo; si confirma, el servidor vuelve a validar el alcance, consulta una ubicacion GPS actual y envia esos datos al gerente de la comision destino.

## Requisitos locales

- Java 17 o superior y Maven.
- PostgreSQL.
- API mock de GPS provista en `dsi-api-bolsines`.
- Acceso de red al proveedor cartografico elegido y al servicio de correo configurado.

## Configuracion

No guardar credenciales ni claves en archivos versionados. PPAI lee la base PostgreSQL desde `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`. La clave compartida XTR se configura mediante `GPS_XTR_API_KEY` en PPAI y en el mock.

El mapa utiliza Google Maps cuando se configura `GOOGLE_MAPS_API_KEY`. La clave se entrega al navegador para cargar el mapa, por lo que debe restringirse por referer HTTP y por las APIs habilitadas en Google Cloud. Sin esa variable, el perfil local conserva el mapa Leaflet/OpenStreetMap existente.

El envio real de correo requiere configurar `MAIL_GRAPH_TENANT_ID`, `MAIL_GRAPH_CLIENT_ID`, `MAIL_GRAPH_CLIENT_SECRET` y `MAIL_GRAPH_SENDER_EMAIL`. El adaptador solicita un token app-only con `client_credentials` y el scope `https://graph.microsoft.com/.default`, y envia mediante `POST /v1.0/users/{MAIL_GRAPH_SENDER_EMAIL}/sendMail`.

En Microsoft Entra, abrir el registro de la aplicacion, ir a **API permissions** y agregar **Microsoft Graph > Application permissions > Mail.Send**. Luego un administrador debe ejecutar **Grant admin consent** para el tenant y comprobar que el permiso aparezca concedido. Debe existir el buzón remitente indicado en `MAIL_GRAPH_SENDER_EMAIL`; las restricciones de acceso a buzones de Exchange también deben permitir ese remitente. No alcanza con configurar las cuatro variables si el permiso o el consentimiento no están concedidos. El endpoint de Graph devuelve `202 Accepted` al aceptar el mensaje; la aplicacion solo informa ese resultado como aceptado, no como confirmacion de entrega. Los errores de token/permisos o envío se devuelven al usuario y no se reemplazan por una captura simulada.

El cuerpo del correo contiene número de bolsín, precinto, código y nombre de comisión origen y destino, latitud, longitud y fecha/hora GPS. Cancelar el diálogo de confirmación no llama al endpoint de notificación. Si no existe email para la comisión destino, el backend lo informa antes de consultar el GPS o llamar a Microsoft Graph.

Los correos de gerentes se consultan por codigo de comision en la tabla `comision_medica` (columna `email_gerente`). Hibernate crea la tabla con la estrategia `update`; cargar y mantener alli las comisiones y direcciones institucionales reales es responsabilidad de la configuracion del entorno. Si el correo de destino no esta registrado, la notificacion no se envia.

## Ejecucion de desarrollo

Crear la base `ppai_bolsines` en PostgreSQL. Configurar las variables indicadas en una terminal y arrancar el mock GPS con su configuracion local. Luego iniciar PPAI:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/ppai_bolsines"
$env:SPRING_DATASOURCE_USERNAME = Read-Host "Usuario PostgreSQL"
$env:SPRING_DATASOURCE_PASSWORD = Read-Host "Clave PostgreSQL"
$env:GPS_XTR_API_KEY = Read-Host "Clave compartida XTR"
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Abrir `http://localhost:8081/`. El perfil `dev` habilita una sesion de prueba; no habilitarlo en un ambiente productivo. La ubicacion de cada bolsín siempre proviene del GPS: la aplicacion no genera coordenadas de respaldo.

## API del caso de uso

- `GET /api/seguimiento/sesion`: devuelve el empleado y su comision en sesion (`204` si no hay sesion).
- `POST /api/seguimiento/consultas/{consultaId}`: registra el token de cancelación para una consulta asociada a la sesión actual.
- `GET /api/seguimiento/bolsines?consultaId={consultaId}`: consulta bolsines enviados del origen permitido y sus ubicaciones GPS (`401` sin sesión, `409` si se canceló o no está activa, `502` si falla o devuelve datos inválidos el GPS).
- `POST /api/seguimiento/consultas/{consultaId}/cancelacion`: cancela la consulta activa, abortando la llamada HTTP XTR en curso y evitando iniciar las ubicaciones pendientes.
- `POST /api/seguimiento/bolsines/{numeroBolsin}/notificacion`: vuelve a verificar el bolsín, obtiene la ubicacion GPS actual y envia el correo al gerente destino (`202` al aceptar Graph, error si no puede enviarlo).
- `POST /api/dev/sesion` y `DELETE /api/dev/sesion`: iniciar y cerrar la sesion local, solo con perfil `dev`.
- `GET /api/config/maps`: publica la clave de mapa configurada; responde vacia cuando no hay clave.

## Pruebas

```powershell
mvn test
```

Las pruebas usan H2/mock MVC y dobles de GPS, persistencia y correo; no requieren credenciales externas. Cubren el Adapter XTR, restricciones de consulta, carga de sesion y la solicitud de correo a Microsoft Graph con el contenido requerido.
