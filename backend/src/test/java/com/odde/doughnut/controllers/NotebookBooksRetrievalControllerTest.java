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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
      assertThat(controller.getBook(nb).getSourceFileRef(), not(blankOrNullString()));
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
      AttachBookLayoutNodeRequest n = node("Headed Section");
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
      AttachBookLayoutNodeRequest n = node("Section With Bbox");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(
                  headingBlock("Section With Bbox", 1, 2, List.of(1.0, 2.0, 100.0, 15.0)),
                  textBlock("Body paragraph", 2, List.of(10.0, 20.0, 300.0, 400.0)))));
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
      Map<String, Object> header = new LinkedHashMap<>();
      header.put("type", "header");
      header.put("text", "Running title");
      header.put("page_idx", 2);
      header.put("bbox", new ArrayList<>(List.of(1.0, 1.0, 50.0, 10.0)));
      Map<String, Object> footer = new LinkedHashMap<>();
      footer.put("type", "footer");
      footer.put("text", "copyright");
      footer.put("page_idx", 2);
      footer.put("bbox", new ArrayList<>(List.of(1.0, 500.0, 50.0, 510.0)));
      Map<String, Object> pageNum = new LinkedHashMap<>();
      pageNum.put("type", "page_number");
      pageNum.put("text", "7");
      pageNum.put("page_idx", 2);
      pageNum.put("bbox", new ArrayList<>(List.of(400.0, 500.0, 410.0, 510.0)));
      Map<String, Object> subHeading = new LinkedHashMap<>();
      subHeading.put("type", "text");
      subHeading.put("text_level", 2);
      subHeading.put("text", "2.1 Section");
      subHeading.put("page_idx", 2);
      subHeading.put("bbox", new ArrayList<>(List.of(15.0, 30.0, 200.0, 45.0)));
      AttachBookLayoutNodeRequest n = node("Section With Noise");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(
                  headingBlock("Section With Noise", 1, 2, List.of(1.0, 2.0, 100.0, 15.0)),
                  header,
                  footer,
                  pageNum,
                  subHeading,
                  textBlock("Body paragraph", 2, List.of(10.0, 20.0, 300.0, 400.0)))));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      BookBlock block = rootBlocksSorted(controller.getBook(nb)).getFirst();
      assertThat(block.getContentLocators(), hasSize(2));
      assertThat(
          ((PdfLocator) block.getContentLocators().get(0)).bbox(),
          equalTo(List.of(1.0, 2.0, 100.0, 15.0)));
      assertThat(
          ((PdfLocator) block.getContentLocators().get(1)).bbox(),
          equalTo(List.of(10.0, 20.0, 300.0, 400.0)));
    }

    @Test
    void epubFixtureAttachPersistsFormatStorageRefOutlineAndContentLocators() throws Exception {
      Notebook nb = myNotebook();
      ResponseEntity<Book> attached =
          controller.attachBook(
              nb, epubAttachRequest("Minimal EPUB"), epubFile(readFixtureEpubValidMinimal()));
      assertThat(attached.getStatusCode(), equalTo(HttpStatus.CREATED));
      Book created = attached.getBody();
      assertThat(created, notNullValue());
      assertThat(created.getFormat(), equalTo(BookReadingWireConstants.BOOK_FORMAT_EPUB));
      assertThat(created.getBookName(), equalTo("Minimal EPUB"));
      assertThat(created.getSourceFileRef(), not(blankOrNullString()));
      List<BookBlock> createdPreorder = blocksByLayoutOrder(created);
      assertThat(
          createdPreorder.stream().map(BookBlock::getStructuralTitle).toList(),
          equalTo(
              List.of(
                  "Part One",
                  "Chapter Alpha",
                  "Chapter Beta",
                  "Section Beta-One",
                  "Section Beta-Two")));
      assertThat(
          createdPreorder.stream().map(BookBlock::getDepth).toList(),
          equalTo(List.of(0, 1, 0, 1, 1)));
      BookBlock chapterBeta = createdPreorder.get(2);
      assertThat(chapterBeta.getContentLocators(), hasSize(2));
      EpubLocator betaFirst = (EpubLocator) chapterBeta.getContentLocators().getFirst();
      assertThat(betaFirst.href(), equalTo("OEBPS/chapter3.xhtml"));
      assertThat(betaFirst.fragment(), nullValue());
      EpubLocator betaTable = (EpubLocator) chapterBeta.getContentLocators().get(1);
      assertThat(betaTable.href(), equalTo("OEBPS/chapter3.xhtml"));
      assertThat(betaTable.fragment(), equalTo("beta-table"));
    }
  }
}
