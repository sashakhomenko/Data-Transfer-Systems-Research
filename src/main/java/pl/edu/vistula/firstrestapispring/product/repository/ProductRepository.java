package pl.edu.vistula.firstrestapispring.product.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.vistula.firstrestapispring.product.domain.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}

