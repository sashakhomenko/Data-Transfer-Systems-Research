package pl.edu.vistula.firstrestapispring.grpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.vistula.firstrestapispring.product.domain.Product;
import pl.edu.vistula.firstrestapispring.product.repository.ProductRepository;
import pl.edu.vistula.firstrestapispring.grpc.ProductProto;
import pl.edu.vistula.firstrestapispring.grpc.ProductServiceGrpc;


import java.util.List;

@GrpcService
public class ProductGrpcService extends ProductServiceGrpc.ProductServiceImplBase {

    @Autowired
    private ProductRepository productRepository;

    @Override
    public void getProduct(ProductProto.ProductRequest request,
                           StreamObserver<ProductProto.ProductResponse> responseObserver) {

        Product product = productRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + request.getId()));

        ProductProto.ProductResponse response = ProductProto.ProductResponse.newBuilder()
                .setId(product.getId())
                .setName(product.getName())
                .build();

        responseObserver.onNext(response);   // send the response back to the caller here
        responseObserver.onCompleted();
    }

    @Override
    public void getAllProducts(ProductProto.EmptyRequest request,
                               StreamObserver<ProductProto.ProductListResponse> responseObserver) {

        List<Product> products = productRepository.findAll();

        ProductProto.ProductListResponse.Builder listBuilder =
                ProductProto.ProductListResponse.newBuilder();

        for (Product product : products) {
            listBuilder.addProducts(
                    ProductProto.ProductResponse.newBuilder()
                            .setId(product.getId())
                            .setName(product.getName())
                            .build()
            );
        }

        responseObserver.onNext(listBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void createProduct(ProductProto.CreateProductRequest request,
                              StreamObserver<ProductProto.ProductResponse> responseObserver) {

        Product product = new Product();
        product.setName(request.getName());
        product = productRepository.save(product);

        ProductProto.ProductResponse response = ProductProto.ProductResponse.newBuilder()
                .setId(product.getId())
                .setName(product.getName())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}