package com.odde.doughnut.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
public class UserTest {

  @Autowired MakeMe makeMe;
  @Autowired private Validator validator;
  User user;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
  }

  @Test
  void validate() {
    Set<ConstraintViolation<User>> violations = validator.validate(user);
    assertEquals(0, violations.size());
  }

  @Test
  void validateName() {
    user.setName("");
    Set<ConstraintViolation<User>> violations = validator.validate(user);
    assertEquals(1, violations.size());
  }

  @Test
  void validateSpacing() {
    user.setSpaceIntervals("1,2a");
    Set<ConstraintViolation<User>> violations = validator.validate(user);
    assertEquals(1, violations.size());
  }

  @Test
  void validateSpacingValid() {
    user.setSpaceIntervals("1,2,33, 444");
    Set<ConstraintViolation<User>> violations = validator.validate(user);
    assertEquals(0, violations.size());
  }
}
