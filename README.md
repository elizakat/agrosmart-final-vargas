# AgroSmart

API reactiva para consultar productos agrícolas y generar publicidad mediante inteligencia artificial. El proyecto utiliza Spring WebFlux, JPA/Hibernate, PostgreSQL, Docker Compose y LangChain4j.

## Semilla personal

La semilla se obtuvo de los dos últimos dígitos de la cédula `1725531956`.

| Parámetro | Valor |
|---|---|
| NN | `56` |
| Tabla PostgreSQL | `tbl_productos_base_56` |
| Puerto de la aplicación | `8156` |
| Último dígito | `6` |
| Categoría | `Flores` |
| Audiencia | `floristerías premium` |

El puerto comienza con `81` y termina con los mismos dígitos `56` utilizados en el nombre de la tabla.

## Tecnologías

- Java 21
- Spring Boot 4
- Spring WebFlux y Netty
- Spring Data JPA e Hibernate
- PostgreSQL
- Docker Compose
- Project Reactor
- LangChain4j
- JUnit 5, Mockito y Reactor Test

## Requisitos

- Java 21 configurado en `JAVA_HOME`.
- Docker Desktop iniciado.
- Git.
- No es necesario instalar Maven porque el proyecto incluye Maven Wrapper.

## Ejecución

El archivo `compose.yaml` crea automáticamente la base de datos:

```text
Base de datos: agrosmart_db
Usuario: agrosmart
Puerto externo de PostgreSQL: 5434
Puerto interno del contenedor: 5432
```

Se utilizó `5434:5432` porque PostgreSQL local ya ocupaba el puerto `5432`.

No se declararon propiedades `spring.datasource.*`: Spring Boot Docker Compose Support obtiene automáticamente la URL y las credenciales del servicio.

Para ejecutar:

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
```

La aplicación queda disponible en:

```text
http://localhost:8156
```

El perfil activo es `prod`, configurado en:

```properties
spring.profiles.active=prod
```

Hibernate conserva la estructura y los datos mediante:

```properties
spring.jpa.hibernate.ddl-auto=update
```

## Datos iniciales

`DataInitializer` realiza una siembra idempotente: solamente inserta datos cuando `productoRepository.count()` es cero.

La tabla contiene:

- Tres productos válidos: precio mayor que cero y correos presentes.
- Un producto inválido por precio igual a cero.
- Un producto inválido por correos vacíos.

Todos pertenecen a la categoría `Flores`.

## Endpoints

| Método | Ruta | Retorno | Descripción |
|---|---|---|---|
| GET | `/api/productos` | `Flux<Producto>` | Devuelve los productos comercializables |
| GET | `/api/productos/{id}` | `Mono<Producto>` | Busca un producto por identificador |
| GET | `/api/agrosmart/publicidad` | `Mono<String>` | Genera publicidad con LangChain4j |

### Productos comercializables

```powershell
curl.exe http://localhost:8156/api/productos
```

Salida real:

```json
[
  {
    "id": 1,
    "nombre": "ROSAS FREEDOM",
    "categoria": "Flores",
    "precioUsd": 2.50,
    "correosNotificacion": [
      "ventas@flores56.ec",
      "exportaciones@flores56.ec"
    ]
  },
  {
    "id": 2,
    "nombre": "GYPSOPHILA",
    "categoria": "Flores",
    "precioUsd": 1.80,
    "correosNotificacion": [
      "ventas@flores56.ec"
    ]
  },
  {
    "id": 3,
    "nombre": "CLAVELES",
    "categoria": "Flores",
    "precioUsd": 1.20,
    "correosNotificacion": [
      "pedidos@flores56.ec"
    ]
  }
]
```

### Producto existente

```powershell
curl.exe http://localhost:8156/api/productos/1
```

Salida real:

```json
{
  "id": 1,
  "nombre": "Rosas Freedom",
  "categoria": "Flores",
  "precioUsd": 2.50,
  "correosNotificacion": [
    "ventas@flores56.ec",
    "exportaciones@flores56.ec"
  ]
}
```

### Producto inexistente

```powershell
curl.exe -i http://localhost:8156/api/productos/9999
```

Resultado:

```text
HTTP/1.1 404 Not Found
```

`ProductoService.buscarPorId()` emite `ProductoNoEncontradoException` mediante `switchIfEmpty`. La excepción tiene `@ResponseStatus(HttpStatus.NOT_FOUND)`.

### Publicidad con IA

```powershell
curl.exe "http://localhost:8156/api/agrosmart/publicidad?producto=Rosas%20Freedom&audiencia=florister%C3%ADas%20premium"
```

Salida real:

```text
"Despierta emociones con nuestras Rosas Freedom: elegancia y frescura para tus arreglos premium."
```

Si el proveedor falla, `onErrorResume` devuelve una respuesta controlada en lugar de propagar el error.

## Puente bloqueante a reactivo

JPA/Hibernate utiliza JDBC y realiza operaciones bloqueantes. LangChain4j también espera de forma bloqueante la respuesta HTTP del proveedor.

Por esta razón, ambas operaciones se envuelven de esta forma:

```java
Mono.fromCallable(...)
        .subscribeOn(Schedulers.boundedElastic());
```

`fromCallable` difiere la operación hasta la suscripción. `boundedElastic` ejecuta el bloqueo fuera de los hilos `reactor-http-nio` de Netty, evitando detener la atención de otras solicitudes.

El proyecto no utiliza `block()`, `blockFirst()`, `blockLast()` ni devuelve colecciones bloqueantes desde el servicio o controlador.

## Operadores reactivos

| Operador | Uso en el proyecto |
|---|---|
| `Mono.fromCallable` | Difiere las consultas JPA y la llamada a la IA |
| `subscribeOn(boundedElastic)` | Aísla las operaciones bloqueantes |
| `flatMapMany` | Convierte la lista obtenida por JPA en un `Flux` |
| `flatMap` | Convierte `Optional.empty()` en un `Mono` vacío |
| `map` | Convierte entidad a dominio y crea nombres en mayúsculas |
| `filter` | Conserva productos con precio positivo y correos |
| `doOnNext` | Registra el identificador y nombre sin transformar |
| `defaultIfEmpty` | Emite un producto genérico si todos son inválidos |
| `switchIfEmpty` | Emite un error cuando el identificador no existe |
| `timeout` | Limita la espera del proveedor de IA a 30 segundos |
| `doOnError` | Registra el mensaje del proveedor |
| `onErrorResume` | Convierte el fallo de IA en una respuesta de respaldo |

## Modelo y persistencia

`ProductoEntity` es mutable porque Hibernate necesita constructor vacío y setters.

`Producto` es el modelo de dominio inmutable:

- Clase `final`.
- Atributos `private final`.
- Sin setters.
- Copia defensiva de entrada.
- Copia defensiva de salida no modificable.

`ProductoMapper` transforma `ProductoEntity` en `Producto` y convierte los correos separados por comas en una lista.

## Pruebas

Las pruebas son unitarias y no dependen de PostgreSQL ni de internet. `ProductoRepository` y `AgroSmartAIService` se sustituyen con mocks de Mockito.

Para ejecutarlas:

```powershell
.\mvnw.cmd clean test
```

Se validan:

- Getters e inmutabilidad de `Producto`.
- Los tres casos de `ProductoFilters.IS_VALID`.
- Creación de una instancia nueva con `A_MAYUSCULAS`.
- Tres productos comercializables de una siembra con cinco registros.
- Producto genérico cuando todos los registros son inválidos.
- Error para un identificador inexistente.
- Respuesta y recuperación de fallos de la IA.

## Evidencias

- [Arranque con perfil prod y puerto 8156](docs/evidencias/01-arranque-prod-puerto-8156.png)
- [Estructura de tbl_productos_base_56](docs/evidencias/02-estructura-tabla-56.png)
- [Datos de la siembra](docs/evidencias/03-datos-semilla-56.png)
- [Ejecución de los endpoints](docs/evidencias/04-endpoints-curl.png)
- [Pruebas en verde](docs/evidencias/05-pruebas-verdes.png)
- [Historial Git](docs/evidencias/06-historial-git.png)