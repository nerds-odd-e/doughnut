package com.odde.doughnut.validators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.MakeMe;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TextContentValidatorTest {

  @Autowired MakeMe makeMe;

  private Validator validator;
  private Note newNote;

  @BeforeEach
  public void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    newNote = makeMe.aNote().inMemoryPlease();
    validator = factory.getValidator();
  }

  @Test
  public void defaultNoteFromMakeMeIsValidate() {
    assertThat(getViolations(), is(empty()));
  }

  @Test
  public void titleIsNotOptional() {
    newNote.setTitle("");
    assertThat(getViolations(), is(not(empty())));
  }

  @Test
  public void titleCannotBeTooLong() {
    newNote.setTitle(makeMe.aStringOfLength(151));
    assertThat(getViolations(), is(not(empty())));
  }

  private Set<ConstraintViolation<Note>> getViolations() {
    return validator.validate(newNote);
  }
}
