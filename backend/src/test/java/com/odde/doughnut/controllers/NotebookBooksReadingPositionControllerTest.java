package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.BookLastReadPositionRequest;
import com.odde.doughnut.controllers.dto.BookUserLastReadPositionResponse;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.book.ContentLocator;
import com.odde.doughnut.services.book.EpubLocator;
import com.odde.doughnut.services.book.PdfLocator;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class NotebookBooksReadingPositionControllerTest extends NotebookBooksControllerTestBase {

  @Nested
  class PatchReadingPosition {
    @Test
    void persistsSnapshotForCurrentUserAndBook() throws Exception {
      Notebook nb = notebookWithBook();

      controller.patchReadingPosition(nb, lastReadBody(2, 750));

      var stored =
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookOf(nb).getId())
              .orElseThrow();
      ContentLocator fromJson =
          objectMapper.readValue(stored.getReadingPositionLocatorJson(), ContentLocator.class);
      assertThat(
          fromJson, equalTo(new PdfLocator(2, List.of(0.0, 750.0, 100.0, 600.0), null, null)));
    }

    @Test
    void secondPatchUpdatesSameRow() throws Exception {
      Notebook nb = notebookWithBook();

      controller.patchReadingPosition(nb, lastReadBody(0, 100));
      controller.patchReadingPosition(nb, lastReadBody(5, 0));

      assertThat(bookUserLastReadPositionRepository.count(), equalTo(1L));
      var stored =
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookOf(nb).getId())
              .orElseThrow();
      ContentLocator fromJson =
          objectMapper.readValue(stored.getReadingPositionLocatorJson(), ContentLocator.class);
      assertThat(fromJson, equalTo(new PdfLocator(5, List.of(0.0, 0.0, 100.0, 600.0), null, null)));
    }

    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = myNotebook();
      assertThrows(
          ResponseStatusException.class,
          () -> controller.patchReadingPosition(nb, lastReadBody(0, 0)));
    }

    @Test
    void rejectsNotebookWithoutReadAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.patchReadingPosition(otherNb, lastReadBody(0, 0)));
    }

    @Test
    void requiresLoggedInUser() {
      Notebook nb = notebookWithBook();
      currentUser.setUser(null);
      assertThrows(
          ResponseStatusException.class,
          () -> controller.patchReadingPosition(nb, lastReadBody(0, 0)));
    }

    @Test
    void removesPositionWhenBookDeleted() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      int bookId = bookOf(nb).getId();
      controller.patchReadingPosition(nb, lastReadBody(1, 500));

      controller.deleteBook(nb);

      assertThat(
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookId)
              .isEmpty(),
          equalTo(true));
    }

    @Test
    void persistsSelectedBookBlockId() throws Exception {
      Notebook nb = myNotebook();
      controller.attachBook(
          nb,
          attachRequest(node("Chapter 1", node("Section 1.1"))),
          pdfFile(new byte[] {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e}));
      int secondBlockId = blocksByLayoutOrder(bookOf(nb)).get(1).getId();

      controller.patchReadingPosition(nb, lastReadBody(3, 420, secondBlockId));

      var stored =
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookOf(nb).getId())
              .orElseThrow();
      assertThat(stored.getSelectedBookBlockId(), equalTo(secondBlockId));
      ContentLocator fromJson =
          objectMapper.readValue(stored.getReadingPositionLocatorJson(), ContentLocator.class);
      assertThat(
          fromJson, equalTo(new PdfLocator(3, List.of(0.0, 420.0, 100.0, 600.0), null, null)));
    }

    @Test
    void patchRejectsBlockIdFromAnotherNotebookBook() throws Exception {
      Notebook nbA = myNotebook();
      Notebook nbB = myNotebook();
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e};
      controller.attachBook(nbA, attachRequest(node("A")), pdfFile(pdfBytes));
      controller.attachBook(nbB, attachRequest(node("B")), pdfFile(pdfBytes));
      int blockFromA = blocksByLayoutOrder(bookOf(nbA)).getFirst().getId();

      assertThrows(
          ResponseStatusException.class,
          () -> controller.patchReadingPosition(nbB, lastReadBody(0, 100, blockFromA)));
    }

    @Test
    void patchWithoutSelectedBookBlockIdLeavesStoredBlockUnchanged() throws Exception {
      Notebook nb = notebookWithBook();
      int blockId = blocksByLayoutOrder(bookOf(nb)).getFirst().getId();
      controller.patchReadingPosition(nb, lastReadBody(1, 500, blockId));
      controller.patchReadingPosition(nb, lastReadBody(2, 600));

      var stored =
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookOf(nb).getId())
              .orElseThrow();
      assertThat(stored.getSelectedBookBlockId(), equalTo(blockId));
      ContentLocator fromJson =
          objectMapper.readValue(stored.getReadingPositionLocatorJson(), ContentLocator.class);
      assertThat(
          fromJson, equalTo(new PdfLocator(2, List.of(0.0, 600.0, 100.0, 600.0), null, null)));
    }

    @Test
    void persistsEpubLocatorAndClearsPdfFields() throws Exception {
      Notebook nb = notebookWithBook();

      controller.patchReadingPosition(
          nb, lastReadEpubBody("OEBPS/chapter2.xhtml#section-beta-two"));

      var stored =
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookOf(nb).getId())
              .orElseThrow();
      ContentLocator fromJson =
          objectMapper.readValue(stored.getReadingPositionLocatorJson(), ContentLocator.class);
      assertThat(fromJson, equalTo(new EpubLocator("OEBPS/chapter2.xhtml", "section-beta-two")));
    }

    @Test
    void rejectsPatchWhenEmptyAndNoStoredLocator() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      BookLastReadPositionRequest empty = new BookLastReadPositionRequest();

      ApiException ex =
          assertThrows(ApiException.class, () -> controller.patchReadingPosition(nb, empty));
      assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.BINDING_ERROR));
    }

    @Test
    void persistsPdfReadingPositionFromPdfLocator() throws Exception {
      Notebook nb = notebookWithBook();
      BookLastReadPositionRequest req = new BookLastReadPositionRequest();
      req.setLocator(new PdfLocator(3, List.of(0.0, 420.0, 100.0, 600.0), null, null));
      controller.patchReadingPosition(nb, req);

      var stored =
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookOf(nb).getId())
              .orElseThrow();
      ContentLocator fromJson =
          objectMapper.readValue(stored.getReadingPositionLocatorJson(), ContentLocator.class);
      assertThat(fromJson, equalTo(req.getLocator()));
    }

    @Test
    void persistsEpubReadingPositionFromEpubLocator() throws Exception {
      Notebook nb = myNotebook();
      byte[] epubBytes = readFixtureEpubValidMinimal();
      controller.attachBook(nb, epubAttachRequest("Minimal EPUB"), epubFile(epubBytes));

      BookLastReadPositionRequest req = new BookLastReadPositionRequest();
      req.setLocator(new EpubLocator("OEBPS/chapter2.xhtml", "section-beta-two"));
      controller.patchReadingPosition(nb, req);

      var stored =
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookOf(nb).getId())
              .orElseThrow();
      ContentLocator fromJson =
          objectMapper.readValue(stored.getReadingPositionLocatorJson(), ContentLocator.class);
      assertThat(fromJson, equalTo(req.getLocator()));
    }

    @Test
    void patchOverwritesReadingPositionLocatorJson() throws Exception {
      Notebook nb = notebookWithBook();
      controller.patchReadingPosition(nb, lastReadBody(1, 100));
      controller.patchReadingPosition(nb, lastReadBody(3, 420));

      var stored =
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookOf(nb).getId())
              .orElseThrow();
      ContentLocator fromJson =
          objectMapper.readValue(stored.getReadingPositionLocatorJson(), ContentLocator.class);
      assertThat(
          fromJson, equalTo(new PdfLocator(3, List.of(0.0, 420.0, 100.0, 600.0), null, null)));

      ResponseEntity<BookUserLastReadPositionResponse> res = controller.getReadingPosition(nb);
      assertThat(res.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(res.getBody(), notNullValue());
      assertThat(res.getBody().locator(), equalTo(fromJson));
    }
  }

  @Nested
  class GetReadingPosition {
    @Test
    void returnsSavedSnapshotAfterPatch() throws Exception {
      Notebook nb = notebookWithBook();
      controller.patchReadingPosition(nb, lastReadBody(3, 420));

      ResponseEntity<BookUserLastReadPositionResponse> res = controller.getReadingPosition(nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(res.getBody(), notNullValue());
      assertThat(
          res.getBody().locator(),
          equalTo(new PdfLocator(3, List.of(0.0, 420.0, 100.0, 600.0), null, null)));

      var row =
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookOf(nb).getId())
              .orElseThrow();
      ContentLocator fromJson =
          objectMapper.readValue(row.getReadingPositionLocatorJson(), ContentLocator.class);
      assertThat(
          fromJson, equalTo(new PdfLocator(3, List.of(0.0, 420.0, 100.0, 600.0), null, null)));
    }

    @Test
    void returnsSelectedBookBlockIdAfterPatch() throws Exception {
      Notebook nb = myNotebook();
      controller.attachBook(
          nb,
          attachRequest(node("Chapter 1", node("Section 1.1"))),
          pdfFile(new byte[] {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e}));
      int secondBlockId = blocksByLayoutOrder(bookOf(nb)).get(1).getId();
      controller.patchReadingPosition(nb, lastReadBody(1, 200, secondBlockId));

      ResponseEntity<BookUserLastReadPositionResponse> res = controller.getReadingPosition(nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(res.getBody(), notNullValue());
      assertThat(res.getBody().selectedBookBlockId(), equalTo(secondBlockId));
    }

    @Test
    void responseBodyExposesOnlySelectedBookBlockIdNotTheEntity() throws Exception {
      Notebook nb = myNotebook();
      controller.attachBook(
          nb,
          attachRequest(node("Chapter 1", node("Section 1.1"))),
          pdfFile(new byte[] {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e}));
      int secondBlockId = blocksByLayoutOrder(bookOf(nb)).get(1).getId();
      controller.patchReadingPosition(nb, lastReadBody(1, 200, secondBlockId));

      ResponseEntity<BookUserLastReadPositionResponse> res = controller.getReadingPosition(nb);

      JsonNode json = objectMapper.valueToTree(res.getBody());
      assertThat(json.get("selectedBookBlockId").asInt(), equalTo(secondBlockId));
      assertThat(json.has("locator"), is(true));
      assertThat(
          "selectedBookBlock entity must not appear in JSON (causes 500 via Hibernate proxy)",
          json.has("selectedBookBlock"),
          is(false));
    }

    @Test
    void returnsSavedEpubLocatorAfterPatch() throws Exception {
      Notebook nb = notebookWithBook();
      controller.patchReadingPosition(
          nb, lastReadEpubBody("OEBPS/chapter2.xhtml#section-beta-two"));

      ResponseEntity<BookUserLastReadPositionResponse> res = controller.getReadingPosition(nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(res.getBody(), notNullValue());
      assertThat(
          res.getBody().locator(),
          equalTo(new EpubLocator("OEBPS/chapter2.xhtml", "section-beta-two")));

      var row =
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookOf(nb).getId())
              .orElseThrow();
      ContentLocator fromJson =
          objectMapper.readValue(row.getReadingPositionLocatorJson(), ContentLocator.class);
      assertThat(fromJson, equalTo(new EpubLocator("OEBPS/chapter2.xhtml", "section-beta-two")));
    }

    @Test
    void returns204WhenNoSnapshotStored() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();

      ResponseEntity<BookUserLastReadPositionResponse> res = controller.getReadingPosition(nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
      assertThat(res.getBody(), nullValue());
    }

    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = myNotebook();
      assertThrows(ResponseStatusException.class, () -> controller.getReadingPosition(nb));
    }

    @Test
    void rejectsNotebookWithoutReadAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.getReadingPosition(otherNb));
    }

    @Test
    void requiresLoggedInUser() {
      Notebook nb = notebookWithBook();
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.getReadingPosition(nb));
    }
  }
}
