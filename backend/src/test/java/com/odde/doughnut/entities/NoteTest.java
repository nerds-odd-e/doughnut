package com.odde.doughnut.entities;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.testability.MakeMe;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
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
public class NoteTest {

  @Autowired MakeMe makeMe;
  User user;

  @Test
  void timeOrder() {
    Note parent = makeMe.aNote().please();
    Note note1 = makeMe.aNote().under(parent).please();
    Note note2 = makeMe.aNote().under(parent).please();
    makeMe.flush();
    makeMe.refresh(parent);
    assertThat(parent.getChildren(), containsInRelativeOrder(note1, note2));
  }

  @Nested
  class Picture {

    @Test
    void useParentPicture() {
      Note parent = makeMe.aNote().pictureUrl("https://img.com/xxx.jpg").inMemoryPlease();
      Note child = makeMe.aNote().under(parent).useParentPicture().inMemoryPlease();
      assertThat(
          child.getPictureWithMask().get().notePicture,
          equalTo(parent.getNoteAccessories().getPictureUrl()));
    }

    @Test
    void useParentPictureWhenTheUrlIsEmptyString() {
      Note parent = makeMe.aNote().pictureUrl("").inMemoryPlease();
      Note child = makeMe.aNote().under(parent).useParentPicture().inMemoryPlease();
      assertTrue(child.getPictureWithMask().isEmpty());
    }
  }

  @Nested
  class ValidationTest {
    private Validator validator;
    private final Note note = makeMe.aNote().inMemoryPlease();

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
    public void goodMask() {
      note.getNoteAccessories().setPictureMask("1 -2.3 3 -4");
      assertThat(getViolations(), is(empty()));
    }

    @Test
    public void goodMaskWith2Rect() {
      note.getNoteAccessories().setPictureMask("-1 2 3 4 11 22 33 44");
      assertThat(getViolations(), is(empty()));
    }

    @Test
    public void masksNeedToBeFourNumbers() {
      note.getNoteAccessories().setPictureMask("1 2 3 4 5 6 7");
      assertThat(getViolations(), is(not(empty())));
      Path propertyPath = getViolations().stream().findFirst().get().getPropertyPath();
      assertThat(propertyPath.toString(), equalTo("noteAccessories.pictureMask"));
    }

    @Test
    public void withBothUploadPictureProxyAndPicture() {
      note.getNoteAccessories()
          .setUploadPictureProxy(makeMe.anUploadedPicture().toMultiplePartFilePlease());
      note.getNoteAccessories().setPictureUrl("http://url/img");
      assertThat(getViolations(), is(not(empty())));
      List<String> errorFields =
          getViolations().stream().map(v -> v.getPropertyPath().toString()).collect(toList());
      assertThat(
          errorFields,
          containsInAnyOrder("noteAccessories.uploadPicture", "noteAccessories.pictureUrl"));
    }

    private Set<ConstraintViolation<Note>> getViolations() {
      return validator.validate(note);
    }
  }
}
