package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.controllers.dto.AttachBookLayoutNodeRequest;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookBlock;
import com.odde.doughnut.entities.BookContentBlock;
import com.odde.doughnut.entities.BookViews;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.ApiException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NotebookBooksAttachContentControllerTest extends NotebookBooksControllerTestBase {

  @Nested
  class AttachBookContent {
    @Test
    void contentListAttachSkipsMineruLevelsAndDerivesWireTree() throws Exception {
      Notebook nb = myNotebook();
      List<Object> contentList =
          List.of(
              headingBlock("Part A", 1, 0, List.of(10.0, 20.0, 100.0, 40.0)),
              headingBlock("Deep section", 3, 0, List.of(10.0, 50.0, 100.0, 70.0)));

      Book created =
          controller
              .attachBook(
                  nb, contentListAttachRequest("MinerU book", contentList), pdfFile(STUB_PDF_BYTES))
              .getBody();

      assertThat(created, notNullValue());
      assertThat(created.getBlocks(), hasSize(2));
      BookBlock root = rootBlocksSorted(created).getFirst();
      assertThat(root.getStructuralTitle(), equalTo("Part A"));
      assertThat(root.getDepth(), equalTo(0));
      List<BookBlock> children = childrenOf(created, root);
      assertThat(children, hasSize(1));
      assertThat(children.getFirst().getStructuralTitle(), equalTo("Deep section"));
      assertThat(children.getFirst().getDepth(), equalTo(1));
    }

    @Test
    void persistsContentBlocksForEachBlock() throws Exception {
      Notebook nb = myNotebook();
      AttachBookLayoutNodeRequest n = node("Chapter 1");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(
                  headingBlock("Chapter 1", 1, 0, List.of(0.0, 0.0, 100.0, 20.0)),
                  textBlock("Some body text", 1, null))));

      Book created = controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES)).getBody();
      BookBlock block = rootBlocksSorted(created).getFirst();
      List<BookContentBlock> cbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(block.getId());
      assertThat(cbs, hasSize(2));
      assertThat(cbs.get(0).getSiblingOrder(), equalTo(0));
      assertThat(cbs.get(0).getType(), equalTo("text"));
      assertThat(cbs.get(1).getSiblingOrder(), equalTo(1));
      assertThat(cbs.get(1).getType(), equalTo("text"));
      assertThat(cbs.get(1).getPageIdx(), equalTo(1));

      Book detail = controller.getBook(nb);
      BookBlock detailChapter =
          detail.getBlocks().stream()
              .filter(b -> b.getId().equals(block.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(detailChapter.getContentBlocks(), hasSize(2));

      String json = objectMapper.writerWithView(BookViews.Full.class).writeValueAsString(detail);
      JsonNode chapterNode = null;
      for (JsonNode b : objectMapper.readTree(json).get("blocks")) {
        if (b.get("id").asInt() == block.getId()) {
          chapterNode = b;
          break;
        }
      }
      assertThat(chapterNode, notNullValue());
      JsonNode wireCbs = chapterNode.get("contentBlocks");
      assertThat(wireCbs.size(), equalTo(2));
      assertThat(wireCbs.get(0).get("id").asInt(), equalTo(cbs.get(0).getId()));
      assertThat(wireCbs.get(1).get("id").asInt(), equalTo(cbs.get(1).getId()));
      assertThat(wireCbs.get(0).get("type").asText(), equalTo("text"));
      assertThat(wireCbs.get(1).get("pageIdx").asInt(), equalTo(1));
      assertThat(wireCbs.get(0).get("raw"), nullValue());
      assertThat(wireCbs.get(1).get("raw"), nullValue());
    }

    @Test
    void persistsContentBlocksUnderSyntheticBeginningRoot() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> anchorBlock = new LinkedHashMap<>();
      anchorBlock.put("type", "beginning_anchor");
      anchorBlock.put("page_idx", 0);
      anchorBlock.put("bbox", new ArrayList<>(List.of(10.0, 70.0, 200.0, 100.0)));

      AttachBookLayoutNodeRequest beginning = node("*beginning*");
      beginning.setContentBlocks(
          new ArrayList<>(List.of(anchorBlock, textBlock("Preface paragraph", 0, null))));
      AttachBookLayoutNodeRequest chapter = node("Chapter 1");

      Book created =
          controller
              .attachBook(nb, attachRequest(beginning, chapter), pdfFile(STUB_PDF_BYTES))
              .getBody();

      List<BookBlock> roots = rootBlocksSorted(created);
      assertThat(roots, hasSize(2));
      assertThat(roots.getFirst().getStructuralTitle(), equalTo("*beginning*"));
      assertThat(roots.get(1).getStructuralTitle(), equalTo("Chapter 1"));

      List<BookContentBlock> beginningCbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(
              roots.getFirst().getId());
      assertThat(beginningCbs, hasSize(2));
      assertThat(beginningCbs.getFirst().getType(), equalTo("beginning_anchor"));
      assertThat(beginningCbs.get(1).getType(), equalTo("text"));
      assertThat(beginningCbs.get(1).getPageIdx(), equalTo(0));
    }

    @Test
    void persistsFromContentListBuildsBeginningAndChapter() throws Exception {
      Notebook nb = myNotebook();
      List<Object> cl =
          List.of(
              textBlock("Orphan body", 0, List.of(10.0, 100.0, 200.0, 130.0)),
              headingBlock("Chapter One", 2, 1, List.of(1.0, 200.0, 300.0, 240.0)));

      Book created =
          controller
              .attachBook(
                  nb, contentListAttachRequest("Linear Algebra", cl), pdfFile(STUB_PDF_BYTES))
              .getBody();

      List<BookBlock> roots = rootBlocksSorted(created);
      assertThat(roots, hasSize(2));
      assertThat(roots.getFirst().getStructuralTitle(), equalTo("*beginning*"));
      assertThat(roots.get(1).getStructuralTitle(), equalTo("Chapter One"));

      List<BookContentBlock> beginningCbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(
              roots.getFirst().getId());
      assertThat(beginningCbs, hasSize(2));
      assertThat(beginningCbs.getFirst().getType(), equalTo("beginning_anchor"));
      assertThat(beginningCbs.get(1).getRawData(), containsString("Orphan body"));
    }

    @Test
    void rejectsContentListThatProducesNoBlocks() {
      Notebook nb = myNotebook();
      Map<String, Object> pn = new LinkedHashMap<>();
      pn.put("type", "page_number");
      pn.put("text", "1");
      pn.put("page_idx", 0);
      assertThrows(
          ApiException.class,
          () ->
              controller.attachBook(
                  nb, contentListAttachRequest("Book", List.of(pn)), pdfFile(STUB_PDF_BYTES)));
    }
  }
}
