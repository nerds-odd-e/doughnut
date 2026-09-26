package com.odde.donut.controllers;

import static com.odde.donut.services.book.BookReadingWireConstants.BOOK_FORMAT_EPUB;
import static com.odde.donut.services.book.BookReadingWireConstants.BOOK_FORMAT_PDF;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Book;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.BookRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class BooksControllerTest extends ControllerTestBase {

  @Autowired BooksController booksController;
  @Autowired BookRepository bookRepository;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private Notebook notebookWithBook() {
    return makeMe
        .aNotebook()
        .creatorAndOwner(currentUser.getUser())
        .withBook("Linear Algebra")
        .please();
  }

  private Book bookOf(Notebook nb) {
    return bookRepository.findByNotebook_Id(nb.getId()).orElseThrow();
  }

  private Book bookWithFile(String bookName, String format, byte[] fileBytes) {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    return makeMe
        .aBook()
        .notebook(nb)
        .bookName(bookName)
        .format(format)
        .fileBytes(fileBytes)
        .please();
  }

  private static ServletWebRequest webRequest() {
    return new ServletWebRequest(new MockHttpServletRequest());
  }

  @Nested
  class GetBookFileByBook {
    @Test
    void rejectsNotebookWithoutReadAccess() {
      Notebook otherNb =
          makeMe
              .aNotebook()
              .creatorAndOwner(makeMe.aUser().please())
              .withBook("Linear Algebra")
              .please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> booksController.getBookFile(webRequest(), bookOf(otherNb)));
    }

    @Test
    void returnsThePdfFromTheBooksNotebookFile() throws UnexpectedNoAccessRightException {
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};
      Book book = bookWithFile("Linear Algebra", BOOK_FORMAT_PDF, pdfBytes);

      ResponseEntity<byte[]> res = booksController.getBookFile(webRequest(), book);

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
      Book book = bookWithFile("Minimal EPUB", BOOK_FORMAT_EPUB, epubBytes);

      ResponseEntity<byte[]> res = booksController.getBookFile(webRequest(), book);

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
      Book book = bookOf(notebookWithBook());
      String etag = booksController.getBookFile(webRequest(), book).getHeaders().getETag();

      MockHttpServletRequest req = new MockHttpServletRequest();
      req.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
      ResponseEntity<byte[]> res = booksController.getBookFile(new ServletWebRequest(req), book);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.NOT_MODIFIED));
      assertThat(res.getBody(), nullValue());
      assertThat(res.getHeaders().getETag(), equalTo(etag));
    }
  }
}
