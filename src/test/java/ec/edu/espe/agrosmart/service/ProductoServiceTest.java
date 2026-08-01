package ec.edu.espe.agrosmart.service;

import ec.edu.espe.agrosmart.entity.ProductoEntity;
import ec.edu.espe.agrosmart.exception.ProductoNoEncontradoException;
import ec.edu.espe.agrosmart.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;

class ProductoServiceTest {

    @Test
    void obtenerProductosComercializables_conTresValidosYDosInvalidos_debeEmitirTres() {
        // Arrange
        ProductoRepository repository =
                Mockito.mock(ProductoRepository.class);
        AgroSmartAIService ia =
                Mockito.mock(AgroSmartAIService.class);

        when(repository.findAll()).thenReturn(List.of(
                entity(1L, "Rosas Freedom", "2.50", "ventas@flores56.ec"),
                entity(2L, "Gypsophila", "1.80", "ventas@flores56.ec"),
                entity(3L, "Claveles", "1.20", "pedidos@flores56.ec"),
                entity(4L, "Flores de descarte", "0.00", "calidad@flores56.ec"),
                entity(5L, "Flores sin contacto", "0.90", "")
        ));

        ProductoService service =
                new ProductoService(repository, ia);

        // Act & Assert
        StepVerifier.create(service.obtenerProductosComercializables())
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void obtenerProductosComercializables_conTodosInvalidos_debeEmitirGenerico() {
        // Arrange
        ProductoRepository repository =
                Mockito.mock(ProductoRepository.class);
        AgroSmartAIService ia =
                Mockito.mock(AgroSmartAIService.class);

        when(repository.findAll()).thenReturn(List.of(
                entity(4L, "Flores de descarte", "0.00", "correo@flores56.ec"),
                entity(5L, "Flores sin contacto", "1.00", "")
        ));

        ProductoService service =
                new ProductoService(repository, ia);

        // Act & Assert
        StepVerifier.create(service.obtenerProductosComercializables())
                .expectNextMatches(producto ->
                        producto.getId().equals(0L)
                                && producto.getNombre()
                                .equals("SIN PRODUCTOS COMERCIALIZABLES"))
                .verifyComplete();
    }

    @Test
    void buscarPorId_conIdInexistente_debeEmitirError() {
        // Arrange
        ProductoRepository repository =
                Mockito.mock(ProductoRepository.class);
        AgroSmartAIService ia =
                Mockito.mock(AgroSmartAIService.class);

        when(repository.findById(9999L))
                .thenReturn(Optional.empty());

        ProductoService service =
                new ProductoService(repository, ia);

        // Act & Assert
        StepVerifier.create(service.buscarPorId(9999L))
                .expectError(ProductoNoEncontradoException.class)
                .verify();
    }

    private ProductoEntity entity(
            Long id,
            String nombre,
            String precio,
            String correos) {

        ProductoEntity entity = new ProductoEntity(
                nombre,
                new BigDecimal(precio),
                50,
                "Flores",
                correos
        );
        entity.setIdProducto(id);
        return entity;
    }
}