package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.EmbeddingService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class DisplayNameNormalizationMvcTest extends ControllerTestBase {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private NoteRepository noteRepository;

  @MockitoBean private EmbeddingService embeddingService;

  @BeforeEach
  void setup() {
    when(embeddingService.streamEmbeddingsForNoteList(any()))
        .thenAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              List<Note> notes = (List<Note>) invocation.getArgument(0);
              return notes.stream()
                  .map(
                      n ->
                          new EmbeddingService.EmbeddingForNote(
                              n, Optional.of(List.of(1.0f, 2.0f, 3.0f))));
            });
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void createNoteStoresTitleWithoutSurroundingUnicodeWhitespace() throws Exception {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();

    MvcResult result =
        mockMvc
            .perform(
                post("/api/notebooks/{notebookId}/create-note", nb.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"newTitle\":\"\\u3000 spaced \\u3000\"}"))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    Note created = noteRepository.findById(root.get("id").asInt()).orElseThrow();
    assertThat(created.getTitle(), equalTo("spaced"));
  }

  @Test
  void updateNoteTitleStoresTitleWithoutSurroundingWhitespace() throws Exception {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Before").please();

    mockMvc
        .perform(
            patch("/api/text_content/{note}/title", note.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newTitle\":\"  After  \"}"))
        .andExpect(status().isOk());

    makeMe.refresh(note);
    assertThat(note.getTitle(), equalTo("After"));
  }
}
