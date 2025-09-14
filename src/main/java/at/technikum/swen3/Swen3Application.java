package at.technikum.swen3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "at.technikum.swen3")
@EnableJpaRepositories(basePackages = "at.technikum.swen3.repository")
@EntityScan(basePackages = "at.technikum.swen3.entity")
public class Swen3Application {
	public static void main(String[] args) {
		SpringApplication.run(Swen3Application.class, args);
	}
}