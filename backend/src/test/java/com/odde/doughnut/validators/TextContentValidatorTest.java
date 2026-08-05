package com.odde.doughnut.validators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.MakeMe;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TextContentValidatorTest {

  @Autowired MakeMe makeMe;
  @Autowired Validator validator;

  @Test
  void defaultNoteIsValid() {
    assertThat(getViolations(makeMe.aNote().inMemoryPlease()), is(empty()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void blankTitleIsInvalid(String title) {
    assertThat(getViolations(makeMe.aNote().title(title).inMemoryPlease()), is(not(empty())));
  }

  @Test
  void titleCannotBeTooLong() {
    assertThat(
        getViolations(makeMe.aNote().title(makeMe.aStringOfLength(151)).inMemoryPlease()),
        is(not(empty())));
  }

  private Set<ConstraintViolation<Note>> getViolations(Note note) {
    return validator.validate(note);
  }
}
