package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.BookMutationResponse;
import com.odde.donut.entities.Notebook;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookBooksApplyLayoutControllerTest
    extends NotebookBooksLayoutReorganizationControllerTestBase {

  @Nested
  class ApplyBookLayoutReorganization {

    private Notebook nb;

    @BeforeEach
    void setupBook() throws Exception {
      nb = myNotebook();
      controller.attachBook(
          nb, attachRequest(node("A"), node("B", node("C")), node("D")), pdfFile(STUB_PDF_BYTES));
    }

    @Test
    void appliesDepthChangesAndReturnsMutation() throws Exception {
      BookMutationResponse result =
          controller.applyBookLayoutReorganization(nb, suggestionWithDepths(nb, nestBAndCDepths()));

      Map<String, Integer> byTitle = new LinkedHashMap<>();
      for (var row : result.getBlocks()) {
        byTitle.put(row.getTitle(), row.getDepth());
      }
      assertThat(byTitle, equalTo(nestBAndCDepths()));
    }

    @Test
    void rejectsInvalidPreorderDepths() {
      var ex =
          assertThrows(
              ResponseStatusException.class,
              () ->
                  controller.applyBookLayoutReorganization(
                      nb, suggestionWithDepths(nb, Map.of("A", 0, "B", 2, "C", 2, "D", 2))));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void rejectsNotebookWithoutWriteAccess() {
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () ->
              controller.applyBookLayoutReorganization(
                  otherUsersNotebookWithBook(), suggestionWithDepths(nb, nestBAndCDepths())));
    }
  }
}
