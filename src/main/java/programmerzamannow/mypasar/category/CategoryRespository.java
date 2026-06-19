package programmerzamannow.mypasar.category;

import org.springframework.data.jpa.repository.JpaRepository;
import programmerzamannow.mypasar.category.entity.Category;

public interface CategoryRespository extends JpaRepository<Category, String> {

}
