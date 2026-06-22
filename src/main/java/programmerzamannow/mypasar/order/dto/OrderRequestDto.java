package programmerzamannow.mypasar.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderRequestDto {

    @NotEmpty(message = "Items list cannot be empty")
    @Valid // 🌟 SAKTI: Berfungsi memaksa Spring Boot ikut memvalidasi isi 'quantity' di
           // dalam list item
    private List<OrderItemDto> items; // Menampung daftar list barang belanjaan (keranjang)
}