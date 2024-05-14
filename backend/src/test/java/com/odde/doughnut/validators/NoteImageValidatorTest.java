package com.odde.doughnut.validators;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.doughnut.controllers.dto.NoteAccessoriesDTO;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.MakeMeWithoutDB;
import jakarta.validation.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NoteImageValidatorTest {

  MakeMeWithoutDB makeMe = MakeMe.makeMeWithoutFactoryService();

  private Validator validator;
  private final NoteAccessoriesDTO note = new NoteAccessoriesDTO();

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
    note.setImageMask("1 -2.3 3 -4");
    assertThat(getViolations(), is(empty()));
  }

  @Test
  public void goodMaskWith2Rect() {
    note.setImageMask("-1 2 3 4 11 22 33 44");
    assertThat(getViolations(), is(empty()));
  }

  @Test
  public void masksNeedToBeFourNumbers() {
    note.setImageMask("1 2 3 4 5 6 7");
    assertThat(getViolations(), is(not(empty())));
    Path propertyPath = getViolations().stream().findFirst().get().getPropertyPath();
    assertThat(propertyPath.toString(), equalTo("imageMask"));
  }

  @Test
  public void withBothUploadImageProxyAndPicture() {
    note.setUploadImage(makeMe.anUploadedPicture().toMultiplePartFilePlease());
    note.setImageUrl("http://url/img");
    assertThat(getViolations(), is(not(empty())));
    List<String> errorFields =
        getViolations().stream().map(v -> v.getPropertyPath().toString()).collect(toList());
    assertThat(errorFields, containsInAnyOrder("uploadImage", "imageUrl"));
  }

  private Set<ConstraintViolation<NoteAccessoriesDTO>> getViolations() {
    return validator.validate(note);
  }
}
