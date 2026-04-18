package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.AttachBookRequest;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.BookRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.book.BookReadingWireConstants;
import com.odde.doughnut.services.book.BookStorage;
import com.openai.client.OpenAIClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

class BooksControllerTest extends ControllerTestBase {

  @Autowired BooksController booksController;
  @Autowired NotebookBooksController notebookBooksController;
  @Autowired BookRepository bookRepository;
  @Autowired BookStorage bookStorage;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialOpenAiClient;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private Notebook myNotebook() {
    return makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
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

  private void setSourceFileRef(Notebook nb, String ref) {
    Book book = bookOf(nb);
    book.setSourceFileRef(ref);
    makeMe.entityPersister.save(book);
    makeMe.entityPersister.flush();
  }

  private Notebook otherUsersNotebookWithBook() {
    User other = makeMe.aUser().please();
    return makeMe.aNotebook().creatorAndOwner(other).withBook("Linear Algebra").please();
  }

  private static MultipartFile epubFile(byte[] content) {
    return new MockMultipartFile("file", "book.epub", "application/epub+zip", content);
  }

  private static byte[] readFixtureEpubValidMinimal() throws Exception {
    Path epubPath =
        Path.of("..", "e2e_test", "fixtures", "book_reading", "epub_valid_minimal.epub");
    return Files.readAllBytes(epubPath);
  }

  private static AttachBookRequest epubAttachRequest(String bookName) {
    AttachBookRequest r = new AttachBookRequest();
    r.setBookName(bookName);
    r.setFormat(BookReadingWireConstants.BOOK_FORMAT_EPUB);
    return r;
  }

  private static ServletWebRequest webRequest() {
    return new ServletWebRequest(new MockHttpServletRequest());
  }

  @Nested
  class GetBookFileByBook {
    @Test
    void rejectsNotebookWithoutReadAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();
      Book book = bookOf(otherNb);
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> booksController.getBookFile(webRequest(), book));
    }

    @Test
    void returnsPdfWhenSourceFileRefPointsAtBlob() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      Book book = bookOf(nb);
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};
      String ref = bookStorage.put(pdfBytes, "pdf");
      setSourceFileRef(nb, ref);
      String expectedEtag =
          "\"" + DigestUtils.md5DigestAsHex(ref.getBytes(StandardCharsets.UTF_8)) + "\"";

      ResponseEntity<byte[]> res = booksController.getBookFile(webRequest(), book);

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
    void returnsEpubZipWhenBookFormatIsEpub() throws Exception {
      Notebook nb = myNotebook();
      byte[] epubBytes = readFixtureEpubValidMinimal();
      notebookBooksController.attachBook(
          nb, epubAttachRequest("Minimal EPUB"), epubFile(epubBytes));
      makeMe.entityPersister.flushAndClear();
      Book book = bookOf(nb);
      String ref = book.getSourceFileRef();
      String expectedEtag =
          "\"" + DigestUtils.md5DigestAsHex(ref.getBytes(StandardCharsets.UTF_8)) + "\"";

      ResponseEntity<byte[]> res = booksController.getBookFile(webRequest(), book);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(res.getBody(), equalTo(epubBytes));
      assertThat(
          res.getHeaders().getContentType(),
          equalTo(MediaType.parseMediaType("application/epub+zip")));
      assertThat(res.getHeaders().getETag(), equalTo(expectedEtag));
      assertThat(
          res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION),
          equalTo("inline; filename=\"Minimal EPUB.epub\""));
      assertThat(res.getHeaders().getCacheControl(), containsString("no-store"));
    }

    @Test
    void returns304WhenIfNoneMatchMatchesEtag() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      Book book = bookOf(nb);
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};
      String ref = bookStorage.put(pdfBytes, "pdf");
      setSourceFileRef(nb, ref);
      String etag = "\"" + DigestUtils.md5DigestAsHex(ref.getBytes(StandardCharsets.UTF_8)) + "\"";

      MockHttpServletRequest req = new MockHttpServletRequest();
      req.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
      ResponseEntity<byte[]> res = booksController.getBookFile(new ServletWebRequest(req), book);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.NOT_MODIFIED));
      assertThat(res.getBody(), nullValue());
      assertThat(res.getHeaders().getETag(), equalTo(etag));
    }

    @Test
    void returns404WhenSourceFileRefBlobMissing() {
      Notebook nb = notebookWithBook();
      Book book = bookOf(nb);
      setSourceFileRef(nb, String.valueOf(Integer.MAX_VALUE));
      assertThrows(
          ResponseStatusException.class, () -> booksController.getBookFile(webRequest(), book));
    }
  }
}
