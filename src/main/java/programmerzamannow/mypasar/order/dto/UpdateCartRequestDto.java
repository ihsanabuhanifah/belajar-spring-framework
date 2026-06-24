
package programmerzamannow.mypasar.order.dto;

import java.util.List;

public class UpdateCartRequestDto {
    private List<OrderItemDto> items;

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }
}