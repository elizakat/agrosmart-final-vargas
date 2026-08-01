package ec.edu.espe.agrosmart.config;

import ec.edu.espe.agrosmart.entity.ProductoEntity;
import ec.edu.espe.agrosmart.repository.ProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ProductoDataSeeder implements CommandLineRunner {

    private final ProductoRepository repository;

    public ProductoDataSeeder(ProductoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            repository.saveAll(List.of(
                    new ProductoEntity(
                            "Rosas Freedom",
                            new BigDecimal("2.50"),
                            100,
                            "Flores",
                            "ventas@flores56.ec,exportaciones@flores56.ec"
                    ),
                    new ProductoEntity(
                            "Gypsophila",
                            new BigDecimal("1.80"),
                            80,
                            "Flores",
                            "ventas@flores56.ec"
                    ),
                    new ProductoEntity(
                            "Claveles",
                            new BigDecimal("1.20"),
                            60,
                            "Flores",
                            "pedidos@flores56.ec"
                    ),
                    new ProductoEntity(
                            "Flores de descarte",
                            new BigDecimal("0.00"),
                            30,
                            "Flores",
                            "calidad@flores56.ec"
                    ),
                    new ProductoEntity(
                            "Flores sin contacto",
                            new BigDecimal("0.90"),
                            40,
                            "Flores",
                            ""
                    )
            ));
        }
    }
}