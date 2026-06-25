package programmerzamannow.mypasar.product.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import programmerzamannow.mypasar.product.dto.ResponseProductDto;
import programmerzamannow.mypasar.product.repository.ProductElasticsearchRepository;
import programmerzamannow.mypasar.product.entity.ProductDocument; // Sesuaikan dengan nama entity Elasticsearch Ama
import programmerzamannow.mypasar.shared.config.RabbitMqConfig;

import java.util.List;

@Component
@Slf4j
public class ProductSyncConsumer {

    private final ProductElasticsearchRepository productElasticsearchRepository;

    // Constructor Injection agar Repository-nya bisa dipakai
    public ProductSyncConsumer(ProductElasticsearchRepository productElasticsearchRepository) {
        this.productElasticsearchRepository = productElasticsearchRepository;
    }

    @RabbitListener(queues = RabbitMqConfig.PRODUCT_SYNC_QUEUE)
    public void listenProductSync(List<ResponseProductDto> responses) {
        log.info("LOG: Menerima pesan sync dari RabbitMQ sebanyak: {} item", responses.size());
        try {
            List<ProductDocument> elasticProducts = responses.stream().map(dto -> {
                return ProductDocument.builder()
                        .id(dto.getId())
                        .name(dto.getName())
                        .price(dto.getPrice())
                        .stock(dto.getStock())
                        .categoryId(dto.getCategoryId())
                        .build();
            }).toList();

            productElasticsearchRepository.saveAll(elasticProducts);
            System.out.println("LOG: Berhasil menyisipkan data bulk ke indeks idx_products!");

        } catch (Exception e) {
            log.error("LOG: GAGAL memproses sinkronisasi data karena: {}", e.getMessage(), e);
            // Melempar AmqpRejectAndDontRequeueException agar antrean Unacked di RabbitMQ
            // otomatis dibersihkan (di-reject) dan tidak macet
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("Gagal konversi dokumen", e);
        }
    }
}