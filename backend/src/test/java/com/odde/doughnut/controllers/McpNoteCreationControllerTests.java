package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.dto.McpNoteAddDTO;
import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.controllers.dto.NoteSearchResult;
import com.odde.doughnut.controllers.dto.NoteTopology;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import com.odde.doughnut.services.search.NoteSearchService;
import com.odde.doughnut.testability.TestabilitySettings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

class McpNoteCreationControllerTests extends ControllerTestBase {

  private McpNoteCreationController controller;
  private NoteCreationDTO noteCreation;
  @MockitoBean private NoteRepository noteRepository;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();
  @Mock HttpClientAdapter httpClientAdapter;
  @MockitoBean NoteSearchService noteSearchService;
  @Autowired WebApplicationContext webApplicationContext;
  @Autowired NoteService noteService;

  @Autowired private ModelFactoryService modelFactoryService;

  @BeforeEach
  void setup() {
    MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    currentUser.setUser(makeMe.aUser().please());
    noteCreation = new NoteCreationDTO();
    noteCreation.setNewTitle("new note");
    controller =
        new McpNoteCreationController(
            modelFactoryService,
            makeMe.entityPersister,
            httpClientAdapter,
            testabilitySettings,
            noteSearchService,
            noteRepository,
            noteService,
            authorizationService);

    Note lordOfTheRingsNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
    lordOfTheRingsNote.setTopicConstructor("Lord of the Rings");
    when(noteRepository.findById(org.mockito.ArgumentMatchers.any()))
        .thenReturn(java.util.Optional.of(lordOfTheRingsNote));
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
      var noteTopology = new NoteTopology();
      noteTopology.setId(1);
      var searchResult = new NoteSearchResult();
      searchResult.setNoteTopology(noteTopology);

      MockNoteSearchServiceReturn(Arrays.asList(searchResult));

      var result = controller.createNoteViaMcp(GeneratorMcpNoteAddDTO("Lord of the Rings"));
      assertEquals("new note", result.getCreated().getNote().getTopicConstructor());
    }

    @Test
    void whenNotebookNotExistsShouldThrowException() {
      MockNoteSearchServiceReturn(new ArrayList<>());

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> {
            controller.createNoteViaMcp(GeneratorMcpNoteAddDTO("Harry Potter"));
          });
    }

    private void MockNoteSearchServiceReturn(List<NoteSearchResult> noteSearchResults) {
      when(noteSearchService.searchForNotes(
              org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
          .thenReturn(noteSearchResults);
    }

    private McpNoteAddDTO GeneratorMcpNoteAddDTO(String parentNote) {
      var mcpNoteDTO = new McpNoteAddDTO();
      mcpNoteDTO.parentNote = parentNote;
      mcpNoteDTO.noteCreationDTO = noteCreation;
      return mcpNoteDTO;
    }
  }
}
