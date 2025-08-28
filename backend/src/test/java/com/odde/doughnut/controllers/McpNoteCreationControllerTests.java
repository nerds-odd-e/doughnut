package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.dto.McpNoteAddDTO;
import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.controllers.dto.NoteSearchResult;
import com.odde.doughnut.controllers.dto.NoteTopology;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import com.odde.doughnut.services.search.NoteSearchService;
import com.odde.doughnut.testability.MakeMe;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindException;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class McpNoteCreationControllerTests {

  private McpNoteCreationController controller;
  private NoteCreationDTO noteCreation;
  @MockitoBean private NoteRepository noteRepository;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();
  @Mock HttpClientAdapter httpClientAdapter;
  @MockitoBean NoteSearchService noteSearchService;
  @Autowired MakeMe makeMe;
  @Autowired WebApplicationContext webApplicationContext;

  @Autowired private ModelFactoryService modelFactoryService;

  @BeforeEach
  void setup() {
    MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    UserModel userModel = makeMe.aUser().toModelPlease();
    noteCreation = new NoteCreationDTO();
    noteCreation.setNewTitle("new note");
    controller =
        new McpNoteCreationController(
            modelFactoryService,
            userModel,
            httpClientAdapter,
            testabilitySettings,
            noteSearchService,
            noteRepository);

    Note lordOfTheRingsNote = makeMe.aNote().creatorAndOwner(userModel).please();
    lordOfTheRingsNote.setTopicConstructor("Lord of the Rings");
    when(noteRepository.findById(org.mockito.ArgumentMatchers.any()))
        .thenReturn(java.util.Optional.of(lordOfTheRingsNote));
  }

  @Nested
  class CreateNoteTest {

    @Test
    void shouldCreateNoteSuccessfully()
        throws UnexpectedNoAccessRightException, BindException, IOException, InterruptedException {
      var noteTopology = new NoteTopology();
      noteTopology.setId(1);
      var searchResult = new NoteSearchResult();
      searchResult.setNoteTopology(noteTopology);

      MockNoteSearchServiceReturn(Arrays.asList(searchResult));
      var mcpNoteDTO = new McpNoteAddDTO();
      mcpNoteDTO.parentNote = "Lord of the Rings";
      mcpNoteDTO.noteCreationDTO = noteCreation;
      var response = controller.createNote(mcpNoteDTO);
      assertEquals("Added new note to parent Notebook Lord of the Rings", response);
    }

    @Test
    void whenNotebookNotExistsShouldReturnParentDoesNotExist()
        throws UnexpectedNoAccessRightException, BindException, IOException, InterruptedException {

      MockNoteSearchServiceReturn(new ArrayList<>());
      var mcpNoteDTO = new McpNoteAddDTO();
      mcpNoteDTO.parentNote = "Harry Potter";
      mcpNoteDTO.noteCreationDTO = noteCreation;

      var response = controller.createNote(mcpNoteDTO);

      assertEquals("This parent does not exist", response);
    }

    private void MockNoteSearchServiceReturn(List<NoteSearchResult> noteSearchResults) {
      when(noteSearchService.searchForNotes(
              org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
          .thenReturn(noteSearchResults);
    }
  }
}
