package com.odde.doughnut.controllers.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserDTOTest {

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
  void validDefaults() {
    assertThat(validator.validate(userDTO), empty());
  }

  @Test
  void rejectsBlankName() {
    userDTO.setName("");
    assertThat(validator.validate(userDTO), hasSize(1));
  }

  @ParameterizedTest
  @CsvSource({"'1,2a', 1", "'1,2,33, 444', 0"})
  void spaceIntervalsValidation(String intervals, int violationCount) {
    userDTO.setSpaceIntervals(intervals);
    Set<ConstraintViolation<UserDTO>> violations = validator.validate(userDTO);
    assertThat(violations, hasSize(violationCount));
  }
}
