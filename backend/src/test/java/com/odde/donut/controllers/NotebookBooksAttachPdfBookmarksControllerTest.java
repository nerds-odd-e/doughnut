package com.odde.donut.controllers;

import static com.odde.donut.controllers.TestPdfs.pdfWithBookmarks;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.controllers.TestPdfs.PdfBookmark;
import com.odde.donut.entities.Book;
import com.odde.donut.entities.BookBlock;
import com.odde.donut.entities.BookContentBlock;
import com.odde.donut.entities.Notebook;
import com.odde.donut.services.book.PdfLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NotebookBooksAttachPdfBookmarksControllerTest extends NotebookBooksControllerTestBase {

  @Nested
  class AttachPdfWithBookmarks {
    Notebook nb;
    Book book;

    @BeforeEach
    void attachBookWithBookmarks() throws Exception {
      Map<String, Object> pageHeader =
          textBlock("Getting Started", 1, List.of(10.0, 20.0, 900.0, 40.0));
      pageHeader.put("type", "header");
      List<Object> contentList =
          List.of(
              textBlock("Preface", 0, List.of(10.0, 100.0, 900.0, 150.0)),
              textBlock("Chapter 3", 0, List.of(10.0, 300.0, 900.0, 320.0)),
              headingBlock("Getting Started", 1, 0, List.of(10.0, 330.0, 900.0, 360.0)),
              textBlock("First steps", 0, List.of(10.0, 400.0, 900.0, 450.0)),
              pageHeader,
              textBlock("Last words", 1, List.of(10.0, 700.0, 900.0, 750.0)));
      byte[] pdf =
          pdfWithBookmarks(
              2,
              new PdfBookmark("3 Getting Started", 0, 300),
              new PdfBookmark("3.1 Setup", 1, 500),
              new PdfBookmark("3.2 Tools", 1, 600));

      nb = myNotebook();
      controller.attachBook(nb, contentListAttachRequest("Bookmarked", contentList), pdfFile(pdf));
      makeMe.entityPersister.flushAndClear();
      book = controller.getBook(nb);
    }

    @Test
    void oneBlockPerBookmarkAfterTheBeginning() {
      assertThat(
          rootBlocksSorted(book).stream().map(BookBlock::getStructuralTitle).toList(),
          contains("*beginning*", "3 Getting Started", "3.1 Setup", "3.2 Tools"));
    }

    @Test
    void textBeforeTheFirstBookmarkIsTheBeginning() throws Exception {
      assertThat(contentTexts(rootBlocksSorted(book).get(0)), contains("", "Preface"));
    }

    @Test
    void headingsLabelsAndPageHeadersAreContentOfTheBookmarkBlock() throws Exception {
      assertThat(
          contentTexts(rootBlocksSorted(book).get(1)),
          contains("", "Chapter 3", "Getting Started", "First steps", "Getting Started"));
    }

    @Test
    void eachBookmarkBlockLandsAtItsBookmark() throws Exception {
      List<BookBlock> roots = rootBlocksSorted(book);
      assertThat(landing(roots.get(1)), equalTo(List.of(0.0, 300.0)));
      assertThat(landing(roots.get(2)), equalTo(List.of(1.0, 500.0)));
      assertThat(landing(roots.get(3)), equalTo(List.of(1.0, 600.0)));
      assertThat(contentTexts(roots.get(2)), contains(""));
      assertThat(contentTexts(roots.get(3)), contains("", "Last words"));
    }

    private List<String> contentTexts(BookBlock block) throws Exception {
      List<String> texts = new ArrayList<>();
      for (BookContentBlock cb : block.getContentBlocks()) {
        texts.add(objectMapper.readTree(cb.getRawData()).path("text").asText());
      }
      return texts;
    }
  }

  @Nested
  class AttachPdfWithNestedAndIndirectBookmarks {
    Book book;

    @BeforeEach
    void attachBookWithBookmarks() throws Exception {
      List<Object> contentList = List.of(textBlock("Body", 0, List.of(10.0, 500.0, 900.0, 550.0)));
      byte[] pdf =
          pdfWithBookmarks(
              3,
              new PdfBookmark(
                  "1 Part",
                  0,
                  100,
                  new PdfBookmark(
                      "1.1 Section", 0, 200, new PdfBookmark("1.1.1 Subsection", 0, 300))),
              new PdfBookmark("2 Named", 1, 400).named("sec.2"),
              PdfBookmark.fit("3 Fit", 2));

      Notebook nb = myNotebook();
      controller.attachBook(nb, contentListAttachRequest("Nested", contentList), pdfFile(pdf));
      makeMe.entityPersister.flushAndClear();
      book = controller.getBook(nb);
    }

    @Test
    void nestingFollowsTheBookmarks() {
      BookBlock part = rootBlocksSorted(book).getFirst();
      BookBlock section = childrenOf(book, part).getFirst();
      assertThat(section.getStructuralTitle(), equalTo("1.1 Section"));
      assertThat(
          childrenOf(book, section).stream().map(BookBlock::getStructuralTitle).toList(),
          contains("1.1.1 Subsection"));
    }

    @Test
    void aNamedDestinationLandsOnTheNamedPage() {
      assertThat(landing(rootBlocksSorted(book).get(1)), equalTo(List.of(1.0, 400.0)));
    }

    @Test
    void aBookmarkWithoutATopLandsAtItsPageTop() {
      assertThat(landing(rootBlocksSorted(book).get(2)), equalTo(List.of(2.0, 0.0)));
    }
  }

  private static List<Double> landing(BookBlock block) {
    PdfLocator first = (PdfLocator) block.getContentLocators().getFirst();
    return List.of((double) first.pageIndex(), first.bbox().get(1));
  }
}
