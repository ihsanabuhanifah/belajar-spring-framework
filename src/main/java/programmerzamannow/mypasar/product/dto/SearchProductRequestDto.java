package programmerzamannow.mypasar.product.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchProductRequestDto {

    private String name; // 🌟 BARU: Untuk menampung kata kunci pencarian (Optional)
    private BigDecimal minPrice; // 🌟 BARU: Filter harga minimal (Optional)
    private BigDecimal maxPrice; // 🌟 BARU: Filter harga maksimal (Optional)
    private String categoryId;
    private String keyword; // 🌟 BARU: Untuk menampung kata kunci pencarian (Optional) maka mencari dari
                            // semua field

    @NotNull(message = "Page number is required")
    @Min(value = 0, message = "Page page must be at least 0")
    private Integer page;

    @NotNull(message = "Size is required")
    @Min(value = 1, message = "Size must be at least 1")
    private Integer size;
}