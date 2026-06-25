package programmerzamannow.mypasar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
		ElasticsearchClientAutoConfiguration.class
})
@EnableScheduling
public class BelajarSpringRestfullApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BelajarSpringRestfullApiApplication.class, args);
	}

}
