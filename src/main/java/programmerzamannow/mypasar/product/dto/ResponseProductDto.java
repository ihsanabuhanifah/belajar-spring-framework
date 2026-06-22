package programmerzamannow.mypasar.product.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseProductDto {
    private String id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private String categoryId;
    private String categoryName;
}