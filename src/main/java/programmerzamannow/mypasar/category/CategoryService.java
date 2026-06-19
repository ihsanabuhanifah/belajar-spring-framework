package programmerzamannow.mypasar.category;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import programmerzamannow.mypasar.category.dto.CreateBulkCategoryDto;
import programmerzamannow.mypasar.category.dto.CreateCategoryDto;
import programmerzamannow.mypasar.category.dto.ResponseCategorydto;
import programmerzamannow.mypasar.category.entity.Category;
import programmerzamannow.mypasar.shared.jwt.DecodeJwtService;
import programmerzamannow.mypasar.shared.validation.ValidationService;

@Service
public class CategoryService {

    @Autowired
    private CategoryRespository categoryRepository;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private DecodeJwtService decodeJwtService;

    @Transactional
    public ResponseCategorydto createCategory(CreateCategoryDto request) {
        validationService.validate(request);
        Category category = new Category();
        category.setId(UUID.randomUUID().toString());
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setCreatedBy(decodeJwtService.getCurrentName());
        categoryRepository.save(category);

        return ResponseCategorydto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdBy(category.getCreatedBy())
                .build();
    }

    @Transactional
    public List<ResponseCategorydto> createCategoryBulk(CreateBulkCategoryDto request) {
        validationService.validate(request);

        List<Category> categoriesToSave = new ArrayList<>();
        String currentUsername = decodeJwtService.getCurrentName();

        // 2. Lakukan perulangan untuk mengubah DTO menjadi Entity
        for (CreateCategoryDto dto : request.getCategories()) {
            Category category = new Category();
            category.setId(UUID.randomUUID().toString());
            category.setName(dto.getName());
            category.setDescription(dto.getDescription());
            category.setCreatedBy(currentUsername);

            categoriesToSave.add(category);
        }
        categoryRepository.saveAll(categoriesToSave);
        List<ResponseCategorydto> responses = new ArrayList<>();

        for (Category category : categoriesToSave) {
            responses.add(ResponseCategorydto.builder()
                    .id(category.getId())
                    .name(category.getName())
                    .description(category.getDescription())
                    .createdBy(category.getCreatedBy())
                    .build());
        }

        return responses;
    }

}
