package com.odde.donut.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.entities.Note;
import com.odde.donut.services.ai.NoteRefinementLayout;
import com.odde.donut.services.ai.NoteRefinementLayoutItems;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
import com.openai.models.responses.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class AiControllerRefinementLayoutProblemTest extends ControllerTestBase {
  private static final String PARTIAL_LAYOUT = "{\"items\":[{\"text\":\"刀で切";

  @Autowired MockMvc mockMvc;

  OpenAiStructuredResponseMock openAiStructuredResponseMock;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
  }

  @Test
  void outputTokenLimitIsASafeProblemDetail() throws Exception {
    Note note = ownedNote("A dictionary entry.");
    openAiStructuredResponseMock.stubStructuredResponseIncomplete(
        PARTIAL_LAYOUT, Response.IncompleteDetails.Reason.of("max_output_tokens"));

    mockMvc
        .perform(post("/api/ai/generate-refinement-suggestions/{note}", note.getId()))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value("Generation could not finish within its limit."))
        .andExpect(content().string(not(containsString("刀で切"))));
  }

  @Test
  void contentFilterIsAGenericRefinementProblem() throws Exception {
    Note note = ownedNote("A dictionary entry.");
    openAiStructuredResponseMock.stubStructuredResponseIncomplete(
        PARTIAL_LAYOUT, Response.IncompleteDetails.Reason.of("content_filter"));

    mockMvc
        .perform(post("/api/ai/generate-refinement-suggestions/{note}", note.getId()))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value("AI could not generate a refinement layout."))
        .andExpect(content().string(not(containsString("刀で切"))));
  }

  @Test
  void invalidLayoutIsAGenericRefinementProblem() throws Exception {
    Note note = ownedNote("A dictionary entry.");
    openAiStructuredResponseMock.stubStructuredResponse(
        new NoteRefinementLayout(
            List.of(
                NoteRefinementLayoutItems.leaf("same", "Point 1"),
                NoteRefinementLayoutItems.leaf("same", "Point 2"))));

    mockMvc
        .perform(post("/api/ai/generate-refinement-suggestions/{note}", note.getId()))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value("AI could not generate a refinement layout."));
  }

  @Test
  void missingProviderResultIsAGenericRefinementProblem() throws Exception {
    Note note = ownedNote("A dictionary entry.");
    openAiStructuredResponseMock.stubStructuredResponse(null);

    mockMvc
        .perform(post("/api/ai/generate-refinement-suggestions/{note}", note.getId()))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value("AI could not generate a refinement layout."));
  }

  @Test
  void blankNoteIsAValidEmptyLayout() throws Exception {
    Note note = ownedNote("   ");

    mockMvc
        .perform(post("/api/ai/generate-refinement-suggestions/{note}", note.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", empty()));
  }

  @Test
  void completedEmptyLayoutStaysEmpty() throws Exception {
    Note note = ownedNote("A dictionary entry.");
    openAiStructuredResponseMock.stubStructuredResponse(NoteRefinementLayout.empty());

    mockMvc
        .perform(post("/api/ai/generate-refinement-suggestions/{note}", note.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", empty()));
  }

  private Note ownedNote(String content) {
    return makeMe.aNote().notebookOwnedBy(currentUser.getUser()).content(content).please();
  }
}
