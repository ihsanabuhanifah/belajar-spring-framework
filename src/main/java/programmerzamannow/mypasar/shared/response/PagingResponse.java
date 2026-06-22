package programmerzamannow.mypasar.shared.response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PagingResponse {
    private Integer currentPage;
    private Integer totalPages;
    private Long totalElements;
    private Integer size;
}