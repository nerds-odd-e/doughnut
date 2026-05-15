package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.controllers.dto.BookLayoutReorganizationSuggestion;
import com.odde.doughnut.controllers.dto.BookLayoutReorganizationSuggestion.BlockDepthSuggestion;
import com.odde.doughnut.controllers.dto.BookMutationResponse;
import com.odde.doughnut.controllers.dto.CreateBookBlockFromContentRequest;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookBlock;
import com.odde.doughnut.entities.BookBlockTitleLimits;
import com.odde.doughnut.entities.BookContentBlock;
import com.odde.doughnut.entities.BookViews;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.responses.ResponseTextConfig;
import com.openai.models.responses.StructuredResponseCreateParams;
import jakarta.validation.Validation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

class NotebookBooksBlockContentControllerTest extends NotebookBooksControllerTestBase {

  @Nested
  class CreateBookBlockFromContent {

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

    @Test
    void createsChildBlockAndMovesTailContent() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> bodyItem = new LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "Some body text");
      bodyItem.put("page_idx", 1);
      var n = node("Chapter 1");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(headingBlock("Chapter 1", 1, 0, List.of(0.0, 0.0, 100.0, 20.0)), bodyItem)));

      ResponseEntity<Book> attachRes =
          controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      assertThat(attachRes.getStatusCode(), equalTo(HttpStatus.CREATED));
      Book created = attachRes.getBody();
      assertThat(created, notNullValue());
      BookBlock chapter =
          rootBlocksSorted(created).stream()
              .filter(b -> b.getStructuralTitle().equals("Chapter 1"))
              .findFirst()
              .orElseThrow();
      List<BookContentBlock> cbsBefore =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(chapter.getId());
      assertThat(cbsBefore, hasSize(2));
      int secondId = cbsBefore.get(1).getId();

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
      JsonNode tree = objectMapper.readTree(json);
      JsonNode blocksNode = tree.get("blocks");
      assertThat(blocksNode.size(), equalTo(2));
      assertThat(blocksNode.get(0).get("contentBlocks").size(), equalTo(1));
      assertThat(blocksNode.get(1).get("contentBlocks").size(), equalTo(1));
      assertThat(
          blocksNode.get(1).get("contentBlocks").get(0).get("id").asInt(), equalTo(secondId));
    }

    @Test
    void createsChildBlockUsingStructuralTitleOverride() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> bodyItem = new LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "W".repeat(550));
      bodyItem.put("page_idx", 1);
      var n = node("Chapter 1");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(headingBlock("Chapter 1", 1, 0, List.of(0.0, 0.0, 100.0, 20.0)), bodyItem)));

      ResponseEntity<Book> attachRes =
          controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      assertThat(attachRes.getStatusCode(), equalTo(HttpStatus.CREATED));
      Book created = attachRes.getBody();
      assertThat(created, notNullValue());
      BookBlock chapter =
          rootBlocksSorted(created).stream()
              .filter(b -> b.getStructuralTitle().equals("Chapter 1"))
              .findFirst()
              .orElseThrow();
      List<BookContentBlock> cbsBefore =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(chapter.getId());
      int secondId = cbsBefore.get(1).getId();

      ResponseEntity<Book> splitRes =
          controller.createBookBlockFromContent(nb, splitRequest(secondId, "My custom title"));
      assertThat(splitRes.getStatusCode(), equalTo(HttpStatus.CREATED));
      Book after = splitRes.getBody();
      assertThat(after, notNullValue());
      List<BookBlock> ordered = blocksByLayoutOrder(after);
      assertThat(ordered.get(1).getStructuralTitle(), equalTo("My custom title"));
    }

    @Test
    void createBookBlockFromContentRequestRejectsStructuralTitleOverMax() {
      var req = new CreateBookBlockFromContentRequest();
      req.setFromBookContentBlockId(1);
      req.setStructuralTitle("x".repeat(BookBlockTitleLimits.STRUCTURAL_MAX_CHARS + 1));
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
      Map<String, Object> bodyItem = new LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "tail");
      var rootA = node("A");
      rootA.setContentBlocks(
          new ArrayList<>(
              List.of(headingBlock("A", 1, 0, List.of(0.0, 0.0, 100.0, 20.0)), bodyItem)));
      controller.attachBook(nbA, attachRequest(rootA), pdfFile(STUB_PDF_BYTES));
      BookBlock blockA = rootBlocksSorted(bookOf(nbA)).getFirst();
      int foreignContentId =
          bookContentBlockRepository
              .findAllByBookBlock_IdOrderBySiblingOrder(blockA.getId())
              .get(1)
              .getId();

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
      Map<String, Object> bodyItem = new LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "Some body text");
      var n = node("Chapter 1");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(headingBlock("Chapter 1", 1, 0, List.of(0.0, 0.0, 100.0, 20.0)), bodyItem)));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      BookBlock chapter = rootBlocksSorted(bookOf(nb)).getFirst();
      int firstId =
          bookContentBlockRepository
              .findAllByBookBlock_IdOrderBySiblingOrder(chapter.getId())
              .getFirst()
              .getId();

      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.createBookBlockFromContent(nb, splitRequest(firstId)));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsWhenChildWouldExceedMaxLayoutDepth() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> bodyItem1 = new LinkedHashMap<>();
      bodyItem1.put("type", "text");
      bodyItem1.put("text", "a");
      Map<String, Object> bodyItem2 = new LinkedHashMap<>();
      bodyItem2.put("type", "text");
      bodyItem2.put("text", "b");
      var leaf = node("Leaf");
      leaf.setContentBlocks(new ArrayList<>(List.of(bodyItem1, bodyItem2)));
      var cur = leaf;
      for (int i = 0; i < 63; i++) {
        cur = node("B" + i, cur);
      }
      controller.attachBook(nb, attachRequest(cur), pdfFile(STUB_PDF_BYTES));
      Book book = bookOf(nb);
      BookBlock deepestLeaf =
          book.getBlocks().stream()
              .filter(b -> b.getStructuralTitle().equals("Leaf"))
              .findFirst()
              .orElseThrow();
      assertThat(deepestLeaf.getDepth(), equalTo(63));
      List<BookContentBlock> cbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(deepestLeaf.getId());
      int secondId = cbs.get(1).getId();

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
      var root = node("R");
      Map<String, Object> b1 = new LinkedHashMap<>();
      b1.put("type", "text");
      b1.put("text", "a");
      Map<String, Object> b2 = new LinkedHashMap<>();
      b2.put("type", "text");
      b2.put("text", "b");
      root.setContentBlocks(new ArrayList<>(List.of(b1, b2)));
      controller.attachBook(otherNb, attachRequest(root), pdfFile(STUB_PDF_BYTES));
      BookBlock blk = rootBlocksSorted(bookOf(otherNb)).getFirst();
      int cid =
          bookContentBlockRepository
              .findAllByBookBlock_IdOrderBySiblingOrder(blk.getId())
              .get(1)
              .getId();

      currentUser.setUser(makeMe.aUser().please());

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.createBookBlockFromContent(otherNb, splitRequest(cid)));
    }
  }

  @Nested
  class SuggestBookLayoutReorganization {

    private Notebook nb;
    private final byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};

    @BeforeEach
    void setupBook() throws Exception {
      nb = myNotebook();
      controller.attachBook(
          nb, attachRequest(node("A"), node("B", node("C")), node("D")), pdfFile(pdfBytes));
    }

    private BookLayoutReorganizationSuggestion suggestionRenestingBAndC() {
      Book book = bookOf(nb);
      var suggestion = new BookLayoutReorganizationSuggestion();
      List<BlockDepthSuggestion> items = new ArrayList<>();
      for (BookBlock b : book.getBlocks()) {
        var e = new BlockDepthSuggestion();
        e.setId(b.getId());
        e.setDepth(
            switch (b.getStructuralTitle()) {
              case "A" -> 0;
              case "B" -> 1;
              case "C" -> 2;
              case "D" -> 0;
              default -> throw new IllegalStateException();
            });
        items.add(e);
      }
      suggestion.setBlocks(items);
      return suggestion;
    }

    @Test
    void returnsAiSuggestionWithValidatedDepths() throws Exception {
      openAIChatCompletionMock.stubStructuredResponse(suggestionRenestingBAndC());

      BookLayoutReorganizationSuggestion result = controller.suggestBookLayoutReorganization(nb);

      assertThat(result.getBlocks(), hasSize(4));
      Map<String, Integer> byTitle = new LinkedHashMap<>();
      Book book = bookOf(nb);
      for (BookBlock blk : book.getBlocks()) {
        int id = blk.getId();
        int depth =
            result.getBlocks().stream()
                .filter(s -> s.getId().equals(id))
                .map(BlockDepthSuggestion::getDepth)
                .findFirst()
                .orElseThrow();
        byTitle.put(blk.getStructuralTitle(), depth);
      }
      assertThat(byTitle.get("A"), equalTo(0));
      assertThat(byTitle.get("B"), equalTo(1));
      assertThat(byTitle.get("C"), equalTo(2));
      assertThat(byTitle.get("D"), equalTo(0));

      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<StructuredResponseCreateParams<BookLayoutReorganizationSuggestion>>
          paramsCaptor = ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAIChatCompletionMock.responseService()).create(paramsCaptor.capture());
      StructuredResponseCreateParams<BookLayoutReorganizationSuggestion> params =
          paramsCaptor.getValue();
      String instructions = params.rawParams().instructions().orElse("");
      assertThat(instructions, containsString("You reorganize the outline nesting"));
      String input = params.rawParams().input().flatMap(i -> i.text()).orElse("");
      assertThat(input, containsString("\"id\""));
      assertThat(
          "Should use Responses structured text format",
          params.rawParams().text().flatMap(ResponseTextConfig::format).isPresent(),
          is(true));
      verify(openAIChatCompletionMock.completionService(), never())
          .create(any(ChatCompletionCreateParams.class));
    }

    @Test
    void rejectsInvalidPreorderDepthsFromAi() throws Exception {
      Book book = bookOf(nb);
      var bad = new BookLayoutReorganizationSuggestion();
      List<BlockDepthSuggestion> items = new ArrayList<>();
      for (BookBlock b : book.getBlocks()) {
        var e = new BlockDepthSuggestion();
        e.setId(b.getId());
        e.setDepth("A".equals(b.getStructuralTitle()) ? 0 : 2);
        items.add(e);
      }
      bad.setBlocks(items);
      openAIChatCompletionMock.stubStructuredResponse(bad);

      var ex =
          assertThrows(
              ResponseStatusException.class, () -> controller.suggestBookLayoutReorganization(nb));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsWhenAiOmitsABlockId() throws Exception {
      Book book = bookOf(nb);
      var bad = new BookLayoutReorganizationSuggestion();
      List<BlockDepthSuggestion> items = new ArrayList<>();
      for (BookBlock b : book.getBlocks()) {
        if ("C".equals(b.getStructuralTitle())) {
          continue;
        }
        var e = new BlockDepthSuggestion();
        e.setId(b.getId());
        e.setDepth(0);
        items.add(e);
      }
      bad.setBlocks(items);
      openAIChatCompletionMock.stubStructuredResponse(bad);

      var ex =
          assertThrows(
              ResponseStatusException.class, () -> controller.suggestBookLayoutReorganization(nb));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsWhenAiReturnsEmptyStructuredResponse() {
      openAIChatCompletionMock.stubStructuredResponse(null);

      assertThrows(
          OpenAIServiceErrorException.class, () -> controller.suggestBookLayoutReorganization(nb));
    }

    @Test
    void rejectsNotebookWithoutWriteAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.suggestBookLayoutReorganization(otherNb));
    }
  }

  @Nested
  class ApplyBookLayoutReorganization {

    private Notebook nb;
    private final byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};

    @BeforeEach
    void setupBook() throws Exception {
      nb = myNotebook();
      controller.attachBook(
          nb, attachRequest(node("A"), node("B", node("C")), node("D")), pdfFile(pdfBytes));
    }

    private BookLayoutReorganizationSuggestion flatSuggestion() {
      Book book = bookOf(nb);
      var suggestion = new BookLayoutReorganizationSuggestion();
      List<BlockDepthSuggestion> items = new ArrayList<>();
      for (BookBlock b : book.getBlocks()) {
        var e = new BlockDepthSuggestion();
        e.setId(b.getId());
        e.setDepth(
            switch (b.getStructuralTitle()) {
              case "A" -> 0;
              case "B" -> 1;
              case "C" -> 2;
              case "D" -> 0;
              default -> throw new IllegalStateException();
            });
        items.add(e);
      }
      suggestion.setBlocks(items);
      return suggestion;
    }

    @Test
    void appliesDepthChangesAndReturnsMutation() throws Exception {
      BookMutationResponse result = controller.applyBookLayoutReorganization(nb, flatSuggestion());

      assertThat(result.getBlocks(), hasSize(4));
      Map<String, Integer> byTitle = new LinkedHashMap<>();
      for (var row : result.getBlocks()) {
        byTitle.put(row.getTitle(), row.getDepth());
      }
      assertThat(byTitle.get("A"), equalTo(0));
      assertThat(byTitle.get("B"), equalTo(1));
      assertThat(byTitle.get("C"), equalTo(2));
      assertThat(byTitle.get("D"), equalTo(0));
    }

    @Test
    void rejectsInvalidPreorderDepths() {
      Book book = bookOf(nb);
      var bad = new BookLayoutReorganizationSuggestion();
      List<BlockDepthSuggestion> items = new ArrayList<>();
      for (BookBlock b : book.getBlocks()) {
        var e = new BlockDepthSuggestion();
        e.setId(b.getId());
        e.setDepth("A".equals(b.getStructuralTitle()) ? 0 : 2);
        items.add(e);
      }
      bad.setBlocks(items);

      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.applyBookLayoutReorganization(nb, bad));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsNotebookWithoutWriteAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.applyBookLayoutReorganization(otherNb, flatSuggestion()));
    }
  }
}
