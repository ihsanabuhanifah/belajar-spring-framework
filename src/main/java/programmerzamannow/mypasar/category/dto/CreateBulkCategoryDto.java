package programmerzamannow.mypasar.category.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateBulkCategoryDto {

    @NotEmpty(message = "Categories list cannot be empty")
    @Valid // 🌟 SAKTI: Memaksa Spring untuk tetap memvalidasi anotasi @NotBlank di dalam
           // tiap item list
    private List<CreateCategoryDto> categories;
}