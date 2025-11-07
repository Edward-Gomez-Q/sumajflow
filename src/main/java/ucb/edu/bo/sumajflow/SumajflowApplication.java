package ucb.edu.bo.sumajflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "ucb.edu.bo.sumajflow.repository")
@EnableJpaAuditing
public class SumajflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(SumajflowApplication.class, args);
	}

}
