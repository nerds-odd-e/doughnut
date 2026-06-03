package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.AttachBookLayoutNodeRequest;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookBlock;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.book.BookReadingWireConstants;
import com.odde.doughnut.services.book.EpubLocator;
import com.odde.doughnut.services.book.PdfLocator;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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

class NotebookBooksRetrievalControllerTest extends NotebookBooksControllerTestBase {

  @Nested
  class GetBook {
    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = myNotebook();
      assertThrows(ResponseStatusException.class, () -> controller.getBook(nb));
    }

    @Test
    void getBookReturnsBookWithNonBlankSourceFileRef() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      String ref = controller.getBook(nb).getSourceFileRef();
      assertThat(ref, notNullValue());
      assertThat(ref.isBlank(), equalTo(false));
    }

    @Test
    void doesNotReturnAnotherNotebooksBook() {
      notebookWithBook();
      Notebook nb2 = myNotebook();
      assertThrows(ResponseStatusException.class, () -> controller.getBook(nb2));
    }

    @Test
    void pdfContentLocatorsDeriveStartAnchorFromFirstContentBlock() throws Exception {
      Notebook nb = myNotebook();
      AttachBookLayoutNodeRequest n = new AttachBookLayoutNodeRequest();
      n.setTitle("Headed Section");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(headingBlock("Headed Section", 1, 1, List.of(5.0, 10.0, 200.0, 50.0)))));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      BookBlock block = rootBlocksSorted(controller.getBook(nb)).getFirst();
      assertThat(block.getContentLocators(), hasSize(1));
      PdfLocator first = (PdfLocator) block.getContentLocators().getFirst();
      assertThat(first.pageIndex(), equalTo(1));
      assertThat(first.bbox(), equalTo(List.of(5.0, 10.0, 200.0, 50.0)));
    }

    @Test
    void pdfContentLocatorsIncludeHeadingThenBody() throws Exception {
      Notebook nb = myNotebook();
      java.util.Map<String, Object> bodyItem = new java.util.LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "Body paragraph");
      bodyItem.put("page_idx", 2);
      bodyItem.put("bbox", new ArrayList<>(List.of(10.0, 20.0, 300.0, 400.0)));
      AttachBookLayoutNodeRequest n = new AttachBookLayoutNodeRequest();
      n.setTitle("Section With Bbox");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(
                  headingBlock("Section With Bbox", 1, 2, List.of(1.0, 2.0, 100.0, 15.0)),
                  bodyItem)));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      BookBlock block = rootBlocksSorted(controller.getBook(nb)).getFirst();
      assertThat(block.getContentLocators(), hasSize(2));
      PdfLocator loc0 = (PdfLocator) block.getContentLocators().get(0);
      PdfLocator loc1 = (PdfLocator) block.getContentLocators().get(1);
      assertThat(loc0.pageIndex(), equalTo(2));
      assertThat(loc0.bbox(), equalTo(List.of(1.0, 2.0, 100.0, 15.0)));
      assertThat(loc1.pageIndex(), equalTo(2));
      assertThat(loc1.bbox(), equalTo(List.of(10.0, 20.0, 300.0, 400.0)));
    }

    @Test
    void pdfContentLocatorsSkipHeaderFooterPageChromeAndStructuralHeadingsInBodyBlocks()
        throws Exception {
      Notebook nb = myNotebook();
      java.util.Map<String, Object> header = new java.util.LinkedHashMap<>();
      header.put("type", "header");
      header.put("text", "Running title");
      header.put("page_idx", 2);
      header.put("bbox", new ArrayList<>(List.of(1.0, 1.0, 50.0, 10.0)));
      java.util.Map<String, Object> footer = new java.util.LinkedHashMap<>();
      footer.put("type", "footer");
      footer.put("text", "copyright");
      footer.put("page_idx", 2);
      footer.put("bbox", new ArrayList<>(List.of(1.0, 500.0, 50.0, 510.0)));
      java.util.Map<String, Object> pageNum = new java.util.LinkedHashMap<>();
      pageNum.put("type", "page_number");
      pageNum.put("text", "7");
      pageNum.put("page_idx", 2);
      pageNum.put("bbox", new ArrayList<>(List.of(400.0, 500.0, 410.0, 510.0)));
      java.util.Map<String, Object> subHeading = new java.util.LinkedHashMap<>();
      subHeading.put("type", "text");
      subHeading.put("text_level", 2);
      subHeading.put("text", "2.1 Section");
      subHeading.put("page_idx", 2);
      subHeading.put("bbox", new ArrayList<>(List.of(15.0, 30.0, 200.0, 45.0)));
      java.util.Map<String, Object> bodyItem = new java.util.LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "Body paragraph");
      bodyItem.put("page_idx", 2);
      bodyItem.put("bbox", new ArrayList<>(List.of(10.0, 20.0, 300.0, 400.0)));
      AttachBookLayoutNodeRequest n = new AttachBookLayoutNodeRequest();
      n.setTitle("Section With Noise");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(
                  headingBlock("Section With Noise", 1, 2, List.of(1.0, 2.0, 100.0, 15.0)),
                  header,
                  footer,
                  pageNum,
                  subHeading,
                  bodyItem)));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      BookBlock block = rootBlocksSorted(controller.getBook(nb)).getFirst();
      assertThat(block.getContentLocators(), hasSize(2));
      PdfLocator noise0 = (PdfLocator) block.getContentLocators().get(0);
      PdfLocator noise1 = (PdfLocator) block.getContentLocators().get(1);
      assertThat(noise0.bbox(), equalTo(List.of(1.0, 2.0, 100.0, 15.0)));
      assertThat(noise1.bbox(), equalTo(List.of(10.0, 20.0, 300.0, 400.0)));
    }

    @Test
    void pdfContentLocatorsMatchContentBlockBboxes() throws Exception {
      Notebook nb = myNotebook();
      java.util.Map<String, Object> bodyItem = new java.util.LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "Body paragraph");
      bodyItem.put("page_idx", 2);
      bodyItem.put("bbox", new ArrayList<>(List.of(10.0, 20.0, 300.0, 400.0)));
      AttachBookLayoutNodeRequest n = new AttachBookLayoutNodeRequest();
      n.setTitle("Section With Bbox");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(
                  headingBlock("Section With Bbox", 1, 2, List.of(1.0, 2.0, 100.0, 15.0)),
                  bodyItem)));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      BookBlock block = rootBlocksSorted(controller.getBook(nb)).getFirst();
      assertThat(block.getContentLocators(), hasSize(2));
      PdfLocator anchor = (PdfLocator) block.getContentLocators().get(0);
      assertThat(anchor.pageIndex(), equalTo(2));
      assertThat(anchor.bbox(), equalTo(List.of(1.0, 2.0, 100.0, 15.0)));
      PdfLocator body = (PdfLocator) block.getContentLocators().get(1);
      assertThat(body.pageIndex(), equalTo(2));
      assertThat(body.bbox(), equalTo(List.of(10.0, 20.0, 300.0, 400.0)));
    }

    @Test
    void epubContentLocatorsMatchAnchorAndDirectContentOnFixture() throws Exception {
      Notebook nb = myNotebook();
      byte[] epubBytes = readFixtureEpubValidMinimal();
      controller.attachBook(nb, epubAttachRequest("Minimal EPUB"), epubFile(epubBytes));
      makeMe.entityPersister.flushAndClear();

      Book detail = controller.getBook(nb);
      List<BookBlock> preorder = blocksByLayoutOrder(detail);

      BookBlock partOne = preorder.getFirst();
      assertThat(partOne.getContentLocators(), hasSize(1));
      assertThat(partOne.getContentLocators().getFirst(), instanceOf(EpubLocator.class));
      EpubLocator partOneAnchor = (EpubLocator) partOne.getContentLocators().getFirst();
      assertThat(partOneAnchor.href(), equalTo("OEBPS/chapter1.xhtml"));
      assertThat(partOneAnchor.fragment(), nullValue());

      BookBlock chapterBeta = preorder.get(2);
      assertThat(chapterBeta.getStructuralTitle(), equalTo("Chapter Beta"));
      assertThat(chapterBeta.getContentLocators(), hasSize(2));
      assertThat(chapterBeta.getContentLocators().getFirst(), instanceOf(EpubLocator.class));
      EpubLocator betaFirst = (EpubLocator) chapterBeta.getContentLocators().getFirst();
      assertThat(betaFirst.href(), equalTo("OEBPS/chapter3.xhtml"));
      assertThat(betaFirst.fragment(), nullValue());
      assertThat(chapterBeta.getContentLocators().get(1), instanceOf(EpubLocator.class));
      EpubLocator betaTable = (EpubLocator) chapterBeta.getContentLocators().get(1);
      assertThat(betaTable.href(), equalTo("OEBPS/chapter3.xhtml"));
      assertThat(betaTable.fragment(), equalTo("beta-table"));
    }
  }

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
    void returnsEpubZipWhenBookFormatIsEpub() throws Exception {
      Notebook nb = myNotebook();
      byte[] epubBytes = readFixtureEpubValidMinimal();
      controller.attachBook(nb, epubAttachRequest("Minimal EPUB"), epubFile(epubBytes));
      makeMe.entityPersister.flushAndClear();
      String ref = bookOf(nb).getSourceFileRef();
      String expectedEtag =
          "\"" + DigestUtils.md5DigestAsHex(ref.getBytes(StandardCharsets.UTF_8)) + "\"";

      ResponseEntity<byte[]> res = controller.getBookFile(webRequest(), nb);

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
      book.setBookName("Minimal EPUB");
      book.setSourceFileRef(ref);
      makeMe.entityPersister.save(book);
      makeMe.entityPersister.flush();

      controller.deleteBook(nb);

      assertThat(bookRepository.findByNotebook_Id(nb.getId()).isEmpty(), equalTo(true));
      assertThat(bookStorage.get(ref).isEmpty(), equalTo(true));
      assertThrows(ResponseStatusException.class, () -> controller.getBook(nb));
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(webRequest(), nb));
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
