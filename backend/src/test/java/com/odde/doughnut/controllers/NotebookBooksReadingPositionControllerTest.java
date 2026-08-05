package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.BookLastReadPositionRequest;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.book.ContentLocator;
import com.odde.doughnut.services.book.EpubLocator;
import com.odde.doughnut.services.book.PdfLocator;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
          nb, attachRequest(node("Chapter 1", node("Section 1.1"))), pdfFile(STUB_PDF_BYTES));
      int secondBlockId = blocksByLayoutOrder(bookOf(nb)).get(1).getId();

      controller.patchReadingPosition(nb, lastReadBody(3, 420, secondBlockId));

      var stored =
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookOf(nb).getId())
              .orElseThrow();
      assertThat(stored.getSelectedBookBlockId(), equalTo(secondBlockId));
    }

    @Test
    void patchRejectsBlockIdFromAnotherNotebookBook() throws Exception {
      Notebook nbA = myNotebook();
      Notebook nbB = myNotebook();
      controller.attachBook(nbA, attachRequest(node("A")), pdfFile(STUB_PDF_BYTES));
      controller.attachBook(nbB, attachRequest(node("B")), pdfFile(STUB_PDF_BYTES));
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
    void persistsEpubLocator() throws Exception {
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
  }
}
