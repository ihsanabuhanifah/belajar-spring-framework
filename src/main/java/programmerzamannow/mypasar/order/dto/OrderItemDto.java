package programmerzamannow.mypasar.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderItemDto {

    @NotBlank(message = "Product ID is required")
    private String productId; // Menampung ID produk yang mau dibeli

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity; // Menampung jumlah barang yang dibeli (minimal 1)
}