package com.odde.donut.controllers.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserDTOTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
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
