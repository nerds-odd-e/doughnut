package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NotebookAttachmentRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class NotebookAttachmentControllerTest extends ControllerTestBase {
  @Autowired NotebookAttachmentController controller;
  Notebook notebook;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
  }

  private NotebookAttachment attachmentAtRoot(String filename, String content) {
    return makeMe
        .anAttachment(filename)
        .atRootOf(notebook)
        .content(content.getBytes(StandardCharsets.UTF_8))
        .please();
  }

  private static ContentDisposition disposition(ResponseEntity<byte[]> response) {
    return response.getHeaders().getContentDisposition();
  }

  @Nested
  class FilePage {
    @Test
    void showsFilenameSizeAndFolderTrailThroughItsFolder() throws Exception {
      Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
      Folder data = makeMe.aFolder().parentFolder(physics).name("data").please();
      NotebookAttachment runJson =
          makeMe
              .anAttachment("run.json")
              .in(data)
              .content("{\"a\":1}".getBytes(StandardCharsets.UTF_8))
              .please();

      NotebookAttachmentRealm page = controller.getAttachmentPage(notebook, runJson);

      assertThat(page.attachment().filename(), equalTo("run.json"));
      assertThat(page.size(), equalTo(7L));
      assertThat(
          page.sidebar().getAncestorFolders().stream().map(Folder::getName).toList(),
          contains("physics", "data"));
      assertThat(page.sidebar().getNotebookRealm().notebook().getId(), equalTo(notebook.getId()));
    }

    @Test
    void rootFileHasNoFolderTrail() throws Exception {
      NotebookAttachment keep = attachmentAtRoot(".keep", "");

      assertThat(
          controller.getAttachmentPage(notebook, keep).sidebar().getAncestorFolders(), empty());
    }
  }

  @Nested
  class Download {
    @Test
    void returnsExactBytesAsAnOctetStreamAttachmentThatIsNeverSniffed() throws Exception {
      NotebookAttachment runJson = attachmentAtRoot("run.json", "{\"a\":1}");

      ResponseEntity<byte[]> response = controller.downloadAttachment(notebook, runJson);

      assertThat(new String(response.getBody(), StandardCharsets.UTF_8), equalTo("{\"a\":1}"));
      assertThat(
          response.getHeaders().getContentType(), equalTo(MediaType.APPLICATION_OCTET_STREAM));
      assertThat(response.getHeaders().getFirst("X-Content-Type-Options"), equalTo("nosniff"));
      assertThat(disposition(response).isAttachment(), equalTo(true));
      assertThat(disposition(response).getFilename(), equalTo("run.json"));
    }

    @Test
    void svgIsDownloadedAsAnAttachmentNotRenderedInline() throws Exception {
      NotebookAttachment svg = attachmentAtRoot("drawing.svg", "<svg><script/></svg>");

      ResponseEntity<byte[]> response = controller.downloadAttachment(notebook, svg);

      assertThat(disposition(response).isAttachment(), equalTo(true));
      assertThat(response.getHeaders().getFirst("X-Content-Type-Options"), equalTo("nosniff"));
    }

    @Test
    void nonAsciiFilenameIsSentAsUtf8() throws Exception {
      NotebookAttachment file = attachmentAtRoot("力學 résumé.json", "{}");

      ResponseEntity<byte[]> response = controller.downloadAttachment(notebook, file);

      assertThat(
          response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION),
          containsString("filename*=UTF-8''"));
      assertThat(disposition(response).getFilename(), equalTo("力學 résumé.json"));
    }

    @Test
    void zeroByteFileDownloadsEmpty() throws Exception {
      NotebookAttachment keep = attachmentAtRoot(".keep", "");

      ResponseEntity<byte[]> response = controller.downloadAttachment(notebook, keep);

      assertThat(response.getBody().length, equalTo(0));
      assertThat(disposition(response).getFilename(), equalTo(".keep"));
    }
  }

  @Nested
  class LfsNotebook {
    @Autowired NotebookAttachmentContent notebookAttachmentContent;
    final byte[] png = "real png bytes".getBytes(StandardCharsets.UTF_8);
    String oid;
    NotebookAttachment diagram;

    @BeforeEach
    void lfsNotebookWithAPointerRow() throws Exception {
      makeMe
          .aGitBindingFor(notebook)
          .representation(NotebookGitAttachmentRepresentation.LFS)
          .please();
      oid = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png));
      diagram =
          makeMe
              .anAttachment("diagram.png")
              .atRootOf(notebook)
              .content(NotebookGitLfsPointer.format(oid, png.length))
              .please();
    }

    private void storeThePng() throws Exception {
      notebookAttachmentContent.store(
          notebook.getId(), oid, png.length, new ByteArrayInputStream(png));
    }

    @Test
    void pageShowsThePointersSizeWithoutNeedingTheBytes() throws Exception {
      assertThat(
          controller.getAttachmentPage(notebook, diagram).size(), equalTo((long) png.length));
    }

    @Test
    void downloadReturnsTheStoredBytesNotThePointer() throws Exception {
      storeThePng();

      assertThat(controller.downloadAttachment(notebook, diagram).getBody(), equalTo(png));
    }

    @Test
    void missingBytesFailLoudlyNamingTheFile() {
      IllegalStateException error =
          assertThrows(
              IllegalStateException.class, () -> controller.downloadAttachment(notebook, diagram));

      assertThat(error.getMessage(), equalTo("File content unavailable: diagram.png"));
    }

    @Test
    void zeroByteFileDownloadsEmpty() throws Exception {
      NotebookAttachment keep = attachmentAtRoot(".keep", "");

      assertThat(controller.downloadAttachment(notebook, keep).getBody().length, equalTo(0));
      assertThat(controller.getAttachmentPage(notebook, keep).size(), equalTo(0L));
    }
  }

  @Test
  void rawNotebookServesPointerLookingContentUnchanged() throws Exception {
    makeMe
        .aGitBindingFor(notebook)
        .representation(NotebookGitAttachmentRepresentation.RAW)
        .please();
    byte[] pointerText = NotebookGitLfsPointer.format("a".repeat(64), 99);
    NotebookAttachment file =
        makeMe.anAttachment("pointer.txt").atRootOf(notebook).content(pointerText).please();

    assertThat(controller.downloadAttachment(notebook, file).getBody(), equalTo(pointerText));
    assertThat(
        controller.getAttachmentPage(notebook, file).size(), equalTo((long) pointerText.length));
  }

  @Nested
  class Access {
    NotebookAttachment othersFile;

    @BeforeEach
    void othersNotebook() {
      notebook = makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
      othersFile = attachmentAtRoot("run.json", "{}");
    }

    @Test
    void bazaarReaderCanSeeThePageAndDownload() throws Exception {
      makeMe.aBazaarNotebook(notebook).please();

      assertThat(controller.getAttachmentPage(notebook, othersFile).size(), equalTo(2L));
      assertThat(controller.downloadAttachment(notebook, othersFile).getBody().length, equalTo(2));
    }

    @Test
    void nonReaderGetsNeitherPageNorDownload() {
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.getAttachmentPage(notebook, othersFile));
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.downloadAttachment(notebook, othersFile));
    }

    @Test
    void fileFromAnotherNotebookIsRefused() {
      Notebook mine = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();

      assertThat(
          assertThrows(
                  ResponseStatusException.class,
                  () -> controller.getAttachmentPage(mine, othersFile))
              .getStatusCode(),
          equalTo(HttpStatus.NOT_FOUND));
      assertThat(
          assertThrows(
                  ResponseStatusException.class,
                  () -> controller.downloadAttachment(mine, othersFile))
              .getStatusCode(),
          equalTo(HttpStatus.NOT_FOUND));
    }
  }
}
