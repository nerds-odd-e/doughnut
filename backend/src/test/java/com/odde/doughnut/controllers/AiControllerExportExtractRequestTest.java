package com.odde.doughnut.controllers;

import static com.odde.doughnut.controllers.AiControllerExtractNoteTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.ai.NoteRefinementLayout;
import com.odde.doughnut.services.ai.NoteRefinementLayoutItem;
import com.openai.client.OpenAIClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AiControllerExportExtractRequestTest extends ControllerTestBase {
  @Autowired AiController controller;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class ExportExtractRequest {
    @Test
    void shouldExportExtractRequestWithBodyMapReflectingSelection()
        throws UnexpectedNoAccessRightException {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      NoteRefinementLayout layout = sampleLayout();

      Map<String, Object> body =
          controller.exportExtractRequest(
              testNote, layoutSelectionRequest(layout, List.of("p1-1", "p2")));

      assertThat(body).isNotNull();
      assertThat(body).containsKeys("model", "instructions", "input", "text");
      assertThat(body.get("model")).isNotNull();
      assertThat(body.get("instructions")).isNotNull();
      assertThat(body.get("input")).isNotNull();
      assertThat(body.get("max_output_tokens")).isEqualTo(3000);

      @SuppressWarnings("unchecked")
      Map<String, Object> text = (Map<String, Object>) body.get("text");
      assertThat(text).containsKey("format");
      @SuppressWarnings("unchecked")
      Map<String, Object> format = (Map<String, Object>) text.get("format");
      assertThat(format.get("type")).isEqualTo("json_schema");
      assertThat(format).containsKey("schema");

      String instructions = body.get("instructions").toString();
      assertThat(instructions).contains("Full note layout:");
      assertThat(instructions).contains("\"id\" : \"p1-1\"");
      assertThat(instructions).contains("Selected layout item ids to extract together");
      assertThat(instructions).contains("[p1-1, p2]");
      assertThat(instructions).contains("- p1-1: \"key suggestion to extract\"");
      assertThat(instructions).contains("- p2: \"Other point\"");

      verifyNoInteractions(officialClient);
    }

    private NoteRefinementLayout sampleLayout() {
      return new NoteRefinementLayout(
          List.of(
              new NoteRefinementLayoutItem(
                  "p1",
                  "Main concept",
                  false,
                  List.of(
                      new NoteRefinementLayoutItem(
                          "p1-1", "key suggestion to extract", false, List.of()))),
              new NoteRefinementLayoutItem("p2", "Other point", false, List.of())));
    }
  }
}
