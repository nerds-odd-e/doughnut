package com.odde.doughnut.controllers.dto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DisplayNameTrimmingDeserializerTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperConfig().objectMapper();
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void trimsSurroundingUnicodeWhitespaceFromJson() throws Exception {
    NoteUpdateTitleDTO dto =
        OBJECT_MAPPER.readValue(
            "{\"newTitle\": \"\\u3000hello\\u3000\"}", NoteUpdateTitleDTO.class);
    assertThat(dto.getNewTitle(), equalTo("hello"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{\"newTitle\": \"\\u3000\"}", "{\"newTitle\": \"\\u200B\"}"})
  void noteTitleWhitespaceOnlyAfterTrimFailsNotBlank(String json) throws Exception {
    NoteUpdateTitleDTO dto = OBJECT_MAPPER.readValue(json, NoteUpdateTitleDTO.class);
    Set<ConstraintViolation<NoteUpdateTitleDTO>> violations = validator.validate(dto);
    assertThat(violations, hasSize(1));
    assertThat(violations.iterator().next().getPropertyPath().toString(), equalTo("newTitle"));
  }

  @Test
  void folderCreationWhitespaceOnlyAfterTrimFailsNotBlank() throws Exception {
    FolderCreationRequest req =
        OBJECT_MAPPER.readValue("{\"name\": \"\\u3000\\u00A0\"}", FolderCreationRequest.class);
    Set<ConstraintViolation<FolderCreationRequest>> violations = validator.validate(req);
    assertThat(violations, hasSize(1));
    assertThat(violations.iterator().next().getPropertyPath().toString(), equalTo("name"));
  }
}
