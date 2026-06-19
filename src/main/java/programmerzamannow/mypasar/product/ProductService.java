package programmerzamannow.mypasar.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import programmerzamannow.mypasar.category.CategoryRespository;
import programmerzamannow.mypasar.category.entity.Category;
import programmerzamannow.mypasar.product.dto.CreateProductDto;
import programmerzamannow.mypasar.product.dto.ResponseProductDto;
import programmerzamannow.mypasar.product.entity.Product;
import programmerzamannow.mypasar.shared.jwt.DecodeJwtService;
import programmerzamannow.mypasar.shared.validation.ValidationService;
import java.util.UUID;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRespository categoryRepository;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private DecodeJwtService decodeJwtService;

    @Transactional
    public ResponseProductDto createProduct(CreateProductDto request) {
        // 1. Validasi data input
        validationService.validate(request);

        // 2. Pastikan Kategori yang dipilih pembeli itu nyata/ada di MySQL
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        // 3. Konversi DTO ke Entity Product
        Product product = Product.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .price(request.getPrice())
                .stock(request.getStock())
                .createdBy(decodeJwtService.getCurrentName())
                .category(category) // Pasangkan kategorinya di sini
                .build();

        // 4. Simpan ke Database
        productRepository.save(product);

        // 5. Kembalikan bentuk DTO murni ke Controller
        return ResponseProductDto.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .stock(product.getStock())
                .categoryId(category.getId())
                .build();
    }
}