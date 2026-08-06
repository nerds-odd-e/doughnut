package com.odde.doughnut.controllers;

import static com.odde.doughnut.controllers.AiControllerExtractNoteTestSupport.newRootNoteWithExtractableContent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import com.odde.doughnut.controllers.dto.NoteRefinementQuestionContextDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.openai.client.OpenAIClient;
import java.util.List;
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

      Map<String, Object> body = controller.exportRefinementLayoutRequest(testNote, null);

      assertThat(body).containsKeys("model", "instructions", "input", "text");
      assertThat(body.get("max_output_tokens")).isEqualTo(1000);
      @SuppressWarnings("unchecked")
      Map<String, Object> format =
          (Map<String, Object>) ((Map<String, Object>) body.get("text")).get("format");
      assertThat(format.get("type")).isEqualTo("json_schema");
      assertThat(format).containsKey("schema");
      assertThat(body.get("instructions").toString())
          .contains("Return one current-content layout for the note content")
          .contains("not alternative breakdown suggestions")
          .contains("Do not create grandchildren")
          .contains("Focus Note content only")
          .contains("Set ledToQuestion to false for every item")
          .doesNotContain("Set ledToQuestion=true");
      verifyNoInteractions(officialClient);
    }

    @Test
    void shouldExportQuestionAwareInstructionsWhenQuestionContextProvided()
        throws UnexpectedNoAccessRightException {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      NoteRefinementQuestionContextDTO questionContext = new NoteRefinementQuestionContextDTO();
      questionContext.setStem("What is the capital of France?");
      questionContext.setChoices(List.of("Paris", "London"));
      questionContext.setCorrectAnswerIndex(0);

      Map<String, Object> body =
          controller.exportRefinementLayoutRequest(testNote, questionContext);

      assertThat(body.get("instructions").toString())
          .contains("What is the capital of France?")
          .contains("0. Paris")
          .contains("1. London")
          .contains("Correct answer index: 0")
          .contains("Set ledToQuestion=true")
          .doesNotContain("Set ledToQuestion to false for every item");
      verifyNoInteractions(officialClient);
    }
  }
}
