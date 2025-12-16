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

  @Test
  public void noteWithTargetNoteAndNullTitleIsValid() {
    Note parent = makeMe.aNote().inMemoryPlease();
    Note target = makeMe.aNote().inMemoryPlease();
    Note relationNote = makeMe.aRelation().between(parent, target).inMemoryPlease();
    relationNote.setTitle(null);
    assertThat(validator.validate(relationNote), is(empty()));
  }

  @Test
  public void noteWithTargetNoteAndEmptyTitleIsInvalid() {
    Note parent = makeMe.aNote().inMemoryPlease();
    Note target = makeMe.aNote().inMemoryPlease();
    Note relationNote = makeMe.aRelation().between(parent, target).inMemoryPlease();
    relationNote.setTitle("");
    Set<ConstraintViolation<Note>> violations = validator.validate(relationNote);
    assertThat(violations, is(not(empty())));
    assertThat(
        violations.stream()
            .anyMatch(
                v ->
                    v.getMessage().equals("Note with targetNote must have null title")
                        && v.getPropertyPath().toString().equals("title")),
        is(true));
  }

  @Test
  public void noteWithTargetNoteAndNonEmptyTitleIsInvalid() {
    Note parent = makeMe.aNote().inMemoryPlease();
    Note target = makeMe.aNote().inMemoryPlease();
    Note relationNote = makeMe.aRelation().between(parent, target).inMemoryPlease();
    relationNote.setTitle("Some Title");
    Set<ConstraintViolation<Note>> violations = validator.validate(relationNote);
    assertThat(violations, is(not(empty())));
    assertThat(
        violations.stream()
            .anyMatch(
                v ->
                    v.getMessage().equals("Note with targetNote must have null title")
                        && v.getPropertyPath().toString().equals("title")),
        is(true));
  }

  @Test
  public void noteWithoutTargetNoteCanHaveTitle() {
    newNote.setTitle("Valid Title");
    assertThat(getViolations(), is(empty()));
  }

  private Set<ConstraintViolation<Note>> getViolations() {
    return validator.validate(newNote);
  }
}
