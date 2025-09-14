package at.technikum.swen3.endpoint;

import at.technikum.swen3.dto.user.UserCreateDto;
import at.technikum.swen3.dto.user.UserDto;
import at.technikum.swen3.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;

@RestController
@RequestMapping(value = "/api/v1/user")
public class UserEndpoint {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final UserService userService;

  @Autowired
  public UserEndpoint(UserService userService) {
    this.userService = userService;
  }

  @PostMapping
  public UserDto createEvent(@RequestBody UserCreateDto user) {

  }
}
