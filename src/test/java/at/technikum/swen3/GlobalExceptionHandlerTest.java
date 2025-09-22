package at.technikum.swen3;

import at.technikum.swen3.endpoint.exceptionhandler.GlobalExceptionHandler;
import at.technikum.swen3.exception.UserCreationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TestController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    @RestController
    static class TestController {
        @GetMapping("/test/user-creation")
        public void throwUserCreationException() {
            throw new UserCreationException("User creation failed", null);
        }

        @GetMapping("/test/unhandled")
        public void throwUnhandledException() {
            throw new RuntimeException("Some unexpected error");
        }
    }

    @Test
    void givenUserCreationException_whenGet_thenStatus422() throws Exception {
        mvc.perform(get("/test/user-creation")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.error", is("UNPROCESSABLE_ENTITY")))
                .andExpect(jsonPath("$.message", is("User creation failed")))
                .andExpect(jsonPath("$.path", is("/test/user-creation")));
    }

    @Test
    void givenUnhandledException_whenGet_thenStatus500() throws Exception {
        mvc.perform(get("/test/unhandled")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("INTERNAL_SERVER_ERROR")))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred.")))
                .andExpect(jsonPath("$.path", is("/test/unhandled")));
    }
}