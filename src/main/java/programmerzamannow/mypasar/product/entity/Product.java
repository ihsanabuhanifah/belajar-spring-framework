package programmerzamannow.mypasar.product.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;
import programmerzamannow.mypasar.category.entity.Category;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private Category category;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "stock", nullable = false)
    private Integer stock;

    // ⚔️ TAHAP 4: Optimistic Locking anti rebutan stok barang Flash Sale
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_by", length = 100, nullable = false)
    private String createdBy;

    @Column(name = "updated_by", length = 100, nullable = true)
    private String updatedBy;
}