package programmerzamannow.mypasar.product;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import programmerzamannow.mypasar.product.dto.CreateBulkProductDto;
import programmerzamannow.mypasar.product.dto.CreateProductDto;
import programmerzamannow.mypasar.product.dto.ResponseProductDto;
import programmerzamannow.mypasar.product.dto.SearchProductRequestDto;
import programmerzamannow.mypasar.product.service.ProductService;
import programmerzamannow.mypasar.shared.response.PagingResponse;
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

        @GetMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
        public WebResponse<List<ResponseProductDto>> list(
                        @RequestParam(name = "name", required = false) String name,
                        @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
                        @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
                        @RequestParam(name = "categoryId", required = false) String categoryId,

                        @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
                        @RequestParam(name = "size", required = false, defaultValue = "10") Integer size) {
                SearchProductRequestDto request = SearchProductRequestDto.builder()
                                .name(name)
                                .minPrice(minPrice)
                                .maxPrice(maxPrice)
                                .categoryId(categoryId)
                                .page(page)
                                .size(size)
                                .build();

                Page<ResponseProductDto> productPage = productService.getAllProductsPagedAndSearched(request);

                return WebResponse.<List<ResponseProductDto>>builder()
                                .data(productPage.getContent())
                                .paging(PagingResponse.builder()
                                                .currentPage(productPage.getNumber())
                                                .totalPages(productPage.getTotalPages())
                                                .totalElements(productPage.getTotalElements())
                                                .size(productPage.getSize())
                                                .build())
                                .build();
        }

        @PostMapping(value = "createBulk", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
        public WebResponse<List<ResponseProductDto>> createBulk(@RequestBody CreateBulkProductDto request) {
                List<ResponseProductDto> responses = productService.createProductBulk(request);
                return WebResponse.<List<ResponseProductDto>>builder().data(responses).build();
        }
}