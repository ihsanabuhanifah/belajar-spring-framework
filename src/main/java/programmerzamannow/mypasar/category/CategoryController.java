package programmerzamannow.mypasar.category;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import programmerzamannow.mypasar.category.dto.CreateBulkCategoryDto;
import programmerzamannow.mypasar.category.dto.CreateCategoryDto;
import programmerzamannow.mypasar.category.dto.ResponseCategorydto;
import programmerzamannow.mypasar.shared.response.WebResponse;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @PostMapping(value = "create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<ResponseCategorydto> create(@Valid @RequestBody CreateCategoryDto request) {
        ResponseCategorydto responseCategorydto = categoryService.createCategory(request);
        return WebResponse.<ResponseCategorydto>builder()
                .data(responseCategorydto)
                .build();
    }

    @PostMapping(value = "createBulk", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<List<ResponseCategorydto>> createBulk(@RequestBody CreateBulkCategoryDto request) {
        List<ResponseCategorydto> response = categoryService.createCategoryBulk(request);
        return WebResponse.<List<ResponseCategorydto>>builder().data(response).build();
    }

}
