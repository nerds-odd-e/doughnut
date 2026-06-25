package com.odde.doughnut.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    Set<ConstraintViolation<NoteUpdateTitleDTO>> violations = validator.validate(dto);
    assertEquals(1, violations.size());
    assertTrue(
        violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("newTitle")));
  }

  @Test
  void notebookUpdateName_rejectsSeparators() {
    NotebookUpdateRequest req = new NotebookUpdateRequest();
    req.setName("x/y");
    Set<ConstraintViolation<NotebookUpdateRequest>> violations = validator.validate(req);
    assertEquals(1, violations.size());
    assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
  }

  @Test
  void folderCreationName_rejectsSeparators() {
    FolderCreationRequest req = new FolderCreationRequest();
    req.setName("a:b");
    Set<ConstraintViolation<FolderCreationRequest>> violations = validator.validate(req);
    assertEquals(1, violations.size());
    assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
  }
}
