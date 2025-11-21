package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.McpNoteAddDTO;
import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.NoteConstructionService;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.validation.BindException;
import org.springframework.web.server.ResponseStatusException;

class McpNoteCreationControllerTests extends ControllerTestBase {

  @Autowired McpNoteCreationController controller;
  private NoteCreationDTO noteCreation;
  @MockitoBean HttpClientAdapter httpClientAdapter;
  @Autowired NoteService noteService;
  @Autowired NoteConstructionService noteConstructionService;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    noteCreation = new NoteCreationDTO();
    noteCreation.setNewTitle("new note");
  }

  @Nested
  class CreateNoteTest {

    @Test
    void shouldThrowExceptionWhenUserNotFound() {

      currentUser.setUser(null);
      ResponseStatusException exception =
          assertThrows(
              ResponseStatusException.class,
              () -> {
                controller.createNoteViaMcp(GeneratorMcpNoteAddDTO("Harry Potter"));
              });
      assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void shouldCreateNoteSuccessfully()
        throws UnexpectedNoAccessRightException, BindException, IOException, InterruptedException {
      makeMe.aNote("Lord of the Rings").creatorAndOwner(currentUser.getUser()).please();

      var result = controller.createNoteViaMcp(GeneratorMcpNoteAddDTO("Lord of the Rings"));
      assertEquals("new note", result.getCreated().getNote().getTopicConstructor());
    }

    @Test
    void whenNotebookNotExistsShouldThrowException() {
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> {
            controller.createNoteViaMcp(GeneratorMcpNoteAddDTO("Harry Potter"));
          });
    }

    private McpNoteAddDTO GeneratorMcpNoteAddDTO(String parentNote) {
      var mcpNoteDTO = new McpNoteAddDTO();
      mcpNoteDTO.parentNote = parentNote;
      mcpNoteDTO.noteCreationDTO = noteCreation;
      return mcpNoteDTO;
    }
  }
}
