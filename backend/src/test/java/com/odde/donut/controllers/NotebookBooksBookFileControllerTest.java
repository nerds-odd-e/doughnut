package com.odde.donut.controllers;

import static com.odde.donut.services.book.BookReadingWireConstants.BOOK_FORMAT_EPUB;
import static com.odde.donut.services.book.BookReadingWireConstants.BOOK_FORMAT_PDF;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Book;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;

class NotebookBooksBookFileControllerTest extends NotebookBooksControllerTestBase {

  @Nested
  class GetBookFile {
    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = myNotebook();
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(webRequest(), nb));
    }

    @Test
    void rejectsNotebookWithoutReadAccess() {
      Notebook otherNb = otherUsersNotebook();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.getBookFile(webRequest(), otherNb));
    }

    @Test
    void returnsThePdfFromTheBooksNotebookFile() throws UnexpectedNoAccessRightException {
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};
      Notebook nb = notebookWithBookFile("Linear Algebra", BOOK_FORMAT_PDF, pdfBytes);

      ResponseEntity<byte[]> res = controller.getBookFile(webRequest(), nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(res.getBody(), equalTo(pdfBytes));
      assertThat(res.getHeaders().getContentType(), equalTo(MediaType.APPLICATION_PDF));
      assertThat(res.getHeaders().getETag(), notNullValue());
      assertThat(
          res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION),
          equalTo("inline; filename=\"Linear Algebra.pdf\""));
      assertThat(res.getHeaders().getCacheControl(), containsString("no-store"));
    }

    @Test
    void returnsEpubZipWhenBookFormatIsEpub() throws UnexpectedNoAccessRightException {
      byte[] epubBytes = new byte[] {0x50, 0x4b, 0x03, 0x04};
      Notebook nb = notebookWithBookFile("Minimal EPUB", BOOK_FORMAT_EPUB, epubBytes);

      ResponseEntity<byte[]> res = controller.getBookFile(webRequest(), nb);

      assertThat(res.getBody(), equalTo(epubBytes));
      assertThat(
          res.getHeaders().getContentType(),
          equalTo(MediaType.parseMediaType("application/epub+zip")));
      assertThat(
          res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION),
          equalTo("inline; filename=\"Minimal EPUB.epub\""));
    }

    @Test
    void returns304WhenIfNoneMatchMatchesEtag() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      String etag = controller.getBookFile(webRequest(), nb).getHeaders().getETag();

      MockHttpServletRequest req = new MockHttpServletRequest();
      req.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
      ResponseEntity<byte[]> res = controller.getBookFile(new ServletWebRequest(req), nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.NOT_MODIFIED));
      assertThat(res.getBody(), nullValue());
      assertThat(res.getHeaders().getETag(), equalTo(etag));
    }

    @Test
    void returns404WhenTheSourcePathNamesNoNotebookFile() {
      Notebook nb = notebookWithBook();
      Book book = bookOf(nb);
      book.setSourceFilePath("Missing.pdf");
      makeMe.entityPersister.save(book);
      makeMe.entityPersister.flush();

      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(webRequest(), nb));
    }
  }

  @Nested
  class DeleteBook {
    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = myNotebook();
      assertThrows(ResponseStatusException.class, () -> controller.deleteBook(nb));
    }

    @Test
    void rejectsUnauthorizedNotebook() {
      Notebook otherNb = otherUsersNotebook();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.deleteBook(otherNb));
    }
  }
}
