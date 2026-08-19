package com.odde.doughnut.validators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasToString;

import com.odde.doughnut.controllers.dto.FolderCreationRequest;
import com.odde.doughnut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.doughnut.controllers.dto.NotebookUpdateRequest;
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
class DisplayNamePathSeparatorsValidationTest {

  @Autowired private Validator validator;

  @ParameterizedTest
  @ValueSource(
      strings = {"\\", "/", ":", "*", "?", "\"", "<", ">", "|", "\u0000", "\u0001", "\u001F"})
  void noteTitle_rejectsOsInvalidCharacters(String invalidChar) {
    NoteUpdateTitleDTO dto = new NoteUpdateTitleDTO();
    dto.setNewTitle("a" + invalidChar + "b");
    assertViolatesProperty(validator.validate(dto), "newTitle");
  }

  @Test
  void noteTitle_rejectionMessageNamesTheForbiddenSet() {
    NoteUpdateTitleDTO dto = new NoteUpdateTitleDTO();
    dto.setNewTitle("Recipe*");
    Set<ConstraintViolation<NoteUpdateTitleDTO>> violations = validator.validate(dto);
    assertThat(violations, hasSize(1));
    assertThat(
        violations.iterator().next().getMessage(),
        equalTo("Name must not contain \\ / : * ? \" < > | or ASCII control characters."));
  }

  @Test
  void noteTitle_surroundingNewlinesAreTrimmedNotOsInvalid() {
    NoteUpdateTitleDTO dto = new NoteUpdateTitleDTO();
    dto.setNewTitle("\nAfter\n");
    assertThat(validator.validate(dto).isEmpty(), equalTo(true));
  }

  @Test
  void noteTitle_embeddedNewlineIsOsInvalid() {
    NoteUpdateTitleDTO dto = new NoteUpdateTitleDTO();
    dto.setNewTitle("After\nBefore");
    assertViolatesProperty(validator.validate(dto), "newTitle");
  }

  @Test
  void noteTitle_allowsFullwidthFormsOfOsInvalidCharacters() {
    NoteUpdateTitleDTO dto = new NoteUpdateTitleDTO();
    dto.setNewTitle("Recipe＊？＂＜＞｜");
    assertThat(validator.validate(dto).isEmpty(), equalTo(true));
  }

  @Test
  void notebookUpdateName_rejectsOsInvalidCharacters() {
    NotebookUpdateRequest req = new NotebookUpdateRequest();
    req.setName("x/y");
    assertViolatesProperty(validator.validate(req), "name");
  }

  @Test
  void folderCreationName_rejectsOsInvalidCharacters() {
    FolderCreationRequest req = new FolderCreationRequest();
    req.setName("a:b");
    assertViolatesProperty(validator.validate(req), "name");
  }

  private static <T> void assertViolatesProperty(
      Set<ConstraintViolation<T>> violations, String property) {
    assertThat(violations, hasItem(hasProperty("propertyPath", hasToString(property))));
  }
}
