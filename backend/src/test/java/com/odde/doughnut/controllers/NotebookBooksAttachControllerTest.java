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
import com.odde.doughnut.entities.BookViews;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.book.BookReadingWireConstants;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

class NotebookBooksAttachControllerTest extends NotebookBooksControllerTestBase {

  @Nested
  class AttachBook {
    @Test
    void persistsNestedOutlineAndReturnsCreatedBook() throws Exception {
      Notebook nb = myNotebook();
      AttachBookLayoutNodeRequest root =
          node("Chapter 1", node("Section 1.1"), node("Section 1.2"));
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e};

      ResponseEntity<Book> res = controller.attachBook(nb, attachRequest(root), pdfFile(pdfBytes));

      assertThat(res.getStatusCode(), equalTo(HttpStatus.CREATED));
      Book created = res.getBody();
      assertThat(created, notNullValue());
      assertThat(created.getId(), notNullValue());
      assertThat(created.getBookName(), equalTo("Linear Algebra"));
      assertThat(created.getSourceFileRef(), not(blankOrNullString()));
      assertThat(created.getBlocks(), hasSize(3));

      BookBlock outRoot = rootBlocksSorted(created).getFirst();
      assertThat(outRoot.getStructuralTitle(), equalTo("Chapter 1"));
      List<BookBlock> children = childrenOf(created, outRoot);
      assertThat(children, hasSize(2));
      assertThat(children.getFirst().getStructuralTitle(), equalTo("Section 1.1"));
      assertThat(children.get(1).getStructuralTitle(), equalTo("Section 1.2"));
    }

    @Test
    void getBookAfterAttachReturnsSameBlockTree() throws Exception {
      Notebook nb = myNotebook();
      AttachBookLayoutNodeRequest root =
          node("Chapter 1", node("Section 1.1"), node("Section 1.2"));
      Book created =
          controller.attachBook(nb, attachRequest(root), pdfFile(STUB_PDF_BYTES)).getBody();
      BookBlock outRoot = rootBlocksSorted(created).getFirst();

      Book detail = controller.getBook(nb);

      assertThat(detail.getBlocks(), hasSize(3));
      BookBlock detailRoot = rootBlocksSorted(detail).getFirst();
      assertThat(detailRoot.getId(), equalTo(outRoot.getId()));
      assertThat(childrenOf(detail, detailRoot), hasSize(2));
    }

    @Test
    void returnsAttachedPdfBytes() throws Exception {
      Notebook nb = myNotebook();
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e};
      controller.attachBook(nb, attachRequest(node("Chapter 1")), pdfFile(pdfBytes));

      ResponseEntity<byte[]> fileRes = controller.getBookFile(webRequest(), nb);

      assertThat(fileRes.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(fileRes.getBody(), equalTo(pdfBytes));
    }

    @Test
    void fullViewSerializesLayoutOrderDepthTitlesAndEmptyContentBlocks() throws Exception {
      Notebook nb = myNotebook();
      controller.attachBook(
          nb,
          attachRequest(node("Chapter 1", node("Section 1.1"), node("Section 1.2"))),
          pdfFile(STUB_PDF_BYTES));
      Book detail = controller.getBook(nb);

      String json = objectMapper.writerWithView(BookViews.Full.class).writeValueAsString(detail);
      JsonNode blocks = objectMapper.readTree(json).get("blocks");
      assertThat(blocks.size(), equalTo(3));
      assertThat(blocks.get(0).get("depth").asInt(), equalTo(0));
      assertThat(blocks.get(1).get("depth").asInt(), equalTo(1));
      assertThat(blocks.get(2).get("depth").asInt(), equalTo(1));
      assertThat(blocks.get(0).get("title").asText(), equalTo("Chapter 1"));
      assertThat(blocks.get(1).get("title").asText(), equalTo("Section 1.1"));
      assertThat(blocks.get(2).get("title").asText(), equalTo("Section 1.2"));
      List<BookBlock> byLayoutSeq = blocksByLayoutOrder(detail);
      for (int i = 0; i < 3; i++) {
        assertThat(blocks.get(i).get("id").asInt(), equalTo(byLayoutSeq.get(i).getId()));
        assertThat(blocks.get(i).get("contentBlocks").size(), equalTo(0));
      }
    }

    @Test
    void rejectsSecondAttachForSameNotebook() {
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
      ApiException ex =
          assertThrows(
              ApiException.class,
              () ->
                  controller.attachBook(
                      nb,
                      epubAttachRequest("DRM EPUB"),
                      epubFile(readFixtureEpubInvalidDrmEncryptionXml())));
      assertThat(ex.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.BINDING_ERROR));
      assertThat(ex.getErrorBody().getMessage(), containsString("encrypted or DRM-protected"));
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
    void rejectsEmptyFile() {
      Notebook nb = myNotebook();
      MockMultipartFile empty =
          new MockMultipartFile("file", "book.pdf", "application/pdf", new byte[0]);
      assertThrows(
          ApiException.class, () -> controller.attachBook(nb, attachRequest(node("A")), empty));
    }

    @Test
    void rejectsBothLayoutRootsAndContentList() {
      Notebook nb = myNotebook();
      AttachBookRequest req = attachRequest(node("A"));
      req.setContentList(List.of(textBlock("only body", 0, List.of(0.0, 0.0, 1.0, 1.0))));
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
  }
}
