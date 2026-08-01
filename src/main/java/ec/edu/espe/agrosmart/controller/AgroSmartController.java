package ec.edu.espe.agrosmart.controller;

import ec.edu.espe.agrosmart.domain.Producto;
import ec.edu.espe.agrosmart.service.ProductoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class AgroSmartController {

    private final ProductoService productoService;

    public AgroSmartController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping("/productos")
    public Flux<Producto> obtenerProductos() {
        return productoService.obtenerProductosComercializables();
    }

    @GetMapping("/productos/{id}")
    public Mono<Producto> buscarPorId(@PathVariable Long id) {
        return productoService.buscarPorId(id);
    }

    @GetMapping(
            value = "/agrosmart/publicidad",
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public Mono<String> generarPublicidad(
            @RequestParam String producto,
            @RequestParam String audiencia) {
        return productoService.generarPublicidad(producto, audiencia);
    }
}