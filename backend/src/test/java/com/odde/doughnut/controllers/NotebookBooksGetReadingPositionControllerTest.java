package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.controllers.dto.BookUserLastReadPositionResponse;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.book.EpubLocator;
import com.odde.doughnut.services.book.PdfLocator;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class NotebookBooksGetReadingPositionControllerTest extends NotebookBooksControllerTestBase {

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
    }

    @Test
    void returnsSelectedBookBlockIdWithoutEntityInJson() throws Exception {
      Notebook nb = myNotebook();
      controller.attachBook(
          nb, attachRequest(node("Chapter 1", node("Section 1.1"))), pdfFile(STUB_PDF_BYTES));
      int secondBlockId = blocksByLayoutOrder(bookOf(nb)).get(1).getId();
      controller.patchReadingPosition(nb, lastReadBody(1, 200, secondBlockId));

      ResponseEntity<BookUserLastReadPositionResponse> res = controller.getReadingPosition(nb);

      assertThat(res.getBody().selectedBookBlockId(), equalTo(secondBlockId));
      JsonNode json = objectMapper.valueToTree(res.getBody());
      assertThat(json.has("locator"), is(true));
      assertThat(json.has("selectedBookBlock"), is(false));
    }

    @Test
    void returnsSavedEpubLocatorAfterPatch() throws Exception {
      Notebook nb = notebookWithBook();
      controller.patchReadingPosition(
          nb, lastReadEpubBody("OEBPS/chapter2.xhtml#section-beta-two"));

      ResponseEntity<BookUserLastReadPositionResponse> res = controller.getReadingPosition(nb);

      assertThat(
          res.getBody().locator(),
          equalTo(new EpubLocator("OEBPS/chapter2.xhtml", "section-beta-two")));
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
