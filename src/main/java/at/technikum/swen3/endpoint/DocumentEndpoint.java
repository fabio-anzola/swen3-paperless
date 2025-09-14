package at.technikum.swen3.endpoint;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = DocumentEndpoint.BASE_PATH)
public class DocumentEndpoint {
  static final String BASE_PATH = "/api/v1/documents";


}
