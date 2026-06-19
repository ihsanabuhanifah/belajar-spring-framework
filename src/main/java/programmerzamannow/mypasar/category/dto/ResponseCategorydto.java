package programmerzamannow.mypasar.category.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseCategorydto {

    private String id;
    private String name;
    private String description;
    private String createdBy;
}
