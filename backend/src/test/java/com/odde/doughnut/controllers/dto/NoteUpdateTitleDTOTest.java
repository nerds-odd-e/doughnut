package com.odde.doughnut.controllers.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.MakeMe;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
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
class NoteUpdateTitleDTOTest {

  @Autowired private MakeMe makeMe;
  @Autowired private Validator validator;

  private NoteUpdateTitleDTO dto;

  @BeforeEach
  void setup() {
    dto = new NoteUpdateTitleDTO();
    dto.setNewTitle("A valid title");
  }

  @Test
  void validTitle() {
    assertEquals(0, validator.validate(dto).size());
  }

  @Test
  void rejectsNullTitle() {
    dto.setNewTitle(null);
    Set<ConstraintViolation<NoteUpdateTitleDTO>> violations = validator.validate(dto);
    assertEquals(1, violations.size());
    assertTrue(
        violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("newTitle")));
  }

  @Test
  void rejectsEmptyTitle() {
    dto.setNewTitle("");
    Set<ConstraintViolation<NoteUpdateTitleDTO>> violations = validator.validate(dto);
    assertEquals(1, violations.size());
  }

  @Test
  void rejectsWhitespaceOnlyTitle() {
    dto.setNewTitle("   ");
    Set<ConstraintViolation<NoteUpdateTitleDTO>> violations = validator.validate(dto);
    assertEquals(1, violations.size());
  }

  @Test
  void rejectsTitleOverMaxLength() {
    dto.setNewTitle(makeMe.aStringOfLength(Note.MAX_TITLE_LENGTH + 1));
    Set<ConstraintViolation<NoteUpdateTitleDTO>> violations = validator.validate(dto);
    assertEquals(1, violations.size());
  }
}
