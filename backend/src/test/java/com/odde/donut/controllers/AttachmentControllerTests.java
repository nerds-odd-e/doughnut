package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Image;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class AttachmentControllerTests extends ControllerTestBase {
  @Autowired AttachmentController controller;
  Note note;
  Image image;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    image = makeMe.anImage().forNote(note).please();
  }

  @Test
  void imageDownload() throws UnexpectedNoAccessRightException {
    ResponseEntity<byte[]> resp = controller.showImage(image, "filename");
    assertThat(resp.getStatusCode(), equalTo(HttpStatus.OK));
    assertThat(resp.getHeaders().getContentType().toString(), equalTo("image/png"));
    assertThat(
        resp.getHeaders().getContentDisposition().toString(),
        equalTo("inline; filename=\"example.png\""));
  }

  @Test
  void refusesImageOfAnotherUsersPrivateNotebook() {
    currentUser.setUser(makeMe.aUser().please());
    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.showImage(image, "filename"));
  }

  @Test
  void refusesPrivateImageWhenNotLoggedIn() {
    currentUser.setUser(null);
    ResponseStatusException exception =
        assertThrows(ResponseStatusException.class, () -> controller.showImage(image, "filename"));
    assertThat(exception.getStatusCode(), equalTo(HttpStatus.UNAUTHORIZED));
  }

  @Test
  void servesBazaarImageWhenNotLoggedIn() throws UnexpectedNoAccessRightException {
    makeMe.aBazaarNotebook(note.getNotebook()).please();
    currentUser.setUser(null);
    assertThat(controller.showImage(image, "filename").getStatusCode(), equalTo(HttpStatus.OK));
  }
}
