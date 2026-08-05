package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.ImageRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.services.focusContext.FocusContextResult;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import jakarta.validation.Validation;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerTests extends ControllerTestBase {
  @Autowired ImageRepository imageRepository;
  @Autowired NoteController controller;
  @Autowired UserService userService;
  @MockitoBean HttpClientAdapter httpClientAdapter;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class UploadNoteImage {
    @Test
    void shouldReturnImagePathAndPersistImageLinkedToNote()
        throws UnexpectedNoAccessRightException, IOException {
      Note note = makeMe.aNote("n").notebookOwnedBy(currentUser.getUser()).please();
      NoteImageUploadDTO dto = new NoteImageUploadDTO();
      dto.setUploadImage(makeMe.anUploadedImage().toMultiplePartFilePlease());

      NoteImageUploadResult result = controller.uploadNoteImage(note, dto);

      assertThat(result.imagePath(), startsWith("/attachments/images/"));
      String[] segments = result.imagePath().split("/");
      assertThat(segments.length, equalTo(5));
      assertThat(segments[1], equalTo("attachments"));
      assertThat(segments[2], equalTo("images"));
      int imageId = Integer.parseInt(segments[3]);
      assertThat(segments[4], equalTo("my.png"));
      Image saved = imageRepository.findById(imageId).orElseThrow();
      assertThat(saved.getNote().getId(), equalTo(note.getId()));
    }

    @Test
    void shouldNotAllowUploadForNoteBelongingToAnotherUser() {
      User other = makeMe.aUser().please();
      Note note = makeMe.aNote().notebookOwnedBy(other).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.uploadNoteImage(note, new NoteImageUploadDTO()));
    }

    @Test
    void shouldRejectInvalidUploadContentType() {
      try (var factory = Validation.buildDefaultValidatorFactory()) {
        NoteImageUploadDTO dto = new NoteImageUploadDTO();
        dto.setUploadImage(
            new MockMultipartFile(
                "uploadImage", "x.pdf", "application/pdf", "not-empty".getBytes()));
        assertThat(factory.getValidator().validate(dto), is(not(empty())));
      }
    }
  }

  @Nested
  class UpdateNoteRecallSetting {
    @Test
    void shouldPutNoteBackToAssimilationListWhenRememberSpellingIsAddedLater()
        throws UnexpectedNoAccessRightException {
      Note source = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      makeMe.aMemoryTrackerFor(source).by(currentUser.getUser()).please();
      assertThat(
          userService.getUnassimilatedNotes(currentUser.getUser()).toList(),
          not(hasItem(hasProperty("id", equalTo(source.getId())))));

      NoteRecallSetting noteRecallSetting = new NoteRecallSetting();
      noteRecallSetting.setRememberSpelling(true);
      controller.updateNoteRecallSetting(source, noteRecallSetting);

      assertThat(
          userService.getUnassimilatedNotes(currentUser.getUser()).toList(),
          hasItem(hasProperty("id", equalTo(source.getId()))));
    }
  }

  @Nested
  class GraphTests {
    Note rootNote;

    @BeforeEach
    void setup() {
      rootNote = makeMe.aNote("Root").notebookOwnedBy(currentUser.getUser()).please();
    }

    @Test
    void shouldReturnGraphWithDefaultTokenLimit() throws UnexpectedNoAccessRightException {
      FocusContextResult result = controller.getGraph(rootNote, 5000);

      assertThat(result.getFocusNote().getNotebook(), equalTo(rootNote.getNotebook().getName()));
      assertThat(result.getFocusNote().getDepth(), equalTo(0));
      assertThat(result.getFocusNote().getOutgoingLinks(), is(notNullValue()));
    }

    @Test
    void shouldRespectCustomTokenLimit() throws UnexpectedNoAccessRightException {
      FocusContextResult result = controller.getGraph(rootNote, 1);
      assertThat(result.getRelatedNotes(), is(empty()));
    }

    @Test
    void relatedNotesExposeEdgeTypeDepthAndPath() throws UnexpectedNoAccessRightException {
      FocusContextResult result = controller.getGraph(rootNote, 5000);
      for (var n : result.getRelatedNotes()) {
        assertThat(n.getEdgeType(), is(notNullValue()));
        assertThat(n.getDepth(), greaterThan(0));
        assertThat(n.getRetrievalPath(), is(notNullValue()));
      }
    }

    @Test
    void shouldNotAllowAccessToUnauthorizedNotes() {
      User otherUser = makeMe.aUser().please();
      Note unauthorizedNote = makeMe.aNote().notebookOwnedBy(otherUser).please();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.getGraph(unauthorizedNote, 5000));
    }
  }

  @Nested
  class AiContextMarkdownTests {
    @Test
    void shouldReturnMarkdownForReadableNote() throws UnexpectedNoAccessRightException {
      Note note =
          makeMe.aNote("Focus").content("Body").notebookOwnedBy(currentUser.getUser()).please();
      NoteAiContextMarkdown dto = controller.getAiContextMarkdown(note, 5000);
      assertThat(dto.markdown(), containsString("Focus"));
      assertThat(dto.markdown(), containsString("Body"));
    }

    @Test
    void shouldNotAllowAccessToUnauthorizedNotes() {
      User otherUser = makeMe.aUser().please();
      Note unauthorizedNote = makeMe.aNote().notebookOwnedBy(otherUser).please();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.getAiContextMarkdown(unauthorizedNote, 5000));
    }
  }
}
