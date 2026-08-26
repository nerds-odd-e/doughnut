package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.donut.controllers.dto.AttachBookLayoutNodeRequest;
import com.odde.donut.controllers.dto.CreateBookBlockFromContentRequest;
import com.odde.donut.entities.Book;
import com.odde.donut.entities.BookBlock;
import com.odde.donut.entities.BookBlockTitleLimits;
import com.odde.donut.entities.BookContentBlock;
import com.odde.donut.entities.BookViews;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.book.BookReadingWireConstants;
import jakarta.validation.Validation;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class NotebookBooksCreateBlockFromContentControllerTest
    extends NotebookBooksBlockControllerTestBase {

  private static CreateBookBlockFromContentRequest splitRequest(int contentBlockId) {
    return splitRequest(contentBlockId, null);
  }

  private static CreateBookBlockFromContentRequest splitRequest(
      int contentBlockId, String structuralTitle) {
    var req = new CreateBookBlockFromContentRequest();
    req.setFromBookContentBlockId(contentBlockId);
    req.setStructuralTitle(structuralTitle);
    return req;
  }

  private static AttachBookLayoutNodeRequest chapterWithHeadingAndBody(
      String title, String bodyText) {
    var n = node(title);
    n.setContentBlocks(
        new ArrayList<>(
            List.of(
                headingBlock(title, 1, 0, List.of(0.0, 0.0, 100.0, 20.0)),
                textBlock(bodyText, 1, null))));
    return n;
  }

  private static AttachBookLayoutNodeRequest chapterWithTwoBodies(String title) {
    var n = node(title);
    n.setContentBlocks(new ArrayList<>(List.of(textBlock("a", 1, null), textBlock("b", 1, null))));
    return n;
  }

  private int contentBlockId(BookBlock block, int index) {
    return bookContentBlockRepository
        .findAllByBookBlock_IdOrderBySiblingOrder(block.getId())
        .get(index)
        .getId();
  }

  @Nested
  class CreateBookBlockFromContent {

    @Test
    void createsChildBlockAndMovesTailContent() throws Exception {
      Notebook nb = myNotebook();
      Book created =
          controller
              .attachBook(
                  nb,
                  attachRequest(chapterWithHeadingAndBody("Chapter 1", "Some body text")),
                  pdfFile(STUB_PDF_BYTES))
              .getBody();
      BookBlock chapter = blockByTitle(created, "Chapter 1");
      int secondId = contentBlockId(chapter, 1);

      ResponseEntity<Book> splitRes =
          controller.createBookBlockFromContent(nb, splitRequest(secondId));
      assertThat(splitRes.getStatusCode(), equalTo(HttpStatus.CREATED));
      Book after = splitRes.getBody();
      assertThat(after, notNullValue());
      assertThat(after.getBlocks(), hasSize(2));

      List<BookBlock> ordered = blocksByLayoutOrder(after);
      assertThat(ordered.get(0).getStructuralTitle(), equalTo("Chapter 1"));
      assertThat(ordered.get(1).getStructuralTitle(), equalTo("Some body text"));
      assertThat(ordered.get(0).getDepth(), equalTo(0));
      assertThat(ordered.get(1).getDepth(), equalTo(1));

      List<BookContentBlock> ownerCbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(
              ordered.get(0).getId());
      List<BookContentBlock> childCbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(
              ordered.get(1).getId());
      assertThat(ownerCbs, hasSize(1));
      assertThat(childCbs, hasSize(1));
      assertThat(childCbs.get(0).getId(), equalTo(secondId));

      for (int i = 0; i < ordered.size(); i++) {
        assertThat(ordered.get(i).getLayoutSequence(), equalTo(i));
      }

      String json = objectMapper.writerWithView(BookViews.Full.class).writeValueAsString(after);
      JsonNode blocksNode = objectMapper.readTree(json).get("blocks");
      assertThat(blocksNode.get(0).get("contentBlocks").size(), equalTo(1));
      assertThat(blocksNode.get(1).get("contentBlocks").size(), equalTo(1));
      assertThat(
          blocksNode.get(1).get("contentBlocks").get(0).get("id").asInt(), equalTo(secondId));
    }

    @Test
    void createsChildBlockUsingStructuralTitleOverride() throws Exception {
      Notebook nb = myNotebook();
      Book created =
          controller
              .attachBook(
                  nb,
                  attachRequest(chapterWithHeadingAndBody("Chapter 1", "W".repeat(550))),
                  pdfFile(STUB_PDF_BYTES))
              .getBody();
      int secondId = contentBlockId(blockByTitle(created, "Chapter 1"), 1);

      Book after =
          controller
              .createBookBlockFromContent(nb, splitRequest(secondId, "My custom title"))
              .getBody();

      assertThat(
          blocksByLayoutOrder(after).get(1).getStructuralTitle(), equalTo("My custom title"));
    }

    @Test
    void createBookBlockFromContentRequestRejectsStructuralTitleOverMax() {
      var req = splitRequest(1, "x".repeat(BookBlockTitleLimits.STRUCTURAL_MAX_CHARS + 1));
      try (var factory = Validation.buildDefaultValidatorFactory()) {
        assertThat(factory.getValidator().validate(req), not(empty()));
      }
    }

    @Test
    void unknownContentBlockIdThrows404() throws Exception {
      Notebook nb = myNotebook();
      controller.attachBook(nb, attachRequest(node("A")), pdfFile(STUB_PDF_BYTES));

      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.createBookBlockFromContent(nb, splitRequest(Integer.MAX_VALUE)));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void contentBlockFromAnotherNotebookBookThrows404() throws Exception {
      Notebook nbA = myNotebook();
      controller.attachBook(
          nbA, attachRequest(chapterWithHeadingAndBody("A", "tail")), pdfFile(STUB_PDF_BYTES));
      int foreignContentId = contentBlockId(rootBlocksSorted(bookOf(nbA)).getFirst(), 1);

      Notebook nbB = myNotebook();
      controller.attachBook(nbB, attachRequest(node("B")), pdfFile(STUB_PDF_BYTES));

      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.createBookBlockFromContent(nbB, splitRequest(foreignContentId)));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void splitAtFirstContentBlockThrows400() throws Exception {
      Notebook nb = myNotebook();
      controller.attachBook(
          nb,
          attachRequest(chapterWithHeadingAndBody("Chapter 1", "Some body text")),
          pdfFile(STUB_PDF_BYTES));
      int firstId = contentBlockId(rootBlocksSorted(bookOf(nb)).getFirst(), 0);

      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.createBookBlockFromContent(nb, splitRequest(firstId)));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsWhenChildWouldExceedMaxLayoutDepth() throws Exception {
      Notebook nb = myNotebook();
      controller.attachBook(
          nb, attachRequest(chapterWithTwoBodies("Leaf")), pdfFile(STUB_PDF_BYTES));
      BookBlock leaf = rootBlocksSorted(bookOf(nb)).getFirst();
      leaf.setDepth(BookReadingWireConstants.MAX_LAYOUT_DEPTH - 1);
      entityManager.flush();
      int secondId = contentBlockId(leaf, 1);

      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.createBookBlockFromContent(nb, splitRequest(secondId)));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsNotebookWithoutWriteAccess() throws Exception {
      User owner = makeMe.aUser().please();
      Notebook otherNb = makeMe.aNotebook().creatorAndOwner(owner).please();
      currentUser.setUser(owner);
      controller.attachBook(
          otherNb, attachRequest(chapterWithTwoBodies("R")), pdfFile(STUB_PDF_BYTES));
      int cid = contentBlockId(rootBlocksSorted(bookOf(otherNb)).getFirst(), 1);

      currentUser.setUser(makeMe.aUser().please());

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.createBookBlockFromContent(otherNb, splitRequest(cid)));
    }
  }
}
