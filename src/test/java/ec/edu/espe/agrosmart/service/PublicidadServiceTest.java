package ec.edu.espe.agrosmart.service;

import ec.edu.espe.agrosmart.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

class PublicidadServiceTest {

    @Test
    void generarPublicidad_conRespuestaExitosa_debeEmitirTexto() {
        // Arrange
        ProductoRepository repository =
                Mockito.mock(ProductoRepository.class);
        AgroSmartAIService ia =
                Mockito.mock(AgroSmartAIService.class);

        when(ia.generarPublicidad(
                "Rosas Freedom",
                "floristerías premium"
        )).thenReturn("Elegancia y frescura para cada ocasión.");

        ProductoService service =
                new ProductoService(repository, ia);

        // Act & Assert
        StepVerifier.create(service.generarPublicidad(
                        "Rosas Freedom",
                        "floristerías premium"
                ))
                .expectNext("Elegancia y frescura para cada ocasión.")
                .verifyComplete();
    }

    @Test
    void generarPublicidad_cuandoProveedorFalla_debeEmitirRespaldo() {
        // Arrange
        ProductoRepository repository =
                Mockito.mock(ProductoRepository.class);
        AgroSmartAIService ia =
                Mockito.mock(AgroSmartAIService.class);

        when(ia.generarPublicidad(
                "Rosas Freedom",
                "floristerías premium"
        )).thenThrow(new RuntimeException("429 Too Many Requests"));

        ProductoService service =
                new ProductoService(repository, ia);

        // Act & Assert
        StepVerifier.create(service.generarPublicidad(
                        "Rosas Freedom",
                        "floristerías premium"
                ))
                .expectNextMatches(texto ->
                        texto.contains("Publicidad no disponible")
                                && texto.contains("RuntimeException"))
                .verifyComplete();
    }
}