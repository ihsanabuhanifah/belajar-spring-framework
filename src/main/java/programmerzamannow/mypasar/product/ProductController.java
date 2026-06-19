package programmerzamannow.mypasar.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import programmerzamannow.mypasar.product.dto.CreateProductDto;
import programmerzamannow.mypasar.product.dto.ResponseProductDto;
import programmerzamannow.mypasar.shared.response.WebResponse;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping(value = "create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<ResponseProductDto> create(@RequestBody CreateProductDto request) {
        ResponseProductDto response = productService.createProduct(request);
        return WebResponse.<ResponseProductDto>builder().data(response).build();
    }
}