package com.odde.doughnut.validators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
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
  @ValueSource(strings = {"\\", "/", ":"})
  void noteTitle_rejectsSeparators(String sep) {
    NoteUpdateTitleDTO dto = new NoteUpdateTitleDTO();
    dto.setNewTitle("a" + sep + "b");
    assertViolatesProperty(validator.validate(dto), "newTitle");
  }

  @Test
  void notebookUpdateName_rejectsSeparators() {
    NotebookUpdateRequest req = new NotebookUpdateRequest();
    req.setName("x/y");
    assertViolatesProperty(validator.validate(req), "name");
  }

  @Test
  void folderCreationName_rejectsSeparators() {
    FolderCreationRequest req = new FolderCreationRequest();
    req.setName("a:b");
    assertViolatesProperty(validator.validate(req), "name");
  }

  private static <T> void assertViolatesProperty(
      Set<ConstraintViolation<T>> violations, String property) {
    assertThat(violations, hasItem(hasProperty("propertyPath", hasToString(property))));
  }
}
