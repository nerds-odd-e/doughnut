package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.controllers.dto.AttachBookLayoutNodeRequest;
import com.odde.doughnut.controllers.dto.BookBlockDepthRequest;
import com.odde.doughnut.controllers.dto.BookBlockMutationResponse;
import com.odde.doughnut.controllers.dto.BookMutationResponse;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookBlock;
import com.odde.doughnut.entities.BookContentBlock;
import com.odde.doughnut.entities.BookViews;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookBooksBlockDepthControllerTest extends NotebookBooksBlockControllerTestBase {

  private static BookBlockDepthRequest indent() {
    var r = new BookBlockDepthRequest();
    r.setDirection("INDENT");
    return r;
  }

  private static BookBlockDepthRequest outdent() {
    var r = new BookBlockDepthRequest();
    r.setDirection("OUTDENT");
    return r;
  }

  private static int depthOfMut(List<BookBlockMutationResponse> blocks, String title) {
    return blocks.stream()
        .filter(b -> b.getTitle().equals(title))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Block not found: " + title))
        .getDepth();
  }

  private static Map<String, Integer> depthsByTitle(List<BookBlockMutationResponse> blocks) {
    Map<String, Integer> byTitle = new LinkedHashMap<>();
    for (var b : blocks) {
      byTitle.put(b.getTitle(), b.getDepth());
    }
    return byTitle;
  }

  @Nested
  class ChangeBookBlockDepth {

    private Notebook nb;

    @BeforeEach
    void setup() throws Exception {
      nb = myNotebook();
      // Layout: A(0), B(0), C(1), D(0)
      controller.attachBook(
          nb, attachRequest(node("A"), node("B", node("C")), node("D")), pdfFile(STUB_PDF_BYTES));
    }

    @Test
    void indentMovesDescendantsWithHead() throws Exception {
      BookMutationResponse result =
          controller.changeBookBlockDepth(nb, blockByTitle(bookOf(nb), "B"), indent());

      assertThat(result.getBlocks(), hasSize(4));
      assertThat(
          depthsByTitle(result.getBlocks()), equalTo(Map.of("A", 0, "B", 1, "C", 2, "D", 0)));
    }

    @Test
    void outdentMovesDescendantsWithHead() throws Exception {
      Notebook nb2 = myNotebook();
      controller.attachBook(
          nb2, attachRequest(node("X", node("Y", node("Z"))), node("W")), pdfFile(STUB_PDF_BYTES));
      BookBlock y = blockByTitle(bookOf(nb2), "Y");

      BookMutationResponse result = controller.changeBookBlockDepth(nb2, y, outdent());

      assertThat(
          depthsByTitle(result.getBlocks()), equalTo(Map.of("X", 0, "Y", 0, "Z", 1, "W", 0)));
    }

    @Test
    void indentFirstBlockThrows() {
      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.changeBookBlockDepth(nb, blockByTitle(bookOf(nb), "A"), indent()));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
      assertThat(ex.getReason(), equalTo("Cannot indent the first block"));
    }

    @Test
    void indentWhenAlreadyAtMaxDepthRelativeToPredecessorThrows() {
      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.changeBookBlockDepth(nb, blockByTitle(bookOf(nb), "C"), indent()));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
      assertThat(
          ex.getReason(), equalTo("Block is already at maximum depth relative to predecessor"));
    }

    @Test
    void outdentDecreasesDepthByOne() throws Exception {
      BookMutationResponse result =
          controller.changeBookBlockDepth(nb, blockByTitle(bookOf(nb), "C"), outdent());

      assertThat(depthOfMut(result.getBlocks(), "C"), equalTo(0));
    }

    @Test
    void outdentBlockAtDepthZeroThrows() {
      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.changeBookBlockDepth(nb, blockByTitle(bookOf(nb), "A"), outdent()));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
      assertThat(ex.getReason(), equalTo("Block is already at minimum depth"));
    }

    @Test
    void changeBookBlockDepthJsonOmitsContentLocatorsOnEveryBlock() throws Exception {
      BookMutationResponse wire =
          controller.changeBookBlockDepth(nb, blockByTitle(bookOf(nb), "B"), indent());
      JsonNode blocksNode =
          objectMapper
              .readTree(objectMapper.writerWithView(BookViews.Full.class).writeValueAsString(wire))
              .get("blocks");
      assertThat(blocksNode.size(), equalTo(4));
      for (JsonNode block : blocksNode) {
        assertThat(block.has("contentLocators"), equalTo(false));
      }
    }

    @Test
    void blockFromAnotherNotebooksBookThrows() {
      Notebook otherNb = otherUsersNotebookWithBook();
      BookBlock otherBlock = rootBlocksSorted(bookOf(otherNb)).getFirst();

      assertThrows(
          ResponseStatusException.class,
          () -> controller.changeBookBlockDepth(nb, otherBlock, indent()));
    }

    @Test
    void rejectsNotebookWithoutWriteAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();
      BookBlock block = rootBlocksSorted(bookOf(otherNb)).getFirst();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.changeBookBlockDepth(otherNb, block, indent()));
    }
  }

  @Nested
  class CancelBookBlock {

    @Test
    void titleBecomesContentBlockOfPredecessorWhenCancelledBlockHasNoContent() throws Exception {
      Notebook nb = myNotebook();
      controller.attachBook(nb, attachRequest(node("A"), node("B")), pdfFile(STUB_PDF_BYTES));
      Book book = bookOf(nb);
      BookBlock blockB = blockByTitle(book, "B");
      BookBlock blockA = blockByTitle(book, "A");

      BookMutationResponse cancelWire = controller.cancelBookBlock(nb, blockB);
      JsonNode cancelTree =
          objectMapper.readTree(
              objectMapper.writerWithView(BookViews.Full.class).writeValueAsString(cancelWire));
      int aId = blockA.getId();
      for (JsonNode row : cancelTree.get("blocks")) {
        if (row.get("id").asInt() == aId) {
          assertThat(row.has("contentLocators"), equalTo(true));
        } else {
          assertThat(row.has("contentLocators"), equalTo(false));
        }
      }

      List<BookContentBlock> aCbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(blockA.getId());
      assertThat(aCbs, hasSize(1));
      assertThat(
          objectMapper.readTree(aCbs.getFirst().getRawData()).get("text").asText(), equalTo("B"));
    }

    @Test
    void titleBecomesFirstContentBlockOfPredecessorWhenCancelledBlockHasContent() throws Exception {
      Notebook nb = myNotebook();
      AttachBookLayoutNodeRequest nodeB = node("B");
      nodeB.setContentBlocks(
          new ArrayList<>(
              List.of(
                  headingBlock("B", 1, 0, List.of(0.0, 0.0, 100.0, 20.0)),
                  textBlock("Body of B", 1, null))));
      controller.attachBook(nb, attachRequest(node("A"), nodeB), pdfFile(STUB_PDF_BYTES));
      Book book = bookOf(nb);
      BookBlock blockB = blockByTitle(book, "B");
      BookBlock blockA = blockByTitle(book, "A");

      controller.cancelBookBlock(nb, blockB);

      List<BookContentBlock> aCbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(blockA.getId());
      assertThat(aCbs, hasSize(2));
      JsonNode headingRaw = objectMapper.readTree(aCbs.get(0).getRawData());
      assertThat(headingRaw.get("text").asText(), equalTo("B"));
      assertThat(headingRaw.has("text_level"), equalTo(false));
      assertThat(
          objectMapper.readTree(aCbs.get(1).getRawData()).get("text").asText(),
          equalTo("Body of B"));
    }
  }
}
