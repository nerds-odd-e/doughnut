package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class NoteImageControllerTest extends ControllerTestBase {
  @Autowired NoteImageController controller;
  final byte[] png = "png bytes".getBytes(StandardCharsets.UTF_8);
  Notebook notebook;
  Folder physics;
  Note force;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    force = makeMe.aNote().folder(physics).title("force").please();
  }

  private void file(Folder folder, String filename, byte[] content) {
    makeMe.anAttachment(filename).in(folder).content(content).please();
  }

  private ResponseStatusException refusal(String path) {
    return assertThrows(ResponseStatusException.class, () -> controller.showNoteImage(force, path));
  }

  @Test
  void servesAnImageInTheNotesFolderInlineAndNeverSniffed() throws Exception {
    file(physics, "force-diagram.png", png);

    ResponseEntity<byte[]> response = controller.showNoteImage(force, "force-diagram.png");

    assertThat(response.getBody(), equalTo(png));
    assertThat(response.getHeaders().getContentType(), equalTo(MediaType.IMAGE_PNG));
    assertThat(response.getHeaders().getContentDisposition().isInline(), equalTo(true));
    assertThat(response.getHeaders().getFirst("X-Content-Type-Options"), equalTo("nosniff"));
  }

  @Test
  void resolvesASubfolderPathUnderTheNotesFolder() throws Exception {
    Folder images = makeMe.aFolder().parentFolder(physics).name("images").please();
    file(physics, "force.png", "wrong folder".getBytes(StandardCharsets.UTF_8));
    file(images, "force.png", png);

    assertThat(controller.showNoteImage(force, "images/force.png").getBody(), equalTo(png));
  }

  @Test
  void jpegExtensionIsMatchedCaseInsensitively() throws Exception {
    file(physics, "photo.JPEG", png);

    assertThat(
        controller.showNoteImage(force, "photo.JPEG").getHeaders().getContentType(),
        equalTo(MediaType.IMAGE_JPEG));
  }

  @Test
  void svgIsRefusedAsUnsupported() {
    file(physics, "drawing.svg", "<svg><script/></svg>".getBytes(StandardCharsets.UTF_8));

    ResponseStatusException refusal = refusal("drawing.svg");

    assertThat(refusal.getStatusCode(), equalTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    assertThat(refusal.getReason(), equalTo("Not a raster image."));
  }

  @ParameterizedTest
  @ValueSource(strings = {"missing.png", "../x.png", "/x.png", "./force-diagram.png"})
  void pathsNotEqualToAFileInTheNotebookAreNotFound(String path) {
    file(physics, "force-diagram.png", png);
    makeMe.anAttachment("x.png").atRootOf(notebook).content(png).please();

    assertThat(refusal(path).getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
  }

  @Nested
  class Access {
    @BeforeEach
    void someoneElsesNote() {
      currentUser.setUser(makeMe.aUser().please());
      file(physics, "force-diagram.png", png);
    }

    @Test
    void bazaarReaderIsServed() throws Exception {
      makeMe.aBazaarNotebook(notebook).please();

      assertThat(controller.showNoteImage(force, "force-diagram.png").getBody(), equalTo(png));
    }

    @Test
    void nonReaderIsRefused() {
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.showNoteImage(force, "force-diagram.png"));
    }
  }
}
