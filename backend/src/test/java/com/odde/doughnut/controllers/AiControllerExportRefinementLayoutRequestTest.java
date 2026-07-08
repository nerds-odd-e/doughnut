package com.odde.doughnut.controllers;

import static com.odde.doughnut.controllers.AiControllerExtractNoteTestSupport.newRootNoteWithExtractableContent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.openai.client.OpenAIClient;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AiControllerExportRefinementLayoutRequestTest extends ControllerTestBase {
  @Autowired AiController controller;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class ExportRefinementLayoutRequest {
    @Test
    void shouldExportRefinementLayoutRequestWithBodyMap() throws UnexpectedNoAccessRightException {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());

      Map<String, Object> body = controller.exportRefinementLayoutRequest(testNote);

      assertThat(body).isNotNull();
      assertThat(body).containsKeys("model", "instructions", "input", "text");
      assertThat(body.get("model")).isNotNull();
      assertThat(body.get("instructions")).isNotNull();
      assertThat(body.get("input")).isNotNull();
      assertThat(body.get("max_output_tokens")).isEqualTo(1000);

      @SuppressWarnings("unchecked")
      Map<String, Object> text = (Map<String, Object>) body.get("text");
      assertThat(text).containsKey("format");
      @SuppressWarnings("unchecked")
      Map<String, Object> format = (Map<String, Object>) text.get("format");
      assertThat(format.get("type")).isEqualTo("json_schema");
      assertThat(format).containsKey("schema");

      String instructions = body.get("instructions").toString();
      assertThat(instructions).contains("Return one current-content layout for the note content");
      assertThat(instructions).contains("not alternative breakdown suggestions");
      assertThat(instructions).contains("Do not create grandchildren");
      assertThat(instructions).contains("Focus Note content only");

      verifyNoInteractions(officialClient);
    }
  }
}
