package programmerzamannow.mypasar.product.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import programmerzamannow.mypasar.category.CategoryRespository;
import programmerzamannow.mypasar.category.entity.Category;
import programmerzamannow.mypasar.product.dto.CreateBulkProductDto;
import programmerzamannow.mypasar.product.dto.CreateProductDto;
import programmerzamannow.mypasar.product.dto.ResponseProductDto;
import programmerzamannow.mypasar.product.dto.SearchProductRequestDto;
import programmerzamannow.mypasar.product.entity.Product;
import programmerzamannow.mypasar.product.entity.ProductDocument;
import programmerzamannow.mypasar.product.repository.ProductRepository;
import programmerzamannow.mypasar.shared.config.RabbitMqConfig;
import programmerzamannow.mypasar.shared.jwt.DecodeJwtService;
import programmerzamannow.mypasar.shared.validation.ValidationService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
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

        @Autowired
        private RabbitTemplate rabbitTemplate;

        @Autowired
        private ElasticsearchClient elasticsearchClient;

        // =========================================================
        // CREATE SINGLE PRODUCT
        // =========================================================
        @Transactional
        public ResponseProductDto createProduct(CreateProductDto request) {
                validationService.validate(request);

                Category category = categoryRepository.findById(request.getCategoryId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Category not found"));

                Product product = Product.builder()
                                .id(UUID.randomUUID().toString())
                                .name(request.getName())
                                .price(request.getPrice())
                                .stock(request.getStock())
                                .createdBy(decodeJwtService.getCurrentName())
                                .category(category)
                                .build();

                productRepository.save(product);

                return ResponseProductDto.builder()
                                .id(product.getId())
                                .name(product.getName())
                                .price(product.getPrice())
                                .stock(product.getStock())
                                .categoryId(category.getId())
                                .build();
        }

        // =========================================================
        // CREATE BULK PRODUCT + KIRIM KE RABBITMQ
        // =========================================================
        @Transactional
        public List<ResponseProductDto> createProductBulk(CreateBulkProductDto request) {
                validationService.validate(request);
                List<Product> productsToSave = new ArrayList<>();
                String currentUsername = decodeJwtService.getCurrentName();

                for (CreateProductDto dto : request.getProducts()) {
                        Category category = categoryRepository.findById(dto.getCategoryId())
                                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                        "Category not found"));

                        Product product = Product.builder()
                                        .id(UUID.randomUUID().toString())
                                        .name(dto.getName())
                                        .price(dto.getPrice())
                                        .stock(dto.getStock())
                                        .createdBy(currentUsername)
                                        .category(category)
                                        .build();

                        productsToSave.add(product);
                }

                productRepository.saveAll(productsToSave);

                List<ResponseProductDto> responses = new ArrayList<>();
                for (Product product : productsToSave) {
                        responses.add(ResponseProductDto.builder()
                                        .id(product.getId())
                                        .name(product.getName())
                                        .price(product.getPrice())
                                        .stock(product.getStock())
                                        .categoryId(product.getCategory().getId())
                                        .categoryName(product.getCategory().getName())
                                        .build());
                }

                // Kirim ke RabbitMQ setelah commit MySQL sukses
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                                log.info("LOG: Mencoba mengirim data ke RabbitMQ...");
                                rabbitTemplate.convertAndSend(RabbitMqConfig.PRODUCT_SYNC_QUEUE, responses);
                                log.info("LOG: Data terkirim ke RabbitMQ!");
                        }
                });

                return responses;
        }

        // =========================================================
        // SEARCH PRODUCT — pakai ES jika ada nama/keyword, MySQL jika tidak
        // =========================================================
        @Transactional(readOnly = true)
        public Page<ResponseProductDto> getAllProductsPagedAndSearched(SearchProductRequestDto request) {
                validationService.validate(request);

                log.info("Menerima request: {}", request);

                boolean useElastic = (request.getName() != null && !request.getName().isBlank())
                                || (request.getKeyword() != null && !request.getKeyword().isBlank());

                if (useElastic) {
                        log.info("Elasticsearch digunakan");
                        return searchFromElasticsearch(request);
                }

                log.info("MySQL digunakan");
                return searchFromMySQL(request);
        }

        // =========================================================
        // PRIVATE — Pencarian via Elasticsearch
        // =========================================================
        private Page<ResponseProductDto> searchFromElasticsearch(SearchProductRequestDto request) {
                try {
                        var queryBuilder = new BoolQuery.Builder();

                        // Cari berdasarkan nama (pakai analyzer indo_cleaner)
                        if (request.getName() != null && !request.getName().isBlank()) {
                                queryBuilder.must(m -> m
                                                .match(mq -> mq
                                                                .field("name")
                                                                .query(request.getName())));
                        }

                        // Cari keyword di semua field
                        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                                queryBuilder.must(m -> m
                                                .multiMatch(mm -> mm
                                                                .fields("name", "categoryId")
                                                                .query(request.getKeyword())
                                                                .type(TextQueryType.BestFields)));
                        }

                        // Filter harga minimum
                        // Filter harga minimum
                        if (request.getMinPrice() != null) {
                                final double minPrice = request.getMinPrice().doubleValue();
                                queryBuilder.filter(f -> f
                                                .range(r -> r
                                                                .field("price")
                                                                .gte(JsonData.of(minPrice))));
                        }

                        // Filter harga maksimum
                        if (request.getMaxPrice() != null) {
                                final double maxPrice = request.getMaxPrice().doubleValue();
                                queryBuilder.filter(f -> f
                                                .range(r -> r
                                                                .field("price")
                                                                .lte(JsonData.of(maxPrice))));
                        }

                        // Filter kategori
                        if (request.getCategoryId() != null && !request.getCategoryId().isBlank()) {
                                queryBuilder.filter(f -> f
                                                .term(t -> t
                                                                .field("categoryId")
                                                                .value(request.getCategoryId())));
                        }

                        int from = request.getPage() * request.getSize();

                        SearchRequest searchRequest = SearchRequest.of(s -> s
                                        .index("idx_products")
                                        .query(q -> q.bool(queryBuilder.build()))
                                        .from(from)
                                        .size(request.getSize()));

                        SearchResponse<ProductDocument> response = elasticsearchClient.search(
                                        searchRequest, ProductDocument.class);

                        List<ResponseProductDto> results = response.hits().hits().stream()
                                        .map(hit -> {
                                                ProductDocument doc = hit.source();
                                                if (doc == null)
                                                        return null;
                                                return ResponseProductDto.builder()
                                                                .id(doc.getId())
                                                                .name(doc.getName())
                                                                .price(doc.getPrice())
                                                                .stock(doc.getStock())
                                                                .categoryId(doc.getCategoryId())
                                                                .build();
                                        })
                                        .filter(dto -> dto != null)
                                        .toList();

                        long totalHits = response.hits().total() != null
                                        ? response.hits().total().value()
                                        : 0;

                        log.info("Elasticsearch mengembalikan {} hasil dari total {}", results.size(), totalHits);

                        return new PageImpl<>(
                                        results,
                                        PageRequest.of(request.getPage(), request.getSize()),
                                        totalHits);

                } catch (IOException e) {
                        log.warn("Elasticsearch gagal, fallback ke MySQL. Penyebab: {}", e.getMessage());
                        return searchFromMySQL(request);
                }
        }

        // =========================================================
        // PRIVATE — Pencarian via MySQL (fallback)
        // =========================================================
        private Page<ResponseProductDto> searchFromMySQL(SearchProductRequestDto request) {
                Specification<Product> specification = (root, query, criteriaBuilder) -> {
                        List<Predicate> predicates = new ArrayList<>();

                        if (request.getName() != null && !request.getName().isBlank()) {
                                predicates.add(criteriaBuilder.like(
                                                criteriaBuilder.lower(root.get("name")),
                                                "%" + request.getName().toLowerCase() + "%"));
                        }

                        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                                predicates.add(criteriaBuilder.like(
                                                criteriaBuilder.lower(root.get("name")),
                                                "%" + request.getKeyword().toLowerCase() + "%"));
                        }

                        if (request.getMinPrice() != null) {
                                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                                root.get("price").as(BigDecimal.class),
                                                request.getMinPrice()));
                        }

                        if (request.getMaxPrice() != null) {
                                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                                root.get("price").as(BigDecimal.class),
                                                request.getMaxPrice()));
                        }

                        if (request.getCategoryId() != null && !request.getCategoryId().isBlank()) {
                                predicates.add(criteriaBuilder.equal(
                                                root.get("category").get("id"),
                                                request.getCategoryId()));
                        }

                        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
                };

                Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
                Page<Product> productPage = productRepository.findAll(specification, pageable);

                return productPage.map(product -> ResponseProductDto.builder()
                                .id(product.getId())
                                .name(product.getName())
                                .price(product.getPrice())
                                .stock(product.getStock())
                                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                                .build());
        }

        @Transactional(readOnly = true)
        public Page<ResponseProductDto> getAllProductsPagedAndSearchedMySQL(SearchProductRequestDto request) {
                validationService.validate(request);
                return searchFromMySQL(request);
        }

}