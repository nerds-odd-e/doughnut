package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteImageUploadDTO;
import com.odde.donut.controllers.dto.NoteImageUploadResult;
import com.odde.donut.entities.Image;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.httpQuery.HttpClientAdapter;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validation;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class NoteControllerUploadNoteImageTests extends ControllerTestBase {
  @Autowired EntityManager entityManager;
  @Autowired NoteController controller;
  @Autowired AttachmentController attachmentController;
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
    assertThat(savedImage(result).getNote().getId(), equalTo(note.getId()));
  }

  @Test
  void shouldKeepTheOriginalBytesOfAPictureWiderThan2000Pixels()
      throws UnexpectedNoAccessRightException, IOException {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    NoteImageUploadDTO dto = new NoteImageUploadDTO();
    dto.setUploadImage(makeMe.anUploadedImage().metrics(2001, 2).toMultiplePartFilePlease());

    NoteImageUploadResult result = controller.uploadNoteImage(note, dto);

    assertThat(
        attachmentController.showImage(savedImage(result), "my.png").getBody(),
        equalTo(dto.getUploadImage().getBytes()));
  }

  private Image savedImage(NoteImageUploadResult result) {
    return entityManager.find(Image.class, Integer.parseInt(result.imagePath().split("/")[3]));
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

  @Test
  void shouldAcceptExactLimitImageUploadDtoThroughBeanValidation() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      NoteImageUploadDTO dto = new NoteImageUploadDTO();
      dto.setUploadImage(
          new MockMultipartFile(
              "uploadImage", "exact.png", "image/png", new byte[10 * 1024 * 1024]));
      assertThat(factory.getValidator().validate(dto), is(empty()));
    }
  }

  @Test
  void shouldRejectOneByteOverImageUploadDtoThroughBeanValidation() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      NoteImageUploadDTO dto = new NoteImageUploadDTO();
      dto.setUploadImage(
          new MockMultipartFile(
              "uploadImage", "over.png", "image/png", new byte[10 * 1024 * 1024 + 1]));
      assertThat(factory.getValidator().validate(dto), is(not(empty())));
    }
  }
}
