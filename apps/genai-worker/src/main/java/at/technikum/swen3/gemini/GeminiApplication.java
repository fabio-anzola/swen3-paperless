package at.technikum.swen3.gemini;

import at.technikum.swen3.gemini.config.GeminiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiApplication {

    public static void main(String[] args) {
        SpringApplication.run(GeminiApplication.class, args);
    }
}
