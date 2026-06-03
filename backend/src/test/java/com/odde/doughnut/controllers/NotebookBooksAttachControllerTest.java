package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.controllers.dto.AttachBookLayoutNodeRequest;
import com.odde.doughnut.controllers.dto.AttachBookRequest;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookBlock;
import com.odde.doughnut.entities.BookContentBlock;
import com.odde.doughnut.entities.BookViews;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.book.BookReadingWireConstants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class NotebookBooksAttachControllerTest extends NotebookBooksControllerTestBase {

  @Nested
  class AttachBook {
    @Test
    void persistsNestedOutlineAndReturnsBookWithBlocks() throws Exception {
      Notebook nb = myNotebook();
      AttachBookLayoutNodeRequest ch1 = node("Section 1.1");
      AttachBookLayoutNodeRequest ch2 = node("Section 1.2");
      AttachBookLayoutNodeRequest root = node("Chapter 1", ch1, ch2);
      AttachBookRequest req = attachRequest(root);
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e};

      ResponseEntity<Book> res = controller.attachBook(nb, req, pdfFile(pdfBytes));

      assertThat(res.getStatusCode(), equalTo(HttpStatus.CREATED));
      assertThat(res.getBody(), notNullValue());
      Book created = res.getBody();
      assertThat(created.getId(), notNullValue());
      assertThat(created.getBookName(), equalTo("Linear Algebra"));
      assertThat(created.getSourceFileRef(), notNullValue());
      assertThat(created.getSourceFileRef().isBlank(), equalTo(false));
      assertThat(created.getBlocks(), hasSize(3));

      BookBlock outRoot = rootBlocksSorted(created).getFirst();
      assertThat(outRoot.getStructuralTitle(), equalTo("Chapter 1"));
      assertThat(outRoot.getId(), notNullValue());
      List<BookBlock> children = childrenOf(created, outRoot);
      assertThat(children, hasSize(2));
      assertThat(children.getFirst().getStructuralTitle(), equalTo("Section 1.1"));
      assertThat(children.get(1).getStructuralTitle(), equalTo("Section 1.2"));

      Book detail = controller.getBook(nb);
      assertThat(detail.getBlocks(), hasSize(3));
      BookBlock detailRoot = rootBlocksSorted(detail).getFirst();
      assertThat(detailRoot.getId(), equalTo(outRoot.getId()));
      assertThat(childrenOf(detail, detailRoot), hasSize(2));

      ResponseEntity<byte[]> fileRes = controller.getBookFile(webRequest(), nb);
      assertThat(fileRes.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(fileRes.getBody(), equalTo(pdfBytes));

      String json = objectMapper.writerWithView(BookViews.Full.class).writeValueAsString(detail);
      JsonNode blocks = objectMapper.readTree(json).get("blocks");
      assertThat(blocks.size(), equalTo(3));
      assertThat(blocks.get(0).get("depth").asInt(), equalTo(0));
      assertThat(blocks.get(1).get("depth").asInt(), equalTo(1));
      assertThat(blocks.get(2).get("depth").asInt(), equalTo(1));
      assertThat(blocks.get(0).get("title").asText(), equalTo("Chapter 1"));
      assertThat(blocks.get(1).get("title").asText(), equalTo("Section 1.1"));
      assertThat(blocks.get(2).get("title").asText(), equalTo("Section 1.2"));
      List<BookBlock> byLayoutSeq =
          detail.getBlocks().stream()
              .sorted(Comparator.comparingInt(BookBlock::getLayoutSequence))
              .toList();
      for (int i = 0; i < 3; i++) {
        assertThat(blocks.get(i).get("id").asInt(), equalTo(byLayoutSeq.get(i).getId()));
        assertThat(blocks.get(i).get("contentBlocks").size(), equalTo(0));
      }
    }

    @Test
    void contentListAttachSkipsMineruLevelsAndDerivesWireTree() throws Exception {
      Notebook nb = myNotebook();
      List<Object> contentList = new ArrayList<>();
      contentList.add(headingBlock("Part A", 1, 0, List.of(10.0, 20.0, 100.0, 40.0)));
      contentList.add(headingBlock("Deep section", 3, 0, List.of(10.0, 50.0, 100.0, 70.0)));
      AttachBookRequest req = new AttachBookRequest();
      req.setBookName("MinerU book");
      req.setFormat(BookReadingWireConstants.BOOK_FORMAT_PDF);
      req.setContentList(contentList);

      ResponseEntity<Book> res = controller.attachBook(nb, req, pdfFile(STUB_PDF_BYTES));

      Book created = res.getBody();
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
    void rejectsSecondAttachForSameNotebook() throws Exception {
      Notebook nb = notebookWithBook();
      ApiException ex =
          assertThrows(
              ApiException.class,
              () ->
                  controller.attachBook(
                      nb, attachRequest(node("Second")), pdfFile(STUB_PDF_BYTES)));
      assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.RESOURCE_CONFLICT));
    }

    @Test
    void rejectsUnauthorizedNotebook() {
      Notebook otherNb = otherUsersNotebook();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.attachBook(otherNb, attachRequest(node("A")), pdfFile(STUB_PDF_BYTES)));
    }

    @Test
    void rejectsEpubWhenMetaInfEncryptionXmlPresent() throws Exception {
      Notebook nb = myNotebook();
      byte[] epubBytes = readFixtureEpubInvalidDrmEncryptionXml();
      AttachBookRequest req = epubAttachRequest("DRM EPUB");
      ApiException ex =
          assertThrows(
              ApiException.class, () -> controller.attachBook(nb, req, epubFile(epubBytes)));
      assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.BINDING_ERROR));
      assertThat(ex.getErrorBody().getMessage(), containsString("encrypted or DRM-protected"));
    }

    @Test
    void persistsEpubAttachWithFormatAndStorageRef() throws Exception {
      Notebook nb = myNotebook();
      byte[] epubBytes = readFixtureEpubValidMinimal();
      AttachBookRequest req = epubAttachRequest("Minimal EPUB");

      ResponseEntity<Book> res = controller.attachBook(nb, req, epubFile(epubBytes));

      assertThat(res.getStatusCode(), equalTo(HttpStatus.CREATED));
      assertThat(res.getBody(), notNullValue());
      Book created = res.getBody();
      assertThat(created.getFormat(), equalTo(BookReadingWireConstants.BOOK_FORMAT_EPUB));
      assertThat(created.getBookName(), equalTo("Minimal EPUB"));
      assertThat(created.getSourceFileRef(), notNullValue());
      assertThat(created.getSourceFileRef().isBlank(), equalTo(false));
      List<BookBlock> createdPreorder = blocksByLayoutOrder(created);
      assertThat(createdPreorder, hasSize(5));
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

      Book detail = controller.getBook(nb);
      assertThat(detail.getFormat(), equalTo(BookReadingWireConstants.BOOK_FORMAT_EPUB));
      assertThat(detail.getSourceFileRef(), equalTo(created.getSourceFileRef()));
      assertThat(
          blocksByLayoutOrder(detail).stream().map(BookBlock::getStructuralTitle).toList(),
          equalTo(createdPreorder.stream().map(BookBlock::getStructuralTitle).toList()));
    }

    @Test
    void rejectsEpubWhenLayoutIncluded() {
      Notebook nb = myNotebook();
      AttachBookRequest req = attachRequest(node("A"));
      req.setFormat(BookReadingWireConstants.BOOK_FORMAT_EPUB);
      ApiException ex =
          assertThrows(
              ApiException.class, () -> controller.attachBook(nb, req, epubFile(STUB_PDF_BYTES)));
      assertThat(ex.getMessage(), equalTo("EPUB attach must not include layout or contentList"));
    }

    @Test
    void rejectsUnknownBookFormat() {
      Notebook nb = myNotebook();
      AttachBookRequest req = attachRequest(node("A"));
      req.setFormat("doc");
      ApiException ex =
          assertThrows(
              ApiException.class, () -> controller.attachBook(nb, req, pdfFile(STUB_PDF_BYTES)));
      assertThat(ex.getMessage(), equalTo("format must be \"pdf\" or \"epub\""));
    }

    @Test
    void rejectsEmptyRoots() {
      Notebook nb = myNotebook();
      AttachBookRequest req = attachRequest();
      req.getLayout().setRoots(new ArrayList<>());
      assertThrows(
          ApiException.class, () -> controller.attachBook(nb, req, pdfFile(STUB_PDF_BYTES)));
    }

    @Test
    void rejectsExcessiveDepth() {
      Notebook nb = myNotebook();
      AttachBookLayoutNodeRequest deep = node("leaf");
      for (int i = 0; i < BookReadingWireConstants.MAX_LAYOUT_DEPTH; i++) {
        deep = node("d" + i, deep);
      }
      AttachBookLayoutNodeRequest root = deep;
      assertThrows(
          ApiException.class,
          () -> controller.attachBook(nb, attachRequest(root), pdfFile(STUB_PDF_BYTES)));
    }

    @Test
    void rejectsEmptyFile() throws Exception {
      Notebook nb = myNotebook();
      org.springframework.mock.web.MockMultipartFile empty =
          new org.springframework.mock.web.MockMultipartFile(
              "file", "book.pdf", "application/pdf", new byte[0]);
      assertThrows(
          ApiException.class, () -> controller.attachBook(nb, attachRequest(node("A")), empty));
    }

    @Test
    void persistsContentBlocksForEachBlock() throws Exception {
      Notebook nb = myNotebook();
      java.util.Map<String, Object> bodyItem = new java.util.LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "Some body text");
      bodyItem.put("page_idx", 1);
      AttachBookLayoutNodeRequest n = node("Chapter 1");
      n.setContentBlocks(
          new ArrayList<>(
              List.of(headingBlock("Chapter 1", 1, 0, List.of(0.0, 0.0, 100.0, 20.0)), bodyItem)));

      ResponseEntity<Book> res =
          controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));

      Book created = res.getBody();
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
      assertThat(
          detail.getBlocks().stream().map(BookBlock::getId).toList(), hasItem(block.getId()));
      BookBlock detailChapter =
          detail.getBlocks().stream()
              .filter(b -> b.getId().equals(block.getId()))
              .findFirst()
              .orElseThrow();
      assertThat(detailChapter.getContentBlocks(), hasSize(2));

      String json = objectMapper.writerWithView(BookViews.Full.class).writeValueAsString(detail);
      JsonNode blocks = objectMapper.readTree(json).get("blocks");
      JsonNode chapter = null;
      for (JsonNode b : blocks) {
        if (b.get("id").asInt() == block.getId()) {
          chapter = b;
          break;
        }
      }
      assertThat(chapter, notNullValue());
      JsonNode wireCbs = chapter.get("contentBlocks");
      assertThat(wireCbs.isArray(), equalTo(true));
      assertThat(wireCbs.size(), equalTo(2));
      assertThat(wireCbs.get(0).get("id").asInt(), equalTo(cbs.get(0).getId()));
      assertThat(wireCbs.get(1).get("id").asInt(), equalTo(cbs.get(1).getId()));
      assertThat(wireCbs.get(0).get("type").asText(), equalTo("text"));
      assertThat(wireCbs.get(1).get("type").asText(), equalTo("text"));
      assertThat(wireCbs.get(1).get("pageIdx").asInt(), equalTo(1));
      assertThat(wireCbs.get(0).get("raw"), nullValue());
      assertThat(wireCbs.get(1).get("raw"), nullValue());

      Book detailAgain = controller.getBook(nb);
      String jsonAgain =
          objectMapper.writerWithView(BookViews.Full.class).writeValueAsString(detailAgain);
      JsonNode blocksAgain = objectMapper.readTree(jsonAgain).get("blocks");
      JsonNode chapterAgain = null;
      for (JsonNode b : blocksAgain) {
        if (b.get("id").asInt() == block.getId()) {
          chapterAgain = b;
          break;
        }
      }
      assertThat(chapterAgain, notNullValue());
      assertThat(
          chapterAgain.get("contentBlocks").get(0).get("id").asInt(), equalTo(cbs.get(0).getId()));
      assertThat(
          chapterAgain.get("contentBlocks").get(1).get("id").asInt(), equalTo(cbs.get(1).getId()));
    }

    @Test
    void persistsContentBlocksUnderSyntheticBeginningRoot() throws Exception {
      Notebook nb = myNotebook();
      java.util.Map<String, Object> anchorBlock = new java.util.LinkedHashMap<>();
      anchorBlock.put("type", "beginning_anchor");
      anchorBlock.put("page_idx", 0);
      anchorBlock.put("bbox", new ArrayList<>(List.of(10.0, 70.0, 200.0, 100.0)));
      java.util.Map<String, Object> orphan = new java.util.LinkedHashMap<>();
      orphan.put("type", "text");
      orphan.put("text", "Preface paragraph");
      orphan.put("page_idx", 0);

      AttachBookLayoutNodeRequest beginning = node("*beginning*");
      beginning.setContentBlocks(new ArrayList<>(List.of(anchorBlock, orphan)));
      AttachBookLayoutNodeRequest chapter = node("Chapter 1");

      ResponseEntity<Book> res =
          controller.attachBook(nb, attachRequest(beginning, chapter), pdfFile(STUB_PDF_BYTES));

      Book created = res.getBody();
      assertThat(created, notNullValue());
      List<BookBlock> roots = rootBlocksSorted(created);
      assertThat(roots, hasSize(2));
      assertThat(roots.getFirst().getStructuralTitle(), equalTo("*beginning*"));
      assertThat(roots.get(1).getStructuralTitle(), equalTo("Chapter 1"));

      List<BookContentBlock> beginningCbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(
              roots.getFirst().getId());
      assertThat(beginningCbs, hasSize(2));
      assertThat(beginningCbs.getFirst().getSiblingOrder(), equalTo(0));
      assertThat(beginningCbs.getFirst().getType(), equalTo("beginning_anchor"));
      assertThat(beginningCbs.get(1).getSiblingOrder(), equalTo(1));
      assertThat(beginningCbs.get(1).getType(), equalTo("text"));
      assertThat(beginningCbs.get(1).getPageIdx(), equalTo(0));
    }

    @Test
    void persistsFromContentListBuildsBeginningAndChapter() throws Exception {
      Notebook nb = myNotebook();
      List<Object> cl = new ArrayList<>();
      java.util.Map<String, Object> orphan = new java.util.LinkedHashMap<>();
      orphan.put("type", "text");
      orphan.put("text", "Orphan body");
      orphan.put("bbox", new ArrayList<>(List.of(10, 100, 200, 130)));
      orphan.put("page_idx", 0);
      cl.add(orphan);
      cl.add(headingBlock("Chapter One", 2, 1, List.of(1.0, 200.0, 300.0, 240.0)));

      AttachBookRequest req = new AttachBookRequest();
      req.setBookName("Linear Algebra");
      req.setFormat(BookReadingWireConstants.BOOK_FORMAT_PDF);
      req.setContentList(cl);

      ResponseEntity<Book> res = controller.attachBook(nb, req, pdfFile(STUB_PDF_BYTES));

      Book created = res.getBody();
      assertThat(created, notNullValue());
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
    void rejectsBothLayoutRootsAndContentList() {
      Notebook nb = myNotebook();
      AttachBookRequest req = attachRequest(node("A"));
      List<Object> cl = new ArrayList<>();
      java.util.Map<String, Object> noise = new java.util.LinkedHashMap<>();
      noise.put("type", "text");
      noise.put("text", "only body");
      noise.put("page_idx", 0);
      noise.put("bbox", new ArrayList<>(List.of(0.0, 0.0, 1.0, 1.0)));
      cl.add(noise);
      req.setContentList(cl);
      assertThrows(
          ApiException.class, () -> controller.attachBook(nb, req, pdfFile(STUB_PDF_BYTES)));
    }

    @Test
    void rejectsNeitherLayoutNorContentList() {
      Notebook nb = myNotebook();
      AttachBookRequest req = new AttachBookRequest();
      req.setBookName("X");
      req.setFormat(BookReadingWireConstants.BOOK_FORMAT_PDF);
      assertThrows(
          ApiException.class, () -> controller.attachBook(nb, req, pdfFile(STUB_PDF_BYTES)));
    }

    @Test
    void rejectsContentListThatProducesNoBlocks() {
      Notebook nb = myNotebook();
      List<Object> cl = new ArrayList<>();
      java.util.Map<String, Object> pn = new java.util.LinkedHashMap<>();
      pn.put("type", "page_number");
      pn.put("text", "1");
      pn.put("page_idx", 0);
      cl.add(pn);
      AttachBookRequest req = new AttachBookRequest();
      req.setBookName("Book");
      req.setFormat(BookReadingWireConstants.BOOK_FORMAT_PDF);
      req.setContentList(cl);
      assertThrows(
          ApiException.class, () -> controller.attachBook(nb, req, pdfFile(STUB_PDF_BYTES)));
    }
  }
}
