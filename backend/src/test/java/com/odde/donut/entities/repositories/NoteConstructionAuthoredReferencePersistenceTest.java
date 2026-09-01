package com.odde.donut.entities.repositories;

import static com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport.rowsFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.controllers.ControllerTestBase;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Exercises the note-creation HTTP boundaries ({@code POST /api/notebooks/{notebook}/create-note}
 * and {@code POST /api/ai/create-extracted-note/{note}}) and asserts what lands in {@code
 * authored_note_reference}: creation establishes the same source-owned reference rows a content
 * save would (ADR 0001 Wiki link).
 *
 * <p>Lives in {@code entities.repositories} (not {@code controllers}) so it can {@code @Autowired}
 * the package-private {@link AuthoredNoteReferenceRowRepository}, while still driving creation
 * through {@link MockMvc} to exercise the real HTTP boundary — same precedent as {@link
 * TextContentControllerAuthoredReferencePersistenceTest}.
 */
@AutoConfigureMockMvc
class NoteConstructionAuthoredReferencePersistenceTest extends ControllerTestBase {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuthoredNoteReferenceRowRepository authoredNoteReferenceRowRepository;
  @Autowired private NoteRepository noteRepository;

  @Test
  void creatingRootNoteWithAuthoredReferencePersistsItsSourceIndexRow() throws Exception {
    currentUser.setUser(makeMe.aUser().please());
    Notebook notebook = makeMe.aNotebook().owner(currentUser.getUser()).please();

    String body =
        objectMapper.writeValueAsString(Map.of("newTitle", "Carrier", "content", "[[Missing]]"));

    MvcResult result =
        mockMvc
            .perform(
                post("/api/notebooks/{notebookId}/create-note", notebook.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    Note created = noteRepository.findById(root.get("id").asInt()).orElseThrow();

    assertThat(rowsFor(authoredNoteReferenceRowRepository, created), hasSize(1));
  }

  @Test
  void creatingNoteFromExtractedSuggestionPersistsAuthoredReferenceRowsForBothNewAndOriginalNote()
      throws Exception {
    currentUser.setUser(makeMe.aUser().please());
    Note source =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .content("Content to extract.")
            .please();
    Note urlTarget = makeMe.aNote().underSameNotebookAs(source).please();

    String requestBody =
        "{\"newNoteTitle\":\"Extracted\",\"newNoteContent\":\"[[Missing]]\","
            + "\"updatedOriginalNoteContent\":\"See [Some Label](/n"
            + urlTarget.getId()
            + ")\"}";

    MvcResult result =
        mockMvc
            .perform(
                post("/api/ai/create-extracted-note/{note}", source.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    Note created = noteRepository.findById(root.get("id").asInt()).orElseThrow();

    assertThat(rowsFor(authoredNoteReferenceRowRepository, created), hasSize(1));
    assertThat(rowsFor(authoredNoteReferenceRowRepository, source), hasSize(1));
  }
}
