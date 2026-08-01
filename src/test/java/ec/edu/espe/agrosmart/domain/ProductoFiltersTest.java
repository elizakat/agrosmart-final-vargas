package ec.edu.espe.agrosmart.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductoFiltersTest {

    @Test
    void isValid_conPrecioPositivoYCorreos_debeDevolverTrue() {
        // Arrange
        Producto producto = producto(
                new BigDecimal("2.50"),
                List.of("ventas@flores56.ec")
        );

        // Act
        boolean resultado = ProductoFilters.IS_VALID.test(producto);

        // Assert
        assertTrue(resultado);
    }

    @Test
    void isValid_conPrecioCero_debeDevolverFalse() {
        // Arrange
        Producto producto = producto(
                BigDecimal.ZERO,
                List.of("ventas@flores56.ec")
        );

        // Act
        boolean resultado = ProductoFilters.IS_VALID.test(producto);

        // Assert
        assertFalse(resultado);
    }

    @Test
    void isValid_sinCorreos_debeDevolverFalse() {
        // Arrange
        Producto producto = producto(
                new BigDecimal("2.50"),
                List.of()
        );

        // Act
        boolean resultado = ProductoFilters.IS_VALID.test(producto);

        // Assert
        assertFalse(resultado);
    }

    @Test
    void aMayusculas_conProducto_debeCrearUnaInstanciaNueva() {
        // Arrange
        Producto original = producto(
                new BigDecimal("2.50"),
                List.of("ventas@flores56.ec")
        );

        // Act
        Producto resultado = ProductoFilters.A_MAYUSCULAS.apply(original);

        // Assert
        assertNotSame(original, resultado);
        assertEquals("Rosas Freedom", original.getNombre());
        assertEquals("ROSAS FREEDOM", resultado.getNombre());
    }

    private Producto producto(
            BigDecimal precio,
            List<String> correos) {
        return new Producto(
                1L, "Rosas Freedom", "Flores", precio, correos
        );
    }
}