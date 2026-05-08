package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.controllers.dto.BookBlockDepthRequest;
import com.odde.doughnut.controllers.dto.BookBlockMutationResponse;
import com.odde.doughnut.controllers.dto.BookMutationResponse;
import com.odde.doughnut.entities.BookBlock;
import com.odde.doughnut.entities.BookContentBlock;
import com.odde.doughnut.entities.BookViews;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookBooksBlockDepthControllerTest extends NotebookBooksControllerTestBase {

  @Nested
  class ChangeBookBlockDepth {

    private Notebook nb;
    private byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};

    private BookBlockDepthRequest indent() {
      var r = new BookBlockDepthRequest();
      r.setDirection("INDENT");
      return r;
    }

    private BookBlockDepthRequest outdent() {
      var r = new BookBlockDepthRequest();
      r.setDirection("OUTDENT");
      return r;
    }

    @BeforeEach
    void setup() throws Exception {
      nb = myNotebook();
      // Layout: A(0), B(0), C(1), D(0)
      controller.attachBook(
          nb, attachRequest(node("A"), node("B", node("C")), node("D")), pdfFile(pdfBytes));
    }

    private List<BookBlock> blocks() {
      return bookOf(nb).getBlocks();
    }

    private BookBlock blockByTitle(String title) {
      return blocks().stream()
          .filter(b -> b.getStructuralTitle().equals(title))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Block not found: " + title));
    }

    private int depthOfMut(List<BookBlockMutationResponse> blocks, String title) {
      return blocks.stream()
          .filter(b -> b.getTitle().equals(title))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Block not found: " + title))
          .getDepth();
    }

    @Test
    void indentIncreasesDepthByOne() throws Exception {
      BookBlock b = blockByTitle("B");
      assertThat(b.getDepth(), equalTo(0));

      BookMutationResponse result = controller.changeBookBlockDepth(nb, b, indent());

      int newDepth =
          result.getBlocks().stream()
              .filter(blk -> blk.getId() == b.getId())
              .findFirst()
              .orElseThrow()
              .getDepth();
      assertThat(newDepth, equalTo(1));
    }

    @Test
    void indentReturnsFullBookWithAllBlocks() throws Exception {
      BookBlock b = blockByTitle("B");

      BookMutationResponse result = controller.changeBookBlockDepth(nb, b, indent());

      assertThat(result.getBlocks(), hasSize(4));
    }

    @Test
    void indentMovesDescendantsWithHead() throws Exception {
      // Layout: A(0), B(0), C(1), D(0) — indent B should move B to 1 and C to 2
      BookBlock b = blockByTitle("B");

      BookMutationResponse result = controller.changeBookBlockDepth(nb, b, indent());

      List<BookBlockMutationResponse> resultBlocks = result.getBlocks();
      assertThat(depthOfMut(resultBlocks, "A"), equalTo(0));
      assertThat(depthOfMut(resultBlocks, "B"), equalTo(1));
      assertThat(depthOfMut(resultBlocks, "C"), equalTo(2));
      assertThat(depthOfMut(resultBlocks, "D"), equalTo(0));
    }

    @Test
    void outdentMovesDescendantsWithHead() throws Exception {
      // Layout: A(0), B(0), C(1), D(0) — outdent C brings it to 0 (leaf, no descendants)
      // To test subtree outdent: attach a deeper layout X(0), Y(1), Z(2), W(0)
      Notebook nb2 = myNotebook();
      controller.attachBook(
          nb2, attachRequest(node("X", node("Y", node("Z"))), node("W")), pdfFile(pdfBytes));
      BookBlock y =
          bookOf(nb2).getBlocks().stream()
              .filter(b -> b.getStructuralTitle().equals("Y"))
              .findFirst()
              .orElseThrow();

      BookMutationResponse result = controller.changeBookBlockDepth(nb2, y, outdent());

      List<BookBlockMutationResponse> resultBlocks = result.getBlocks();
      assertThat(depthOfMut(resultBlocks, "X"), equalTo(0));
      assertThat(depthOfMut(resultBlocks, "Y"), equalTo(0));
      assertThat(depthOfMut(resultBlocks, "Z"), equalTo(1));
      assertThat(depthOfMut(resultBlocks, "W"), equalTo(0));
    }

    @Test
    void indentFirstBlockThrows() {
      BookBlock a = blockByTitle("A");

      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.changeBookBlockDepth(nb, a, indent()));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
      assertThat(ex.getReason(), equalTo("Cannot indent the first block"));
    }

    @Test
    void indentWhenAlreadyAtMaxDepthRelativeToPredecessorThrows() {
      // C is at depth 1, its predecessor B is at depth 0; C is already at B.depth+1
      BookBlock c = blockByTitle("C");

      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.changeBookBlockDepth(nb, c, indent()));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
      assertThat(
          ex.getReason(), equalTo("Block is already at maximum depth relative to predecessor"));
    }

    @Test
    void outdentDecreasesDepthByOne() throws Exception {
      BookBlock c = blockByTitle("C");
      assertThat(c.getDepth(), equalTo(1));

      BookMutationResponse result = controller.changeBookBlockDepth(nb, c, outdent());

      int newDepth =
          result.getBlocks().stream()
              .filter(blk -> blk.getId() == c.getId())
              .findFirst()
              .orElseThrow()
              .getDepth();
      assertThat(newDepth, equalTo(0));
    }

    @Test
    void outdentBlockAtDepthZeroThrows() {
      BookBlock a = blockByTitle("A");

      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.changeBookBlockDepth(nb, a, outdent()));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
      assertThat(ex.getReason(), equalTo("Block is already at minimum depth"));
    }

    @Test
    void outdentMovesEntireSubtreeKeepingInvariantValid() throws Exception {
      // X(0), Y(1), Z(2) — outdenting Y moves Y+Z together → X(0), Y(0), Z(1), valid tree
      Notebook nb2 = myNotebook();
      controller.attachBook(nb2, attachRequest(node("X", node("Y", node("Z")))), pdfFile(pdfBytes));
      BookBlock y =
          bookOf(nb2).getBlocks().stream()
              .filter(blk -> blk.getStructuralTitle().equals("Y"))
              .findFirst()
              .orElseThrow();

      BookMutationResponse result = controller.changeBookBlockDepth(nb2, y, outdent());

      List<BookBlockMutationResponse> resultBlocks = result.getBlocks();
      assertThat(depthOfMut(resultBlocks, "X"), equalTo(0));
      assertThat(depthOfMut(resultBlocks, "Y"), equalTo(0));
      assertThat(depthOfMut(resultBlocks, "Z"), equalTo(1));
    }

    @Test
    void changeBookBlockDepthJsonOmitsContentLocatorsOnEveryBlock() throws Exception {
      BookBlock b = blockByTitle("B");
      BookMutationResponse wire = controller.changeBookBlockDepth(nb, b, indent());
      String json = objectMapper.writerWithView(BookViews.Full.class).writeValueAsString(wire);
      JsonNode tree = objectMapper.readTree(json);
      JsonNode blocksNode = tree.get("blocks");
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
      var book = bookOf(nb);
      BookBlock blockB =
          book.getBlocks().stream()
              .filter(b -> b.getStructuralTitle().equals("B"))
              .findFirst()
              .orElseThrow();
      BookBlock blockA =
          book.getBlocks().stream()
              .filter(b -> b.getStructuralTitle().equals("A"))
              .findFirst()
              .orElseThrow();

      BookMutationResponse cancelWire = controller.cancelBookBlock(nb, blockB);
      String cancelJson =
          objectMapper.writerWithView(BookViews.Full.class).writeValueAsString(cancelWire);
      JsonNode cancelTree = objectMapper.readTree(cancelJson);
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
      JsonNode raw = objectMapper.readTree(aCbs.getFirst().getRawData());
      assertThat(raw.get("text").asText(), equalTo("B"));
    }

    @Test
    void titleBecomesFirstContentBlockOfPredecessorWhenCancelledBlockHasContent() throws Exception {
      Notebook nb = myNotebook();
      java.util.Map<String, Object> bodyItem = new java.util.LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "Body of B");
      bodyItem.put("page_idx", 1);
      var nodeB = node("B");
      nodeB.setContentBlocks(
          new ArrayList<>(
              List.of(headingBlock("B", 1, 0, List.of(0.0, 0.0, 100.0, 20.0)), bodyItem)));
      controller.attachBook(nb, attachRequest(node("A"), nodeB), pdfFile(STUB_PDF_BYTES));
      var book = bookOf(nb);
      BookBlock blockB =
          book.getBlocks().stream()
              .filter(b -> b.getStructuralTitle().equals("B"))
              .findFirst()
              .orElseThrow();
      BookBlock blockA =
          book.getBlocks().stream()
              .filter(b -> b.getStructuralTitle().equals("A"))
              .findFirst()
              .orElseThrow();

      controller.cancelBookBlock(nb, blockB);

      List<BookContentBlock> aCbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(blockA.getId());
      assertThat(aCbs, hasSize(2));
      JsonNode headingRaw = objectMapper.readTree(aCbs.get(0).getRawData());
      assertThat(headingRaw.get("text").asText(), equalTo("B"));
      assertThat(headingRaw.has("text_level"), equalTo(false));
      JsonNode bodyRaw = objectMapper.readTree(aCbs.get(1).getRawData());
      assertThat(bodyRaw.get("text").asText(), equalTo("Body of B"));
    }
  }
}
