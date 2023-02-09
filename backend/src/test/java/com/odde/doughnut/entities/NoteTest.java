package com.odde.doughnut.entities;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
public class NoteTest {

  @Autowired MakeMe makeMe;
  User user;

  @Test
  void timeOrder() {
    Note parent = makeMe.aNote().please();
    Note note1 = makeMe.aNote().under(parent).please();
    Note note2 = makeMe.aNote().under(parent).please();
    makeMe.refresh(parent);
    assertThat(parent.getChildren(), containsInRelativeOrder(note1, note2));
  }

  @Test
  void getTitles_givenSingleNote_returnsOneTitle() {
    Note aNote = makeMe.aNote("This is a Title").please();
    assertEquals(aNote.getTitleAndOffSpringTitles(), List.of("This is a Title"));
  }

  @Test
  void getTitles_givenNoteHasChild_returnsTwoTitles() {
    Note parentNote = makeMe.aNote("This is a parent note").please();
    makeMe.aNote("This is a child note").under(parentNote).please();
    makeMe.refresh(parentNote);
    assertEquals(List.of("This is a parent note", "This is a child note"), parentNote.getTitleAndOffSpringTitles());
  }

  @Test
  void getTitles_givenNoteHasTwoChildren_returnsThreeTitles() {
    Note parentNote = makeMe.aNote("This is a parent note").please();
    makeMe.aNote("This is a child note").under(parentNote).please();
    makeMe.aNote("This is a child note too").under(parentNote).please();
    makeMe.refresh(parentNote);
    assertEquals(
        List.of("This is a parent note", "This is a child note", "This is a child note too"),
        parentNote.getTitleAndOffSpringTitles());
  }

  @Test
  void getTitles_givenNoteHasGrandchild_returnsThreeTitles() {
    Note parentNote = makeMe.aNote("This is a parent note").please();
    Note childNote = makeMe.aNote("This is a child note").under(parentNote).please();
    makeMe.aNote("This is a grand child note").under(childNote).please();
    makeMe.refresh(parentNote);
    makeMe.refresh(childNote);
    assertEquals(
        List.of("This is a parent note", "This is a child note", "This is a grand child note"),
        parentNote.getTitleAndOffSpringTitles());
  }

  @Test
  void getTitles_givenNoteHasGrandchildren_returnsSevenTitles() {
    Note parentNote = makeMe.aNote("parent").please();
    Note childNote = makeMe.aNote("child 1").under(parentNote).please();
    Note childNoteTwo = makeMe.aNote("child 2").under(parentNote).please();
    makeMe.aNote("grand child 1").under(childNote).please();
    makeMe.aNote("grand child 2").under(childNote).please();
    makeMe.aNote("grand child 3").under(childNoteTwo).please();
    makeMe.aNote("grand child 4").under(childNoteTwo).please();
    makeMe.refresh(parentNote);
    makeMe.refresh(childNote);
    makeMe.refresh(childNoteTwo);
    assertEquals(
        List.of(
            "parent",
            "child 1",
            "grand child 1",
            "grand child 2",
            "child 2",
            "grand child 3",
            "grand child 4"),
        parentNote.getTitleAndOffSpringTitles());
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
