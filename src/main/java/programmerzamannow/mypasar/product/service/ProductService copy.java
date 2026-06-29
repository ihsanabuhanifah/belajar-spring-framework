// package programmerzamannow.mypasar.product.service;

// import org.springframework.amqp.rabbit.core.RabbitTemplate;
// import org.springframework.transaction.support.TransactionSynchronization;
// import org.springframework.transaction.support.TransactionSynchronizationManager;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.HttpStatus;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import org.springframework.web.server.ResponseStatusException;
// import org.springframework.data.jpa.domain.Specification;

// import org.springframework.data.domain.Page;
// import org.springframework.data.domain.PageRequest;
// import org.springframework.data.domain.Pageable;
// import jakarta.persistence.criteria.Predicate;
// import programmerzamannow.mypasar.category.CategoryRespository;
// import programmerzamannow.mypasar.category.entity.Category;
// import programmerzamannow.mypasar.product.dto.CreateBulkProductDto;
// import programmerzamannow.mypasar.product.dto.CreateProductDto;
// import programmerzamannow.mypasar.product.dto.ResponseProductDto;
// import programmerzamannow.mypasar.product.dto.SearchProductRequestDto;
// import programmerzamannow.mypasar.product.entity.Product;
// import programmerzamannow.mypasar.product.repository.ProductRepository;
// import programmerzamannow.mypasar.shared.config.RabbitMqConfig;
// import programmerzamannow.mypasar.shared.jwt.DecodeJwtService;
// import programmerzamannow.mypasar.shared.validation.ValidationService;

// import java.math.BigDecimal;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.UUID;

// @Service
// public class ProductService {

//         @Autowired
//         private ProductRepository productRepository;

//         @Autowired
//         private CategoryRespository categoryRepository;

//         @Autowired
//         private ValidationService validationService;

//         @Autowired
//         private DecodeJwtService decodeJwtService;

//         @Autowired
//         private RabbitTemplate rabbitTemplate;

//         @Transactional
//         public ResponseProductDto createProduct(CreateProductDto request) {
//                 // 1. Validasi data input
//                 validationService.validate(request);

//                 // 2. Pastikan Kategori yang dipilih pembeli itu nyata/ada di MySQL
//                 Category category = categoryRepository.findById(request.getCategoryId())
//                                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
//                                                 "Category not found"));

//                 // 3. Konversi DTO ke Entity Product
//                 Product product = Product.builder()
//                                 .id(UUID.randomUUID().toString())
//                                 .name(request.getName())
//                                 .price(request.getPrice())
//                                 .stock(request.getStock())
//                                 .createdBy(decodeJwtService.getCurrentName())
//                                 .category(category) // Pasangkan kategorinya di sini
//                                 .build();

//                 // 4. Simpan ke Database
//                 productRepository.save(product);

//                 // 5. Kembalikan bentuk DTO murni ke Controller
//                 return ResponseProductDto.builder()
//                                 .id(product.getId())
//                                 .name(product.getName())
//                                 .price(product.getPrice())
//                                 .stock(product.getStock())
//                                 .categoryId(category.getId())
//                                 .build();
//         }

//         @Transactional(readOnly = true)
//         public Page<ResponseProductDto> getAllProductsPagedAndSearched(SearchProductRequestDto request) {
//                 validationService.validate(request);

//                 Specification<Product> specification = (root, query, criteriaBuilder) -> {
//                         List<Predicate> predicates = new ArrayList<>();

//                         if (request.getName() != null && !request.getName().isBlank()) {
//                                 predicates.add(criteriaBuilder.like(
//                                                 criteriaBuilder.lower(root.get("name")),
//                                                 "%" + request.getName().toLowerCase() + "%"));
//                         }

//                         if (request.getMinPrice() != null) {
//                                 predicates.add(criteriaBuilder.greaterThanOrEqualTo(
//                                                 root.get("price").as(BigDecimal.class), // 🌟 Ditambahkan
//                                                                                         // .as(BigDecimal.class)
//                                                 request.getMinPrice()));
//                         }

//                         // Filter 3: Perbaikan Harga Maksimal (price <= maxPrice)
//                         if (request.getMaxPrice() != null) {
//                                 predicates.add(criteriaBuilder.lessThanOrEqualTo(
//                                                 root.get("price").as(BigDecimal.class), // 🌟 Ditambahkan
//                                                                                         // .as(BigDecimal.class)
//                                                 request.getMaxPrice()));
//                         }

//                         if (request.getCategoryId() != null && !request.getCategoryId().isBlank()) {
//                                 predicates.add(criteriaBuilder.equal(root.get("category").get("id"),
//                                                 request.getCategoryId()));
//                         }

//                         return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
//                 };

//                 // 2. Set halaman dan ukuran data (Urut nama secara Alphabet)
//                 Pageable pageable = PageRequest.of(request.getPage(), request.getSize());

//                 // 3. Eksekusi pencarian dinamis ke MySQL
//                 Page<Product> productPage = productRepository.findAll(specification, pageable);

//                 // 4. Konversi ke bentuk DTO
//                 return productPage.map(product -> ResponseProductDto.builder()
//                                 .id(product.getId())
//                                 .name(product.getName())
//                                 .price(product.getPrice())
//                                 .stock(product.getStock())
//                                 .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
//                                 .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
//                                 .build());
//         }

//         @Transactional
//         public List<ResponseProductDto> createProductBulk(CreateBulkProductDto request) {
//                 validationService.validate(request);
//                 List<Product> productsToSave = new ArrayList<>();
//                 String currentUsername = decodeJwtService.getCurrentName();

//                 for (CreateProductDto dto : request.getProducts()) {
//                         Category category = categoryRepository.findById(dto.getCategoryId())
//                                         .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
//                                                         "Category not found"));
//                         Product product = Product.builder()
//                                         .id(UUID.randomUUID().toString())
//                                         .name(dto.getName())
//                                         .price(dto.getPrice())
//                                         .stock(dto.getStock())
//                                         .createdBy(currentUsername)
//                                         .category(category)
//                                         .build();
//                         productsToSave.add(product);
//                 }
//                 productRepository.saveAll(productsToSave);
//                 List<ResponseProductDto> responses = new ArrayList<>();
//                 for (Product product : productsToSave) {
//                         responses.add(ResponseProductDto.builder()
//                                         .id(product.getId())
//                                         .name(product.getName())
//                                         .price(product.getPrice())
//                                         .stock(product.getStock())
//                                         .categoryId(product.getCategory().getId())
//                                         .categoryName(product.getCategory().getName())
//                                         .build());
//                 }

//                 // 2. KIRIM KE RABBITMQ SETELAH COMMIT MYSQL SUKSES
//                 // 2. KIRIM KE RABBITMQ SETELAH COMMIT MYSQL SUKSES (Versi Lambda yang ringkas)
//                 // KUNCI: Pastikan baris ini benar-benar terpanggil
//                 TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//                         @Override
//                         public void afterCommit() {
//                                 System.out.println("LOG: Mencoba mengirim data ke RabbitMQ...");
//                                 rabbitTemplate.convertAndSend(RabbitMqConfig.PRODUCT_SYNC_QUEUE, responses);
//                                 System.out.println("LOG: Data terkirim!");
//                         }
//                 });
//                 return responses;
//         }

// }