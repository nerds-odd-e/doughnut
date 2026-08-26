package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import com.odde.donut.controllers.dto.BookLayoutReorganizationSuggestion;
import com.odde.donut.controllers.dto.BookLayoutReorganizationSuggestion.BlockDepthSuggestion;
import com.odde.donut.entities.Book;
import com.odde.donut.entities.BookBlock;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.OpenAIServiceErrorException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.openai.models.responses.ResponseTextConfig;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookBooksSuggestLayoutControllerTest
    extends NotebookBooksLayoutReorganizationControllerTestBase {

  @Nested
  class SuggestBookLayoutReorganization {

    private Notebook nb;

    @BeforeEach
    void setupBook() throws Exception {
      nb = myNotebook();
      controller.attachBook(
          nb, attachRequest(node("A"), node("B", node("C")), node("D")), pdfFile(STUB_PDF_BYTES));
    }

    @Test
    void returnsAiSuggestionWithValidatedDepths() throws Exception {
      openAiStructuredResponseMock.stubStructuredResponse(
          suggestionWithDepths(nb, nestBAndCDepths()));

      BookLayoutReorganizationSuggestion result = controller.suggestBookLayoutReorganization(nb);

      Map<String, Integer> byTitle = new LinkedHashMap<>();
      for (BookBlock blk : bookOf(nb).getBlocks()) {
        int depth =
            result.getBlocks().stream()
                .filter(s -> s.getId().equals(blk.getId()))
                .map(BlockDepthSuggestion::getDepth)
                .findFirst()
                .orElseThrow();
        byTitle.put(blk.getStructuralTitle(), depth);
      }
      assertThat(byTitle, equalTo(nestBAndCDepths()));

      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<StructuredResponseCreateParams<BookLayoutReorganizationSuggestion>>
          paramsCaptor = ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      StructuredResponseCreateParams<BookLayoutReorganizationSuggestion> params =
          paramsCaptor.getValue();
      assertThat(
          params.rawParams().instructions().orElse(""),
          containsString("You reorganize the outline nesting"));
      assertThat(
          params.rawParams().input().flatMap(i -> i.text()).orElse(""), containsString("\"id\""));
      assertThat(
          params.rawParams().text().flatMap(ResponseTextConfig::format).isPresent(), is(true));
    }

    @Test
    void rejectsInvalidPreorderDepthsFromAi() {
      openAiStructuredResponseMock.stubStructuredResponse(
          suggestionWithDepths(nb, Map.of("A", 0, "B", 2, "C", 2, "D", 2)));

      var ex =
          assertThrows(
              ResponseStatusException.class, () -> controller.suggestBookLayoutReorganization(nb));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsWhenAiOmitsABlockId() {
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
      openAiStructuredResponseMock.stubStructuredResponse(bad);

      var ex =
          assertThrows(
              ResponseStatusException.class, () -> controller.suggestBookLayoutReorganization(nb));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsWhenAiReturnsEmptyStructuredResponse() {
      openAiStructuredResponseMock.stubStructuredResponse(null);

      assertThrows(
          OpenAIServiceErrorException.class, () -> controller.suggestBookLayoutReorganization(nb));
    }

    @Test
    void rejectsNotebookWithoutWriteAccess() {
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.suggestBookLayoutReorganization(otherUsersNotebookWithBook()));
    }
  }
}
