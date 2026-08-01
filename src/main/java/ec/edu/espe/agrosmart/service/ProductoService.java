package ec.edu.espe.agrosmart.service;

import ec.edu.espe.agrosmart.domain.Producto;
import ec.edu.espe.agrosmart.domain.ProductoFilters;
import ec.edu.espe.agrosmart.exception.ProductoNoEncontradoException;
import ec.edu.espe.agrosmart.mapper.ProductoMapper;
import ec.edu.espe.agrosmart.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductoService {

    private static final Producto PRODUCTO_GENERICO = new Producto(
            0L,
            "SIN PRODUCTOS COMERCIALIZABLES",
            "Flores",
            BigDecimal.ZERO,
            List.of()
    );

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    public Flux<Producto> obtenerProductosComercializables() {
        // Difiere la consulta bloqueante hasta que alguien se suscribe al flujo.
        return Mono.fromCallable(repository::findAll)

                // JPA usa JDBC y bloquea el hilo; boundedElastic evita bloquear
                // el event loop de Netty.
                .subscribeOn(Schedulers.boundedElastic())

                // Convierte la lista obtenida desde JPA en emisiones individuales.
                .flatMapMany(Flux::fromIterable)

                // Separa la entidad mutable de Hibernate del modelo de dominio.
                .map(ProductoMapper::toDominio)

                // Produce una instancia nueva con el nombre en mayúsculas.
                .map(ProductoFilters.A_MAYUSCULAS)

                // Descarta productos con precio cero o sin correos.
                .filter(ProductoFilters.IS_VALID)

                // Ejecuta la trazabilidad sin modificar el producto emitido.
                .doOnNext(ProductoFilters.LOG_PRODUCTO)

                // Emite un valor conocido si todos los productos fueron descartados.
                .defaultIfEmpty(PRODUCTO_GENERICO);
    }

    public Mono<Producto> buscarPorId(Long id) {
        // La consulta no se ejecuta hasta que exista una suscripción.
        return Mono.fromCallable(() -> repository.findById(id))

                // findById utiliza JDBC bloqueante, por eso sale del event loop.
                .subscribeOn(Schedulers.boundedElastic())

                // Convierte Optional.empty() en un Mono vacío.
                .flatMap(Mono::justOrEmpty)

                // Convierte la entidad recuperada al modelo inmutable.
                .map(ProductoMapper::toDominio)

                // Un Mono vacío se convierte en un error controlado.
                .switchIfEmpty(Mono.error(
                        new ProductoNoEncontradoException(id)
                ));
    }
}