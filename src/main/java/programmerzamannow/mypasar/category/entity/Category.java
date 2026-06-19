package programmerzamannow.mypasar.category.entity;

import jakarta.persistence.*;
import lombok.*;
import programmerzamannow.mypasar.product.entity.Product;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by", length = 100, nullable = false)
    private String createdBy;

    @OneToMany(mappedBy = "category", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private List<Product> products;
}