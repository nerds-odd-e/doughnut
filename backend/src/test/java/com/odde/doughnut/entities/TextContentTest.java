package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.doughnut.testability.MakeMe;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TextContentTest {

  @Autowired MakeMe makeMe;

  @Nested
  class ValidationTest {
    private Validator validator;
    private final Note newNote = makeMe.aNote().inMemoryPlease();

    @BeforeEach
    public void setUp() {
      ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
      validator = factory.getValidator();
    }

    @Test
    public void defaultNoteFromMakeMeIsValidate() {
      assertThat(getViolations(), is(empty()));
    }

    @Test
    public void titleIsNotOptional() {
      newNote.setTopic("");
      assertThat(getViolations(), is(not(empty())));
    }

    @Test
    public void titleCannotBeTooLong() {
      newNote.setTopic(makeMe.aStringOfLength(151));
      assertThat(getViolations(), is(not(empty())));
    }

    private Set<ConstraintViolation<Note>> getViolations() {
      return validator.validate(newNote);
    }
  }
}
