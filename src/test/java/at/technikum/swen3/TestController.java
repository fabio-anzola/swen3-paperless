package at.technikum.swen3;

import at.technikum.swen3.exception.UserCreationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/test/user-creation")
    public void throwUserCreationException() {
        throw new UserCreationException("User creation failed", null);
    }

    @GetMapping("/test/unhandled")
    public void throwUnhandledException() {
        throw new RuntimeException("Some unexpected error");
    }
}