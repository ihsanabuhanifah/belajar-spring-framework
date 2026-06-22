package programmerzamannow.mypasar.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.jpa.domain.Specification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.criteria.Predicate;

import programmerzamannow.mypasar.category.CategoryRespository;
import programmerzamannow.mypasar.category.entity.Category;
import programmerzamannow.mypasar.product.dto.CreateProductDto;
import programmerzamannow.mypasar.product.dto.ResponseProductDto;
import programmerzamannow.mypasar.product.dto.SearchProductRequestDto;
import programmerzamannow.mypasar.product.entity.Product;
import programmerzamannow.mypasar.shared.jwt.DecodeJwtService;
import programmerzamannow.mypasar.shared.validation.ValidationService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Category not found"));

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

        @Transactional(readOnly = true)
        public Page<ResponseProductDto> getAllProductsPagedAndSearched(SearchProductRequestDto request) {
                validationService.validate(request);

                Specification<Product> specification = (root, query, criteriaBuilder) -> {
                        List<Predicate> predicates = new ArrayList<>();

                        if (request.getName() != null && !request.getName().isBlank()) {
                                predicates.add(criteriaBuilder.like(
                                                criteriaBuilder.lower(root.get("name")),
                                                "%" + request.getName().toLowerCase() + "%"));
                        }

                        if (request.getMinPrice() != null) {
                                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                                root.get("price").as(BigDecimal.class), // 🌟 Ditambahkan
                                                                                        // .as(BigDecimal.class)
                                                request.getMinPrice()));
                        }

                        // Filter 3: Perbaikan Harga Maksimal (price <= maxPrice)
                        if (request.getMaxPrice() != null) {
                                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                                root.get("price").as(BigDecimal.class), // 🌟 Ditambahkan
                                                                                        // .as(BigDecimal.class)
                                                request.getMaxPrice()));
                        }

                        if (request.getCategoryId() != null && !request.getCategoryId().isBlank()) {
                                predicates.add(criteriaBuilder.equal(root.get("category").get("id"),
                                                request.getCategoryId()));
                        }

                        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
                };

                // 2. Set halaman dan ukuran data (Urut nama secara Alphabet)
                Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

                // 3. Eksekusi pencarian dinamis ke MySQL
                Page<Product> productPage = productRepository.findAll(specification, pageable);

                // 4. Konversi ke bentuk DTO
                return productPage.map(product -> ResponseProductDto.builder()
                                .id(product.getId())
                                .name(product.getName())
                                .price(product.getPrice())
                                .stock(product.getStock())
                                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                                .build());
        }
}