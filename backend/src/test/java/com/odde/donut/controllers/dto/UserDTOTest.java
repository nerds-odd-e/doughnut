package com.odde.donut.controllers.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserDTOTest {

  @Autowired private Validator validator;
  UserDTO userDTO = new UserDTO();

  @BeforeEach
  void setup() {
    userDTO.setName("a name");
  }

  @Test
  void validDefaults() {
    assertThat(validator.validate(userDTO), empty());
  }

  @Test
  void rejectsBlankName() {
    userDTO.setName("");
    assertThat(validator.validate(userDTO), hasSize(1));
  }
}
