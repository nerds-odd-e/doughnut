package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookGit.NotebookGitLfsPointer;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
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

class NoteAttachmentImageControllerTest extends ControllerTestBase {
  @Autowired NoteAttachmentImageController controller;
  @Autowired NotebookAttachmentContent notebookAttachmentContent;
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

  private HttpStatus refusal(String path) {
    return (HttpStatus)
        assertThrows(
                ResponseStatusException.class, () -> controller.showAttachmentImage(force, path))
            .getStatusCode();
  }

  @Test
  void servesAPictureInTheNotesFolderInlineAndNeverSniffed() throws Exception {
    file(physics, "force-diagram.png", png);

    ResponseEntity<byte[]> response = controller.showAttachmentImage(force, "force-diagram.png");

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

    assertThat(controller.showAttachmentImage(force, "images/force.png").getBody(), equalTo(png));
  }

  @Test
  void jpegExtensionIsMatchedCaseInsensitively() throws Exception {
    file(physics, "photo.JPEG", png);

    assertThat(
        controller.showAttachmentImage(force, "photo.JPEG").getHeaders().getContentType(),
        equalTo(MediaType.IMAGE_JPEG));
  }

  @Test
  void svgIsRefusedAsUnsupported() {
    file(physics, "drawing.svg", "<svg><script/></svg>".getBytes(StandardCharsets.UTF_8));

    assertThat(refusal("drawing.svg"), equalTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
  }

  @ParameterizedTest
  @ValueSource(strings = {"missing.png", "../x.png", "/x.png", "./force-diagram.png"})
  void pathsNotEqualToAFileInTheNotebookAreNotFound(String path) {
    file(physics, "force-diagram.png", png);
    makeMe.anAttachment("x.png").atRootOf(notebook).content(png).please();

    assertThat(refusal(path), equalTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void lfsNotebookServesTheStoredBytesNotThePointer() throws Exception {
    makeMe
        .aGitBindingFor(notebook)
        .representation(NotebookGitAttachmentRepresentation.LFS)
        .please();
    String oid = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png));
    file(physics, "force-diagram.png", NotebookGitLfsPointer.format(oid, png.length));
    notebookAttachmentContent.store(
        notebook.getId(), oid, png.length, new ByteArrayInputStream(png));

    assertThat(controller.showAttachmentImage(force, "force-diagram.png").getBody(), equalTo(png));
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

      assertThat(
          controller.showAttachmentImage(force, "force-diagram.png").getBody(), equalTo(png));
    }

    @Test
    void nonReaderIsRefused() {
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.showAttachmentImage(force, "force-diagram.png"));
    }
  }
}
