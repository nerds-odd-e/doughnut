package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteUpdateDetailsDTO;
import com.odde.doughnut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.UserToken;
import com.odde.doughnut.entities.repositories.UserTokenRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestTextContentControllerTests {
  @Autowired ModelFactoryService modelFactoryService;

  @Autowired MakeMe makeMe;
  private UserModel userModel;
  RestTextContentController controller;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();
  Note note;
  HttpServletRequest mockRequest;

  @Autowired UserTokenRepository userTokenRepository;

  @BeforeEach
  void setup() {
    userModel = makeMe.aUser().toModelPlease();
    userTokenRepository.save(new UserToken(userModel.getEntity().getId(), "valid-token"));
    note = makeMe.aNote("new").creatorAndOwner(userModel).please();
    mockRequest = Mockito.mock(HttpServletRequest.class);
    // Default: controller with currentUser = null, so token-based auth is tested
    controller =
        new RestTextContentController(modelFactoryService, null, testabilitySettings, mockRequest);
  }

  @Nested
  class updateNoteTopticTest {
    NoteUpdateTitleDTO noteUpdateTitleDTO = new NoteUpdateTitleDTO();

    @BeforeEach
    void setup() {
      noteUpdateTitleDTO.setNewTitle("new title");
    }

    @Test
    void shouldBeAbleToSaveNoteTitle() throws UnexpectedNoAccessRightException {
      Mockito.when(mockRequest.getHeader("Authorization")).thenReturn("valid-token");
      NoteRealm response = controller.updateNoteTitle(note, noteUpdateTitleDTO);
      assertThat(response.getId(), equalTo(note.getId()));
      assertThat(response.getNote().getTopicConstructor(), equalTo("new title"));
    }

    @Test
    void shouldNotAllowOthersToChange() {
      note = makeMe.aNote("another").creatorAndOwner(makeMe.aUser().please()).please();
      Mockito.when(mockRequest.getHeader("Authorization")).thenReturn("valid-token");
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNoteTitle(note, noteUpdateTitleDTO));
    }

    @Test
    void shouldNotAllowWithInvalidToken() {
      Mockito.when(mockRequest.getHeader("Authorization")).thenReturn("invalid-token");
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNoteTitle(note, noteUpdateTitleDTO));
    }

    @Test
    void shouldNotAllowWithMissingToken() {
      Mockito.when(mockRequest.getHeader("Authorization")).thenReturn(null);
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNoteTitle(note, noteUpdateTitleDTO));
    }
  }

  @Nested
  class updateNoteDetailsTest {
    NoteUpdateDetailsDTO noteUpdateDetailsDTO = new NoteUpdateDetailsDTO();

    @BeforeEach
    void setup() {
      noteUpdateDetailsDTO.setDetails("new details");
    }

    @Test
    void shouldBeAbleToSaveNoteWhenValid() throws UnexpectedNoAccessRightException, IOException {
      Mockito.when(mockRequest.getHeader("Authorization")).thenReturn("valid-token");
      NoteRealm response = controller.updateNoteDetails(note, noteUpdateDetailsDTO);
      assertThat(response.getId(), equalTo(note.getId()));
      assertThat(response.getNote().getDetails(), equalTo("new details"));
    }

    @Test
    void shouldNotAllowDetailsWithInvalidToken() {
      Mockito.when(mockRequest.getHeader("Authorization")).thenReturn("invalid-token");
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNoteDetails(note, noteUpdateDetailsDTO));
    }

    @Test
    void shouldNotAllowDetailsWithMissingToken() {
      Mockito.when(mockRequest.getHeader("Authorization")).thenReturn(null);
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.updateNoteDetails(note, noteUpdateDetailsDTO));
    }
  }
}
