package at.technikum.swen3.gemini;

import at.technikum.swen3.gemini.config.GeminiProperties;
import at.technikum.swen3.gemini.config.WorkerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({GeminiProperties.class, WorkerProperties.class})
public class GeminiApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(GeminiApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}
