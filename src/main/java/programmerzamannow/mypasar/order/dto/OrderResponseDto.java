package programmerzamannow.mypasar.order.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponseDto {
    private String orderId;
    private String username;
    private Long orderDate;
    private BigDecimal totalAmount;
    private String status;
    private List<OrderDetailResponseDto> items;
}