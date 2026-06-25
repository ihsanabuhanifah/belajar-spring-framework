package programmerzamannow.mypasar.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import programmerzamannow.mypasar.order.dto.OrderRequestDto;
import programmerzamannow.mypasar.order.dto.OrderResponseDto;
import programmerzamannow.mypasar.order.dto.UpdateCartRequestDto;
import programmerzamannow.mypasar.shared.jwt.DecodeJwtService;
import programmerzamannow.mypasar.shared.response.WebResponse;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private DecodeJwtService decodeJwtService;

    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<OrderResponseDto> checkout(@RequestBody OrderRequestDto request) {
        // 🌟 Sementara kita hardcode dulu nama usernya 'ama_developer'
        // Nanti kalau auth JWT Ama sudah siap, ini tinggal diganti dengan user yang
        // login aktif.

        String currentUsername = decodeJwtService.getCurrentName();

        OrderResponseDto response = orderService.createOrder(currentUsername, request);
        return WebResponse.<OrderResponseDto>builder().data(response).build();
    }

    @GetMapping(value = "/{orderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<OrderResponseDto> get(@PathVariable(name = "orderId") String orderId) {
        OrderResponseDto response = orderService.getOrderDetail(orderId);
        return WebResponse.<OrderResponseDto>builder().data(response).build();
    }

    @PutMapping(path = "/{orderId}/cart", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponseDto batchUpdateQuantities(
            @PathVariable("orderId") String orderId,
            @RequestBody UpdateCartRequestDto request) {

        String currentUsername = decodeJwtService.getCurrentName();
        return orderService.batchUpdateCartQuantities(currentUsername, orderId, request);
    }

    @PostMapping(path = "/{orderId}/checkout", produces = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponseDto checkout(
            @PathVariable("orderId") String orderId) {
        String currentUsername = decodeJwtService.getCurrentName();
        return orderService.checkoutOrder(currentUsername, orderId);
    }
}