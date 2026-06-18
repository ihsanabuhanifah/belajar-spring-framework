package programmerzamannow.HRIS.shared.response;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class WebResponse<T> {

    private T data;
    private String errors;
    private PagingResponse paging;

}
