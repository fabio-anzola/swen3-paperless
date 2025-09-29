package at.technikum.swen3;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

@SpringBootApplication
public class Swen3Application {
	public static void main(String[] args) {
		SpringApplication.run(Swen3Application.class, args);
	}

	/**
	 * Bean for printing all Spring Beans loaded in the application context.
	 *
	 * @param ctx the Spring application context
	 * @return CommandLineRunner to be executed at startup
	 */
	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {

			System.out.println("Beans provided by Spring Boot:");

			String[] beanNames = ctx.getBeanDefinitionNames();
			Arrays.sort(beanNames);
			for (String beanName : beanNames) {
				System.out.println(beanName);
			}

		};
	}
}