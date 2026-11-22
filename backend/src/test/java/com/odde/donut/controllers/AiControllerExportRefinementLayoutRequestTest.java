package com.odde.donut.controllers;

import static com.odde.donut.controllers.AiControllerExtractNoteTestSupport.newRootNoteWithExtractableContent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.donut.controllers.dto.NoteRefinementQuestionContextDTO;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.ai.NoteRefinementLayout;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AiControllerExportRefinementLayoutRequestTest extends ControllerTestBase {
  @Autowired AiController controller;

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
      @SuppressWarnings("unchecked")
      Map<String, Object> format =
          (Map<String, Object>) ((Map<String, Object>) body.get("text")).get("format");
      assertThat(format.get("type")).isEqualTo("json_schema");
      assertThat(format).containsKey("schema");
      assertThat(body.get("instructions").toString())
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
          .contains("Set ledToQuestion=true")
          .doesNotContain("Set ledToQuestion to false for every item");
      verifyNoInteractions(officialClient);
    }

    @Test
    void exportedOutputTokenBudgetMatchesGeneration()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      long[] generationBudget = {0L};
      new OpenAiStructuredResponseMock(officialClient)
          .stubStructuredResponseFromRequest(request -> {
            generationBudget[0] = request.rawParams().maxOutputTokens().orElse(0L);
            return new NoteRefinementLayout(List.of());
          });

      controller.generateRefinementSuggestions(testNote, null);
      Object exportedBudget =
          controller.exportRefinementLayoutRequest(testNote, null).get("max_output_tokens");

      assertThat(((Number) exportedBudget).longValue()).isEqualTo(generationBudget[0]);
    }

    @Test
    void asksForConciseItemsThatFollowSourceSections() throws UnexpectedNoAccessRightException {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());

      String instructions =
          controller.exportRefinementLayoutRequest(testNote, null).get("instructions").toString();

      assertThat(instructions)
          .contains("Prefer meaningful existing sections")
          .contains("concise items in source order")
          .contains("numbered senses are top-level items")
          .contains("distinct usages are children")
          .contains("the note's languages")
          .contains("Grammatical labels are context and stay with their sense")
          .contains("Illustrative or supporting sentences stay with their sense")
          .contains("Decorative markers are not sections");
      verifyNoInteractions(officialClient);
    }
  }
}
