package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Book;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.book.BookReadingWireConstants;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.DigestUtils;
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
    void returnsPdfWhenSourceFileRefPointsAtBlob() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};
      String ref = bookStorage.put(pdfBytes, "pdf");
      setSourceFileRef(nb, ref);
      String expectedEtag =
          "\"" + DigestUtils.md5DigestAsHex(ref.getBytes(StandardCharsets.UTF_8)) + "\"";

      ResponseEntity<byte[]> res = controller.getBookFile(webRequest(), nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(res.getBody(), equalTo(pdfBytes));
      assertThat(res.getHeaders().getContentType(), equalTo(MediaType.APPLICATION_PDF));
      assertThat(res.getHeaders().getETag(), equalTo(expectedEtag));
      assertThat(
          res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION),
          equalTo("inline; filename=\"Linear Algebra.pdf\""));
      assertThat(res.getHeaders().getCacheControl(), containsString("no-store"));
    }

    @Test
    void returnsEpubZipWhenBookFormatIsEpub() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      Book book = bookOf(nb);
      byte[] epubBytes = new byte[] {0x50, 0x4b, 0x03, 0x04};
      String ref = bookStorage.put(epubBytes, BookReadingWireConstants.BOOK_FORMAT_EPUB);
      book.setFormat(BookReadingWireConstants.BOOK_FORMAT_EPUB);
      book.setBookName("Minimal EPUB");
      book.setSourceFileRef(ref);
      makeMe.entityPersister.save(book);
      makeMe.entityPersister.flush();

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
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};
      String ref = bookStorage.put(pdfBytes, "pdf");
      setSourceFileRef(nb, ref);
      String etag = "\"" + DigestUtils.md5DigestAsHex(ref.getBytes(StandardCharsets.UTF_8)) + "\"";

      MockHttpServletRequest req = new MockHttpServletRequest();
      req.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
      ResponseEntity<byte[]> res = controller.getBookFile(new ServletWebRequest(req), nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.NOT_MODIFIED));
      assertThat(res.getBody(), nullValue());
      assertThat(res.getHeaders().getETag(), equalTo(etag));
    }

    @Test
    void returns404WhenSourceFileRefIsNotNumeric() {
      Notebook nb = notebookWithBook();
      setSourceFileRef(nb, "not-an-id");
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(webRequest(), nb));
    }

    @Test
    void returns404WhenSourceFileRefBlobMissing() {
      Notebook nb = notebookWithBook();
      setSourceFileRef(nb, String.valueOf(Integer.MAX_VALUE));
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(webRequest(), nb));
    }
  }

  @Nested
  class DeleteBook {
    @Test
    void removesBookRowAndStoredBytes() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      String ref = bookOf(nb).getSourceFileRef();

      controller.deleteBook(nb);

      assertThat(bookRepository.findByNotebook_Id(nb.getId()).isEmpty(), equalTo(true));
      assertThat(bookStorage.get(ref).isEmpty(), equalTo(true));
      assertThrows(ResponseStatusException.class, () -> controller.getBook(nb));
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(webRequest(), nb));
    }

    @Test
    void removesEpubBookRowAndStoredBytes() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      Book book = bookOf(nb);
      byte[] epubBytes = new byte[] {0x50, 0x4b, 0x03, 0x04};
      String ref = bookStorage.put(epubBytes, BookReadingWireConstants.BOOK_FORMAT_EPUB);
      book.setFormat(BookReadingWireConstants.BOOK_FORMAT_EPUB);
      book.setSourceFileRef(ref);
      makeMe.entityPersister.save(book);
      makeMe.entityPersister.flush();

      controller.deleteBook(nb);

      assertThat(bookRepository.findByNotebook_Id(nb.getId()).isEmpty(), equalTo(true));
      assertThat(bookStorage.get(ref).isEmpty(), equalTo(true));
    }

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
