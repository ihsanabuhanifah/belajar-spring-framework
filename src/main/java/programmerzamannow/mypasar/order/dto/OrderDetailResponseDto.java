package programmerzamannow.mypasar.order.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderDetailResponseDto {
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal price; // Harga pengunci saat dibeli
}