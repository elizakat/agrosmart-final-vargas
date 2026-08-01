# 🧭 DECISIONES.md — Bitácora de diseño

> **Instrucciones.** Completa **una entrada por fase**, en **primera persona** y
> **refiriéndote a tu propio código**: nombres reales de tus clases, tu tabla, tus
> líneas, tu salida real de terminal.
>
> ❌ **No puntúa** una justificación genérica que podría pegarse en cualquier proyecto
> (ej.: *"usé boundedElastic porque es una buena práctica para operaciones bloqueantes"*).
> ✅ **Sí puntúa** una justificación anclada a tu código (ej.: *"en `ProductoService`
> línea 34 envolví `productoRepository.findAll()` porque Hibernate abre la conexión
> JDBC en el hilo llamante; al probarlo sin `subscribeOn` vi en el log el hilo
> `reactor-http-nio-2`, que es el event loop de Netty"*).
>
> Estas mismas preguntas se te harán en la **defensa oral**.

---

## Datos

- **Nombre:** Katherine Vargas
- **Cédula:** 1725531956
- **NN (dos últimos dígitos):** 56
- **Categoría asignada (según el último dígito):** Flores

---

## Fase 1 — Configuración y perfiles

**1.1** ¿Qué archivo activa el perfil `prod` y qué línea exacta lo hace?

> El perfil `prod` se activa en `src/main/resources/application.properties` mediante esta línea:
>
> `spring.profiles.active=prod`

**1.2** Pega la línea del log de arranque donde se ve tu puerto y el perfil activo.

```
2026-07-30T22:22:50.590-05:00  INFO 49732 --- [agrosmart] [           main] e.e.espe.agrosmart.AgrosmartApplication  : The following 1 profile is active: "prod"

2026-07-30T22:22:57.857-05:00  INFO 49732 --- [agrosmart] [           main] o.s.boot.reactor.netty.NettyWebServer    : Netty started on port 8156 (http)
```

**1.3** ¿Qué habría pasado si dejabas `ddl-auto=create-drop` en lugar de `update`?
Responde pensando en tus datos sembrados.

> Si se dejaba create-drop, hibernate iba a eliminar la tabla en cada nueva ejecución. Al usar update se conserva la tabla y sus datos y se actualiza solamente cuando hay nuevos cambios en base.

**1.4** ¿Levantaste PostgreSQL con `compose.yaml` (Opción A) o con una instalación local
(Opción B)? ¿Qué ventaja tiene la que elegiste?

> Elegí la Opción A porque la libreria de docker de spring  detecta el servicio postgres e inyecta la URL, el usuario y la contraseña automáticamente. Como tengo PostgreSQL local ya utilizaba el puerto 5432, y lo tuve que configurar enel puerto 5434 para evitar conflictos y poder levantar el servicio

---

## Fase 2 — Persistencia con JPA/Hibernate

**2.1** ¿Cuál es el nombre exacto de tu tabla y de dónde salió ese nombre?

> Mi tabla se llama `tbl_productos_base_56` porque los dos últimos números de mi cédula son `56` corresponde a los dos últimos dígitos de mi cédula `1725531956`.


**2.2** Pega la salida de `psql -d agrosmart_db -c "\d tbl_productos_base_NN"` y
señala dónde se ve la restricción `unique` y el `length` de 120.

```
correos_notificacion | character varying(500) |           |          | 
 nombre_producto      | character varying(120) |           | not null | 
 precio_usd           | numeric(10,2)          |           |          | 
 stock_kg             | integer                |           | not null | 
Indexes:
    "tbl_productos_base_56_pkey" PRIMARY KEY, btree (id_producto)
    "ukp0kj0r61xebpbavk4gy5jch6g" UNIQUE CONSTRAINT, btree (nombre_producto)

Podemos ver que en la segunda fila de la tabla el campo nombre producto es del tipo de caracter con longitud de 120 y en la última linea de la salida se ve la constraint donde se indica UNIQUE para el campo nombre_producto
```

**2.3** ¿Por qué usaste `BigDecimal` y no `double` para `precio_usd`? Relaciónalo con el
tipo que generó Hibernate en PostgreSQL.

>Se usó BigDecimal para conservar el valor decimal exacto. Si utilizara double, algunos decimales podrían pueden aproximarse y afectar cálculos monetarios. La anotación @Column(precision = 10, scale = 2) hizo que Hibernate generara en PostgreSQL el tipo numeric(10,2), que permite hasta diez dígitos en total y reserva dos para los centavos.

**2.4** ¿Cómo hiciste idempotente tu siembra y qué pasaría en el segundo arranque si no
lo fuera? (piensa en la restricción `unique` de `nombre_producto`)

>En DataInitializer.run() verifico productoRepository.count() != 0 y retorno antes de insertar cuando la tabla ya contiene registros. En el primer arranque se insertaron cinco productos; en los siguientes no se vuelven a insertar. Sin esta comprobación, saveAll() intentaría repetir los mismos nombres y PostgreSQL rechazaría la operación por la restricción unique de nombre_producto

---

## Fase 3 — Modelo inmutable y lógica funcional

**3.1** ¿Por qué tienes **dos** clases (`ProductoEntity` y `Producto`) en lugar de una?
¿Qué te impide hacer inmutable directamente la entidad de Hibernate?

> `ProductoEntity` representa la tabla `tbl_productos_base_56` que usa Hibernate para las transacciones en base. En cambio, `Producto` representa mi modelo de dominio: es una clase `final`, sus atributos son `private final`, no tiene setters y protege la lista de correos mediante copias defensivas.


**3.2** Escribe el código exacto de **tus dos** copias defensivas e indica en qué línea
está cada una.

```java
// Producto.java, líneas 29-30: copia defensiva de entrada
this.correosNotificacion =
        new ArrayList<>(correosNotificacion);

// Producto.java, líneas 51-53: copia defensiva de salida
return Collections.unmodifiableList(
        new ArrayList<>(correosNotificacion)
);

```

**3.3** ¿Por qué la copia defensiva **solo en el getter** no sería suficiente? Describe
el ataque concreto que quedaría abierto sobre **tu** clase.

>Si creo una lista correos y la paso al constructor de Producto y posteriormente ejecuto correos.clear(), el constructor que hacia referencia a esa lista ve el cambio y también cambiaría. El producto podría pasar de tener correos a quedar con una lista vacía, aunque no tenga setters. La copia de entrada evita este ataque y la copia de salida evita modificaciones mediante el getter.

**3.4** ¿Cómo implementaste `A_MAYUSCULAS` para no mutar el `Producto` recibido?

```java
public static final Function<Producto, Producto> A_MAYUSCULAS =
        producto -> new Producto(
                producto.getId(),
                producto.getNombre().toUpperCase(Locale.ROOT),
                producto.getCategoria(),
                producto.getPrecioUsd(),
                producto.getCorreosNotificacion()
        );

/*Mi función construye una nueva instancia de Producto y conserva los datos del original, cambiando únicamente el nombre a mayúsculas. No modifica el objeto recibido.*/
```

---

## Fase 4 — Servicio reactivo y aislamiento del bloqueo

**4.1** Pega tu método `obtenerProductosComercializables()` completo.

```java
public Flux<Producto> obtenerProductosComercializables() {

    // fromCallable difiere la consulta: findAll se ejecuta al suscribirse.
    return Mono.fromCallable(productoRepository::findAll)

            // JPA/JDBC es bloqueante; boundedElastic evita bloquear
            // los hilos reactor-http-nio del servidor Netty.
            .subscribeOn(Schedulers.boundedElastic())

            // Convierte la lista obtenida por JPA en un flujo de productos.
            .flatMapMany(Flux::fromIterable)

            // Separa la entidad mutable del modelo de dominio inmutable.
            .map(ProductoMapper::toDominio)

            // Crea otro Producto con el nombre en mayúsculas.
            .map(ProductoFilters.A_MAYUSCULAS)

            // Conserva solamente precio > 0 y correos no vacíos.
            .filter(ProductoFilters.IS_VALID)

            // Registra cada producto sin modificarlo.
            .doOnNext(ProductoFilters.LOG_PRODUCTO)

            // Si el filtro elimina todos, emite un producto genérico.
            .defaultIfEmpty(PRODUCTO_GENERICO);
}
```

**4.2** ¿Qué pasa exactamente si eliminas `subscribeOn(boundedElastic())`?

> En mi método `obtenerProductosComercializables()`, `productoRepository.findAll()` utiliza Hibernate y JDBC, que son bloqueantes. Cuando conecte este servicio con `AgroSmartController`, WebFlux realizará la suscripción desde un hilo de Netty llamado normalmente `reactor-http-nio-*`. Si retiro `subscribeOn(Schedulers.boundedElastic())`, la consulta se ejecutaría en ese mismo event loop y el hilo no podría atender otras solicitudes hasta que PostgreSQL respondiera. Con `boundedElastic`, la consulta se mueve a un hilo apropiado para tareas bloqueantes.

**4.3** ¿Por qué `Mono.fromCallable(...)` y no `Mono.just(repository.findAll())`?

> En mi código, `Mono.fromCallable(productoRepository::findAll)` guarda la operación y espera hasta que alguien se suscriba al flujo. Por eso, al arrancar la aplicación todavía no apareció la consulta `findAll()`. En cambio, `Mono.just(productoRepository.findAll())` ejecutaría `findAll()` inmediatamente al construir el `Mono`, antes de que `subscribeOn(boundedElastic())` pueda trasladar la consulta a otro hilo.

**4.4** ¿Dónde usaste `defaultIfEmpty` y dónde `switchIfEmpty`?

> Usé `defaultIfEmpty(PRODUCTO_GENERICO)` al final de `obtenerProductosComercializables()`. Si `IS_VALID` elimina todos los registros, el flujo emite mi producto `"SIN PRODUCTOS COMERCIALIZABLES"`. En `buscarPorId(Long id)` usé `switchIfEmpty(Mono.error(new ProductoNoEncontradoException(id)))`, porque un identificador inexistente debe cambiar a un flujo de error. `defaultIfEmpty` puede emitir un valor, pero no puede producir por sí solo el error reactivo que necesito en `buscarPorId`.

**4.5** ¿Por qué `doOnNext` no sirve para transformar el elemento?

> En mi flujo, `doOnNext(ProductoFilters.LOG_PRODUCTO)` solamente imprime el `id` y el `nombre` de cada producto procesado. Aunque recibe el producto, su tipo es `Consumer<Producto>` y no devuelve un producto nuevo; cualquier valor de retorno sería inexistente o ignorado. 

---

## Fase 5 — Módulo de IA con LangChain4j

**5.1** Pega tu interfaz `AgroSmartAIService` completa.

```java
@AiService
public interface AgroSmartAIService {

    @UserMessage("""
            Redacta una frase publicitaria de máximo 100 caracteres para vender \
            {{producto}} dirigido a {{audiencia}}.""")
    String generarPublicidad(
            @V("producto") String producto,
            @V("audiencia") String audiencia
    );
}
```

**5.2** ¿Qué hace `@V("producto")` y qué pasaría si lo quitaras?

> `@V("producto")` relaciona directamente el parámetro Java `producto` con la variable `{{producto}}` del prompt. De igual manera, `@V("audiencia")` relaciona el segundo parámetro con `{{audiencia}}`. Si quitara `@V`, LangChain4j no entendería esa asociación y podría provocar un error al construir el mensaje.

**5.3** ¿En qué archivo y con qué líneas configuraste el modelo? ¿Por qué no hizo falta un `@Bean`?

> Configuré el modelo en `src/main/resources/application-prod.properties` con estas líneas:

```properties
langchain4j.open-ai.chat-model.api-key=demo
langchain4j.open-ai.chat-model.model-name=gpt-4o-mini
langchain4j.open-ai.chat-model.timeout=30s
langchain4j.open-ai.chat-model.log-requests=true
langchain4j.open-ai.chat-model.log-responses=true
logging.level.dev.langchain4j=DEBUG
```

> No declaré un `@Bean` porque `langchain4j-open-ai-spring-boot-starter` lee estas propiedades y crea automáticamente el modelo.

**5.4** ¿Por qué la llamada a la IA también necesita `boundedElastic`?

> Porque la llamada `agroSmartAIService.generarPublicidad(producto, audiencia)` realiza una petición HTTP (operación bloqueante) y espera la respuesta del proveedor. Por eso se debe envolver en `Mono.fromCallable()` y usar `subscribeOn(Schedulers.boundedElastic())`, evitando bloquear los hilos `reactor-http-nio` de Netty.

**5.5** Si tu proveedor devolvió un error durante el examen...

```text
El proveedor no devolvió errores durante mi ejecución.

```

---

## Fase 6 — API reactiva con WebFlux

**6.1** Pega la salida real de tus cuatro `curl`.

```
GET /api/productos

[{"id":1,"nombre":"ROSAS FREEDOM","categoria":"Flores","precioUsd":2.50,"correosNotificacion":["ventas@flores56.ec","exportaciones@flores56.ec"]},{"id":2,"nombre":"GYPSOPHILA","categoria":"Flores","precioUsd":1.80,"correosNotificacion":["ventas@flores56.ec"]},{"id":3,"nombre":"CLAVELES","categoria":"Flores","precioUsd":1.20,"correosNotificacion":["pedidos@flores56.ec"]}]

GET /api/productos/1

{"id":1,"nombre":"Rosas Freedom","categoria":"Flores","precioUsd":2.50,"correosNotificacion":["ventas@flores56.ec","exportaciones@flores56.ec"]}

GET /api/productos/9999

HTTP/1.1 404 Not Found
Content-Type: application/json
Content-Length: 127

{"timestamp":"2026-07-31T04:45:28.073Z","path":"/api/productos/9999","status":404,"error":"Not Found","requestId":"8ac79cde-3"}

GET /api/agrosmart/publicidad

"Despierta emociones con nuestras Rosas Freedom: elegancia y frescura para tus arreglos premium."
```

**6.2** ¿Cómo lograste que el id inexistente responda **404** y no 500?

>En ProductoService.buscarPorId(), convertí el Optional vacío en un Mono vacío y utilicé switchIfEmpty(Mono.error(new ProductoNoEncontradoException(id))). En ProductoNoEncontradoException agregué @ResponseStatus(HttpStatus.NOT_FOUND). Por eso WebFlux convirtió la excepción en HTTP 404, como comprobé con /api/productos/9999, en lugar de tratarla como un error interno 500.

**6.3** ¿Qué pasaría si tu controlador devolviera `List<Producto>` en lugar de
`Flux<Producto>`? ¿Seguiría compilando? ¿Seguiría siendo no bloqueante?

>Un controlador WebFlux puede compilar devolviendo una lista ya materializada, pero esa respuesta dejaría de representar un flujo reactivo. En mi implementación, ProductoService devuelve Flux<Producto>; para convertirlo directamente en List<Producto> tendría que bloquear el flujo o cambiar el servicio para consultar JPA sin el puente reactivo. Eso impediría atender los productos progresivamente y podría bloquear el hilo de Netty. Por eso mi controlador devuelve directamente Flux<Producto> y Mono<Producto>.

---

## Fase 7 — Pruebas unitarias

**7.1** Pega la salida real de tus pruebas.

```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
```

**7.2** ¿Cuántos productos espera tu `expectNextCount(...)` y por qué?

> En `ProductoServiceTest` utilicé `expectNextCount(3)` porque mis datos de prueba representan la siembra real de `tbl_productos_base_56`: tres productos válidos y dos inválidos. `ProductoFilters.IS_VALID` descarta `"Flores sin precio"` porque su precio es cero y `"Flores sin contacto"` porque su lista de correos está vacía. Por eso el `Flux` debe emitir exactamente tres productos.

**7.3** ¿Por qué mockeaste `ProductoRepository`?

> Mockeé `ProductoRepository` con Mockito para probar solamente el comportamiento de `ProductoService`. Configuré directamente lo que devuelve `findAll()` y `findById()`, por lo que las pruebas no dependen de Docker, PostgreSQL, datos externos ni del estado de `tbl_productos_base_56`. Así son rápidas, repetibles y permiten comprobar los operadores reactivos de forma aislada.

**7.4** ¿Qué demuestra `assertNotSame` que `assertEquals` no demuestra?

> `assertEquals` solamente comprueba que dos listas contienen valores equivalentes. `assertNotSame` comprueba que no son el mismo objeto en memoria. En `ProductoTest` demuestra que `getCorreosNotificacion()` devuelve una lista diferente de la lista original y no expone directamente la referencia interna del producto.

**7.5** ¿Por qué una prueba de un `Flux` sin `verifyComplete()` o `verify()` no prueba nada?

> Los flujos de Reactor son diferidos: no se ejecutan hasta que alguien se suscribe. `verifyComplete()` y `verify()` hacen que `StepVerifier` se suscriba y ejecute el flujo. Sin esa llamada, `findAll()`, los filtros y las verificaciones no se ejecutarían, por lo que la prueba podría terminar sin comprobar realmente ningún resultado.

---

## Fase 8 — Integración y cierre

**8.1** Pega tu `git log --oneline --graph --all`.

```text
* bb62bd3 (HEAD -> feature/documentacion, origin/feature/pruebas, feature/pruebas) test: agrega pruebas del modelo, logica funcional, flujo reactivo e ia
* 3d8703c (origin/feature/api-reactiva, feature/api-reactiva) feat: expone endpoints reactivos y de publicidad
* 5b01287 (origin/feature/ia-langchain4j, feature/ia-langchain4j) feat: integra langchain4j para publicidad de productos
* 1ba65f7 (origin/feature/servicio-reactivo, feature/servicio-reactivo) feat: implementa servicio reactivo con boundedElastic y operadores
* d1d2608 (origin/feature/modelo-inmutable, feature/modelo-inmutable) feat: agrega modelo inmutable de producto y logica funcional
* b12134c (origin/feature/persistencia-jpa, feature/persistencia-jpa) feat: agrega entidad jpa de productos y siembra de datos
* 9ae5f9f (origin/feature/config-perfiles, feature/config-perfiles) chore: configura perfil prod con postgresql y puerto propio
* 6957de3 (origin/main, origin/HEAD, main) chore: inicializa proyecto agrosmart con webflux, jpa y langchain4j
git log --oneline --graph --decorate --all
```

**8.2** ¿Qué fase te tomó más tiempo del previsto y por qué?

> La Fase 1 me tomó más tiempo del previsto por dos problemas de configuración. Aunque `java -version` mostraba Java 21, Maven todavía utilizaba Java 17 y tuve que corregir `JAVA_HOME`. Después, la aplicación no podía autenticarse en PostgreSQL porque yo ya tenía una instalación local ocupando el puerto 5432. Una vez corregidos ambos problemas, la aplicación arrancó con el perfil `prod` y Netty en el puerto 8156.

**8.3** Si tuvieras 30 minutos más, ¿qué mejorarías primero?

> Haría Las pruebas del controlador con postman para verificar todos los endpoints incluyendo el endpoint de publicidad que devuelve texto plano.

**8.4** Declara honestamente qué herramientas consultaste durante el examen y para qué.

> Consulté el repositorio de directrices para verificar los requisitos, las fases, los nombres de las ramas y los commits obligatorios. Utilicé Spring Initializr para generar la estructura inicial con Java 21, WebFlux, JPA, PostgreSQL, Validation y Docker Compose Support. Consulté ChatGPT para comprender conceptos como Netty, `boundedElastic`, idempotencia, copias defensivas y los operadores de Reactor; también lo utilicé para interpretar las fases, proponer borradores de código y pruebas, y diagnosticar los problemas de Java 21, PostgreSQL, Maven y las ramas de Git. Finalmente utilicé los logs de Maven, Docker, PostgreSQL y la aplicación para verificar el funcionamiento real.