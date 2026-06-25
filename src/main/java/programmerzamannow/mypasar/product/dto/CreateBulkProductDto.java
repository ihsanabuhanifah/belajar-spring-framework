
package programmerzamannow.mypasar.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class CreateBulkProductDto {

    @NotEmpty(message = "Product list cannot be empty")
    @Valid
    private List<CreateProductDto> products;

}
