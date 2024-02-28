package com.odde.doughnut.controllers.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class UserDTOTest {

  @Autowired MakeMe makeMe;
  @Autowired private Validator validator;
  UserDTO userDTO = new UserDTO();

  @BeforeEach
  void setup() {
    User user = makeMe.aUser().please();
    userDTO.setName(user.getName());
    userDTO.setSpaceIntervals(user.getSpaceIntervals());
  }

  @Test
  void validate() {
    Set<ConstraintViolation<UserDTO>> violations = validator.validate(userDTO);
    assertEquals(0, violations.size());
  }

  @Test
  void validateName() {
    userDTO.setName("");
    Set<ConstraintViolation<UserDTO>> violations = validator.validate(userDTO);
    assertEquals(1, violations.size());
  }

  @Test
  void validateSpacing() {
    userDTO.setSpaceIntervals("1,2a");
    Set<ConstraintViolation<UserDTO>> violations = validator.validate(userDTO);
    assertEquals(1, violations.size());
  }

  @Test
  void validateSpacingValid() {
    userDTO.setSpaceIntervals("1,2,33, 444");
    Set<ConstraintViolation<UserDTO>> violations = validator.validate(userDTO);
    assertEquals(0, violations.size());
  }
}
