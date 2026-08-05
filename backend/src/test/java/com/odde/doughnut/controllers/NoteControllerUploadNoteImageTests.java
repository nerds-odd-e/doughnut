package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.NoteImageUploadDTO;
import com.odde.doughnut.controllers.dto.NoteImageUploadResult;
import com.odde.doughnut.entities.Image;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.ImageRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.httpQuery.HttpClientAdapter;
import jakarta.validation.Validation;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerUploadNoteImageTests extends ControllerTestBase {
  @Autowired ImageRepository imageRepository;
  @Autowired NoteController controller;
  @MockitoBean HttpClientAdapter httpClientAdapter;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void shouldReturnImagePathAndPersistImageLinkedToNote()
      throws UnexpectedNoAccessRightException, IOException {
    Note note = makeMe.aNote("n").notebookOwnedBy(currentUser.getUser()).please();
    NoteImageUploadDTO dto = new NoteImageUploadDTO();
    dto.setUploadImage(makeMe.anUploadedImage().toMultiplePartFilePlease());

    NoteImageUploadResult result = controller.uploadNoteImage(note, dto);

    assertThat(result.imagePath(), matchesPattern("/attachments/images/\\d+/my\\.png"));
    int imageId = Integer.parseInt(result.imagePath().split("/")[3]);
    Image saved = imageRepository.findById(imageId).orElseThrow();
    assertThat(saved.getNote().getId(), equalTo(note.getId()));
  }

  @Test
  void shouldNotAllowUploadForNoteBelongingToAnotherUser() {
    Note note = makeMe.aNote().notebookOwnedBy(makeMe.aUser().please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.uploadNoteImage(note, new NoteImageUploadDTO()));
  }

  @Test
  void shouldRejectInvalidUploadContentType() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      NoteImageUploadDTO dto = new NoteImageUploadDTO();
      dto.setUploadImage(
          new MockMultipartFile("uploadImage", "x.pdf", "application/pdf", "not-empty".getBytes()));
      assertThat(factory.getValidator().validate(dto), is(not(empty())));
    }
  }
}
