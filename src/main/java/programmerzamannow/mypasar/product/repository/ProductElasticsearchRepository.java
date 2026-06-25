package programmerzamannow.mypasar.product.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import org.springframework.stereotype.Component;
import programmerzamannow.mypasar.product.entity.ProductDocument;

import java.io.IOException;
import java.util.List;

@Component
public class ProductElasticsearchRepository {

    private final ElasticsearchClient elasticsearchClient;

    public ProductElasticsearchRepository(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    // Menggantikan fungsi saveAll bawaan menggunakan Bulk API murni yang jauh lebih
    // aman
    public void saveAll(List<ProductDocument> documents) {
        try {
            var bulkBuilder = new co.elastic.clients.elasticsearch.core.BulkRequest.Builder();

            for (ProductDocument doc : documents) {
                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index("idx_products")
                                .id(doc.getId())
                                .document(doc)));
            }

            elasticsearchClient.bulk(bulkBuilder.build());
        } catch (IOException e) {
            throw new RuntimeException("Gagal menyimpan bulk data ke Elasticsearch", e);
        }
    }
}