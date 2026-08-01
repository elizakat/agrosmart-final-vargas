package ec.edu.espe.agrosmart.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductoTest {

    @Test
    void getters_conDatosDelConstructor_debenDevolverLosMismosValores() {
        // Arrange
        List<String> correos = List.of("ventas@flores56.ec");
        Producto producto = new Producto(
                1L, "Rosas Freedom", "Flores",
                new BigDecimal("2.50"), correos
        );

        // Act & Assert
        assertEquals(1L, producto.getId());
        assertEquals("Rosas Freedom", producto.getNombre());
        assertEquals("Flores", producto.getCategoria());
        assertEquals(new BigDecimal("2.50"), producto.getPrecioUsd());
        assertEquals(correos, producto.getCorreosNotificacion());
    }

    @Test
    void constructor_alModificarListaOriginal_noDebeCambiarElProducto() {
        // Arrange
        List<String> correos = new ArrayList<>();
        correos.add("ventas@flores56.ec");

        Producto producto = new Producto(
                1L, "Rosas Freedom", "Flores",
                new BigDecimal("2.50"), correos
        );

        // Act
        correos.add("intruso@correo.ec");

        // Assert
        assertEquals(1, producto.getCorreosNotificacion().size());
        assertNotSame(correos, producto.getCorreosNotificacion());
    }

    @Test
    void getCorreosNotificacion_alIntentarModificar_debeLanzarExcepcion() {
        // Arrange
        Producto producto = new Producto(
                1L, "Rosas Freedom", "Flores",
                new BigDecimal("2.50"),
                List.of("ventas@flores56.ec")
        );

        // Act
        List<String> resultado = producto.getCorreosNotificacion();

        // Assert
        assertNotSame(resultado, producto.getCorreosNotificacion());
        assertThrows(
                UnsupportedOperationException.class,
                () -> resultado.add("otro@correo.ec")
        );
    }
}