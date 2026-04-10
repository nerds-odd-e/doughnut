package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookBlock;
import com.odde.doughnut.entities.BookBlockReadingRecord;
import com.odde.doughnut.entities.BookContentBlock;
import com.odde.doughnut.entities.BookUserLastReadPosition;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.BookBlockReadingRecordRepository;
import com.odde.doughnut.entities.repositories.BookContentBlockRepository;
import com.odde.doughnut.entities.repositories.BookRepository;
import com.odde.doughnut.entities.repositories.BookUserLastReadPositionRepository;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.book.BookReadingWireConstants;
import com.odde.doughnut.services.book.BookStorage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

class NotebookBooksControllerTest extends ControllerTestBase {

  private static final byte[] STUB_PDF_BYTES = new byte[] {1};

  @Autowired NotebookBooksController controller;
  @Autowired BookRepository bookRepository;
  @Autowired BookUserLastReadPositionRepository bookUserLastReadPositionRepository;
  @Autowired BookBlockReadingRecordRepository bookBlockReadingRecordRepository;
  @Autowired BookContentBlockRepository bookContentBlockRepository;
  @Autowired BookStorage bookStorage;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private Notebook myNotebook() {
    return makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
  }

  private Notebook notebookWithBook() {
    return makeMe
        .aNotebook()
        .creatorAndOwner(currentUser.getUser())
        .withBook("Linear Algebra")
        .please();
  }

  private Book bookOf(Notebook nb) {
    return bookRepository.findByNotebook_Id(nb.getId()).orElseThrow();
  }

  private void setSourceFileRef(Notebook nb, String ref) {
    Book book = bookOf(nb);
    book.setSourceFileRef(ref);
    makeMe.entityPersister.save(book);
    makeMe.entityPersister.flush();
  }

  private Notebook otherUsersNotebook() {
    return makeMe.aNotebook().creatorAndOwner(makeMe.aUser().please()).please();
  }

  private Notebook otherUsersNotebookWithBook() {
    User other = makeMe.aUser().please();
    return makeMe.aNotebook().creatorAndOwner(other).withBook("Linear Algebra").please();
  }

  private static MultipartFile pdfFile(byte[] content) {
    return new MockMultipartFile("file", "book.pdf", "application/pdf", content);
  }

  private static ServletWebRequest webRequest() {
    return new ServletWebRequest(new MockHttpServletRequest());
  }

  private static List<BookBlock> rootBlocksSorted(Book book) {
    return book.getBlocks().stream()
        .filter(r -> r.getParentBlockId() == null)
        .sorted(Comparator.comparingLong(BookBlock::getSiblingOrder))
        .toList();
  }

  private static List<BookBlock> childrenOf(Book book, BookBlock parent) {
    return book.getBlocks().stream()
        .filter(r -> Objects.equals(r.getParentBlockId(), parent.getId()))
        .sorted(Comparator.comparingLong(BookBlock::getSiblingOrder))
        .toList();
  }

  private static AttachBookAnchorRequest anchor(String value) {
    AttachBookAnchorRequest a = new AttachBookAnchorRequest();
    a.setValue(value);
    return a;
  }

  private static AttachBookLayoutNodeRequest node(
      String title, AttachBookLayoutNodeRequest... kids) {
    AttachBookLayoutNodeRequest n = new AttachBookLayoutNodeRequest();
    n.setTitle(title);
    n.setStartAnchor(anchor("{\"p\":1}"));
    if (kids != null && kids.length > 0) {
      n.setChildren(new ArrayList<>(List.of(kids)));
    }
    return n;
  }

  private static AttachBookRequest attachRequest(AttachBookLayoutNodeRequest... roots) {
    AttachBookLayoutRequest layout = new AttachBookLayoutRequest();
    layout.setRoots(new ArrayList<>(List.of(roots)));
    AttachBookRequest r = new AttachBookRequest();
    r.setBookName("Linear Algebra");
    r.setFormat(BookReadingWireConstants.BOOK_FORMAT_PDF);
    r.setLayout(layout);
    return r;
  }

  private static BookLastReadPositionRequest lastReadBody(int pageIndex, int normalizedY) {
    BookLastReadPositionRequest r = new BookLastReadPositionRequest();
    r.setPageIndex(pageIndex);
    r.setNormalizedY(normalizedY);
    return r;
  }

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
    void rejectsNonPdfFormat() {
      Notebook nb = myNotebook();
      AttachBookRequest req = attachRequest(node("A"));
      req.setFormat("epub");
      assertThrows(
          ApiException.class, () -> controller.attachBook(nb, req, pdfFile(STUB_PDF_BYTES)));
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
      MultipartFile empty =
          new MockMultipartFile("file", "book.pdf", "application/pdf", new byte[0]);
      assertThrows(
          ApiException.class, () -> controller.attachBook(nb, attachRequest(node("A")), empty));
    }

    @Test
    void persistsContentBlocksForEachBlock() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> bodyItem = new LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "Some body text");
      bodyItem.put("page_idx", 1);
      AttachBookLayoutNodeRequest n = node("Chapter 1");
      n.setContentBlocks(new ArrayList<>(List.of(bodyItem)));

      ResponseEntity<Book> res =
          controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));

      Book created = res.getBody();
      BookBlock block = rootBlocksSorted(created).getFirst();
      List<BookContentBlock> cbs =
          bookContentBlockRepository.findAllByBookBlock_IdOrderBySiblingOrder(block.getId());
      assertThat(cbs, hasSize(1));
      assertThat(cbs.get(0).getSiblingOrder(), equalTo(0));
      assertThat(cbs.get(0).getType(), equalTo("text"));
      assertThat(cbs.get(0).getPageIdx(), equalTo(1));
    }

    @Test
    void persistsContentBlocksUnderSyntheticBeginningRoot() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> orphan = new LinkedHashMap<>();
      orphan.put("type", "text");
      orphan.put("text", "Preface paragraph");
      orphan.put("page_idx", 0);

      AttachBookLayoutNodeRequest beginning = node("*beginning*");
      beginning.setContentBlocks(new ArrayList<>(List.of(orphan)));
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
      assertThat(beginningCbs, hasSize(1));
      assertThat(beginningCbs.getFirst().getSiblingOrder(), equalTo(0));
      assertThat(beginningCbs.getFirst().getType(), equalTo("text"));
      assertThat(beginningCbs.getFirst().getPageIdx(), equalTo(0));
    }
  }

  @Nested
  class GetBook {
    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = myNotebook();
      assertThrows(ResponseStatusException.class, () -> controller.getBook(nb));
    }

    @Test
    void getBookReturnsBookWithNonBlankSourceFileRef() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      String ref = controller.getBook(nb).getSourceFileRef();
      assertThat(ref, notNullValue());
      assertThat(ref.isBlank(), equalTo(false));
    }

    @Test
    void doesNotReturnAnotherNotebooksBook() {
      notebookWithBook();
      Notebook nb2 = myNotebook();
      assertThrows(ResponseStatusException.class, () -> controller.getBook(nb2));
    }

    @Test
    void hasDirectContentFalseForHeadingOnlyBlock() throws Exception {
      Notebook nb = myNotebook();
      AttachBookLayoutNodeRequest n = node("Section A");
      n.setContentBlocks(new ArrayList<>());
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      Book book = controller.getBook(nb);

      BookBlock block = rootBlocksSorted(book).getFirst();
      assertThat(block.getHasDirectContent(), equalTo(false));
    }

    @Test
    void hasDirectContentTrueWhenBlockHasBodyText() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> bodyItem = new LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "Some body text");
      bodyItem.put("page_idx", 0);
      AttachBookLayoutNodeRequest n = node("Section B");
      n.setContentBlocks(new ArrayList<>(List.of(bodyItem)));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      Book book = controller.getBook(nb);

      BookBlock block = rootBlocksSorted(book).getFirst();
      assertThat(block.getHasDirectContent(), equalTo(true));
      assertThat(block.getAllBboxes(), empty());
    }

    @Test
    void allBboxesIncludesStartAnchorBboxFirst() throws Exception {
      Notebook nb = myNotebook();
      AttachBookLayoutNodeRequest n = new AttachBookLayoutNodeRequest();
      n.setTitle("Headed Section");
      n.setStartAnchor(anchor("{\"page_idx\":1,\"bbox\":[5.0,10.0,200.0,50.0]}"));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      BookBlock block = rootBlocksSorted(controller.getBook(nb)).getFirst();
      assertThat(block.getAllBboxes(), hasSize(1));
      assertThat(block.getAllBboxes().getFirst().pageIndex(), equalTo(1));
      assertThat(block.getAllBboxes().getFirst().bbox(), equalTo(List.of(5.0, 10.0, 200.0, 50.0)));
    }

    @Test
    void allBboxesIncludesAnchorBboxThenContentBboxes() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> bodyItem = new LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "Body paragraph");
      bodyItem.put("page_idx", 2);
      bodyItem.put("bbox", new ArrayList<>(List.of(10.0, 20.0, 300.0, 400.0)));
      AttachBookLayoutNodeRequest n = new AttachBookLayoutNodeRequest();
      n.setTitle("Section With Bbox");
      n.setStartAnchor(anchor("{\"page_idx\":2,\"bbox\":[1.0,2.0,100.0,15.0]}"));
      n.setContentBlocks(new ArrayList<>(List.of(bodyItem)));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      BookBlock block = rootBlocksSorted(controller.getBook(nb)).getFirst();
      assertThat(block.getAllBboxes(), hasSize(2));
      assertThat(block.getAllBboxes().getFirst().pageIndex(), equalTo(2));
      assertThat(block.getAllBboxes().getFirst().bbox(), equalTo(List.of(1.0, 2.0, 100.0, 15.0)));
      assertThat(block.getAllBboxes().get(1).pageIndex(), equalTo(2));
      assertThat(block.getAllBboxes().get(1).bbox(), equalTo(List.of(10.0, 20.0, 300.0, 400.0)));
    }

    @Test
    void allBboxesSkipsHeaderFooterPageChromeAndStructuralHeadings() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> header = new LinkedHashMap<>();
      header.put("type", "header");
      header.put("text", "Running title");
      header.put("page_idx", 2);
      header.put("bbox", new ArrayList<>(List.of(1.0, 1.0, 50.0, 10.0)));
      Map<String, Object> footer = new LinkedHashMap<>();
      footer.put("type", "footer");
      footer.put("text", "copyright");
      footer.put("page_idx", 2);
      footer.put("bbox", new ArrayList<>(List.of(1.0, 500.0, 50.0, 510.0)));
      Map<String, Object> pageNum = new LinkedHashMap<>();
      pageNum.put("type", "page_number");
      pageNum.put("text", "7");
      pageNum.put("page_idx", 2);
      pageNum.put("bbox", new ArrayList<>(List.of(400.0, 500.0, 410.0, 510.0)));
      Map<String, Object> heading = new LinkedHashMap<>();
      heading.put("type", "text");
      heading.put("text_level", 2);
      heading.put("text", "2.1 Section");
      heading.put("page_idx", 2);
      heading.put("bbox", new ArrayList<>(List.of(15.0, 30.0, 200.0, 45.0)));
      Map<String, Object> bodyItem = new LinkedHashMap<>();
      bodyItem.put("type", "text");
      bodyItem.put("text", "Body paragraph");
      bodyItem.put("page_idx", 2);
      bodyItem.put("bbox", new ArrayList<>(List.of(10.0, 20.0, 300.0, 400.0)));
      AttachBookLayoutNodeRequest n = new AttachBookLayoutNodeRequest();
      n.setTitle("Section With Noise");
      n.setStartAnchor(anchor("{\"page_idx\":2,\"bbox\":[1.0,2.0,100.0,15.0]}"));
      n.setContentBlocks(new ArrayList<>(List.of(header, footer, pageNum, heading, bodyItem)));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      BookBlock block = rootBlocksSorted(controller.getBook(nb)).getFirst();
      assertThat(block.getAllBboxes(), hasSize(2));
      assertThat(block.getAllBboxes().getFirst().bbox(), equalTo(List.of(1.0, 2.0, 100.0, 15.0)));
      assertThat(block.getAllBboxes().get(1).bbox(), equalTo(List.of(10.0, 20.0, 300.0, 400.0)));
    }

    @Test
    void hasDirectContentFalseWhenOnlyPageNumberContentBlocks() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> pageNum = new LinkedHashMap<>();
      pageNum.put("type", "page_number");
      pageNum.put("text", "1");
      pageNum.put("page_idx", 0);
      AttachBookLayoutNodeRequest n = node("Section C");
      n.setContentBlocks(new ArrayList<>(List.of(pageNum)));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      BookBlock block = rootBlocksSorted(controller.getBook(nb)).getFirst();
      assertThat(block.getHasDirectContent(), equalTo(false));
    }

    @Test
    void hasDirectContentFalseWhenOnlyHeaderContentBlocks() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> header = new LinkedHashMap<>();
      header.put("type", "header");
      header.put("text", "Running title");
      header.put("page_idx", 0);
      AttachBookLayoutNodeRequest n = node("Section D");
      n.setContentBlocks(new ArrayList<>(List.of(header)));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      BookBlock block = rootBlocksSorted(controller.getBook(nb)).getFirst();
      assertThat(block.getHasDirectContent(), equalTo(false));
    }

    @Test
    void hasDirectContentTrueWhenOnlyTableContentBlocks() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> table = new LinkedHashMap<>();
      table.put("type", "table");
      table.put("table_body", "<table></table>");
      table.put("page_idx", 0);
      AttachBookLayoutNodeRequest n = node("Section E");
      n.setContentBlocks(new ArrayList<>(List.of(table)));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      BookBlock block = rootBlocksSorted(controller.getBook(nb)).getFirst();
      assertThat(block.getHasDirectContent(), equalTo(true));
    }

    @Test
    void hasDirectContentTrueWhenOnlyImageContentBlocks() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> image = new LinkedHashMap<>();
      image.put("type", "image");
      image.put("img_path", "x.png");
      image.put("page_idx", 0);
      AttachBookLayoutNodeRequest n = node("Section F");
      n.setContentBlocks(new ArrayList<>(List.of(image)));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      BookBlock block = rootBlocksSorted(controller.getBook(nb)).getFirst();
      assertThat(block.getHasDirectContent(), equalTo(true));
    }

    @Test
    void hasDirectContentFalseWhenLegacyHeadingShapedTextOnly() throws Exception {
      Notebook nb = myNotebook();
      Map<String, Object> headingRow = new LinkedHashMap<>();
      headingRow.put("type", "text");
      headingRow.put("text_level", 2);
      headingRow.put("text", "2.1 A heading");
      headingRow.put("page_idx", 0);
      AttachBookLayoutNodeRequest n = node("Section G");
      n.setContentBlocks(new ArrayList<>(List.of(headingRow)));
      controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES));
      makeMe.entityPersister.flushAndClear();

      BookBlock block = rootBlocksSorted(controller.getBook(nb)).getFirst();
      assertThat(block.getHasDirectContent(), equalTo(false));
    }
  }

  @Nested
  class GetBookFile {
    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = myNotebook();
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(webRequest(), nb));
    }

    @Test
    void rejectsNotebookWithoutReadAccess() {
      Notebook otherNb = otherUsersNotebook();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.getBookFile(webRequest(), otherNb));
    }

    @Test
    void returnsPdfWhenSourceFileRefPointsAtBlob() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};
      String ref = bookStorage.put(pdfBytes);
      setSourceFileRef(nb, ref);
      String expectedEtag =
          "\"" + DigestUtils.md5DigestAsHex(ref.getBytes(StandardCharsets.UTF_8)) + "\"";

      ResponseEntity<byte[]> res = controller.getBookFile(webRequest(), nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(res.getBody(), equalTo(pdfBytes));
      assertThat(res.getHeaders().getContentType(), equalTo(MediaType.APPLICATION_PDF));
      assertThat(res.getHeaders().getETag(), equalTo(expectedEtag));
      assertThat(
          res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION),
          equalTo("inline; filename=\"Linear Algebra.pdf\""));
      assertThat(res.getHeaders().getCacheControl(), containsString("private"));
      assertThat(res.getHeaders().getCacheControl(), containsString("max-age="));
    }

    @Test
    void returns304WhenIfNoneMatchMatchesEtag() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};
      String ref = bookStorage.put(pdfBytes);
      setSourceFileRef(nb, ref);
      String etag = "\"" + DigestUtils.md5DigestAsHex(ref.getBytes(StandardCharsets.UTF_8)) + "\"";

      MockHttpServletRequest req = new MockHttpServletRequest();
      req.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
      ResponseEntity<byte[]> res = controller.getBookFile(new ServletWebRequest(req), nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.NOT_MODIFIED));
      assertThat(res.getBody(), nullValue());
      assertThat(res.getHeaders().getETag(), equalTo(etag));
    }

    @Test
    void returns404WhenSourceFileRefIsNotNumeric() {
      Notebook nb = notebookWithBook();
      setSourceFileRef(nb, "not-an-id");
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(webRequest(), nb));
    }

    @Test
    void returns404WhenSourceFileRefBlobMissing() {
      Notebook nb = notebookWithBook();
      setSourceFileRef(nb, String.valueOf(Integer.MAX_VALUE));
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(webRequest(), nb));
    }
  }

  @Nested
  class DeleteBook {
    @Test
    void removesBookRowAndStoredBytes() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      String ref = bookOf(nb).getSourceFileRef();

      controller.deleteBook(nb);

      assertThat(bookRepository.findByNotebook_Id(nb.getId()).isEmpty(), equalTo(true));
      assertThat(bookStorage.get(ref).isEmpty(), equalTo(true));
      assertThrows(ResponseStatusException.class, () -> controller.getBook(nb));
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(webRequest(), nb));
    }

    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = myNotebook();
      assertThrows(ResponseStatusException.class, () -> controller.deleteBook(nb));
    }

    @Test
    void rejectsUnauthorizedNotebook() {
      Notebook otherNb = otherUsersNotebook();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.deleteBook(otherNb));
    }
  }

  @Nested
  class PatchReadingPosition {
    @Test
    void persistsSnapshotForCurrentUserAndBook() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();

      controller.patchReadingPosition(nb, lastReadBody(2, 750));

      var stored =
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookOf(nb).getId())
              .orElseThrow();
      assertThat(stored.getPageIndex(), equalTo(2));
      assertThat(stored.getNormalizedY(), equalTo(750));
    }

    @Test
    void secondPatchUpdatesSameRow() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();

      controller.patchReadingPosition(nb, lastReadBody(0, 100));
      controller.patchReadingPosition(nb, lastReadBody(5, 0));

      assertThat(bookUserLastReadPositionRepository.count(), equalTo(1L));
      var stored =
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookOf(nb).getId())
              .orElseThrow();
      assertThat(stored.getPageIndex(), equalTo(5));
      assertThat(stored.getNormalizedY(), equalTo(0));
    }

    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = myNotebook();
      assertThrows(
          ResponseStatusException.class,
          () -> controller.patchReadingPosition(nb, lastReadBody(0, 0)));
    }

    @Test
    void rejectsNotebookWithoutReadAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.patchReadingPosition(otherNb, lastReadBody(0, 0)));
    }

    @Test
    void requiresLoggedInUser() {
      Notebook nb = notebookWithBook();
      currentUser.setUser(null);
      assertThrows(
          ResponseStatusException.class,
          () -> controller.patchReadingPosition(nb, lastReadBody(0, 0)));
    }

    @Test
    void removesPositionWhenBookDeleted() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      int bookId = bookOf(nb).getId();
      controller.patchReadingPosition(nb, lastReadBody(1, 500));

      controller.deleteBook(nb);

      assertThat(
          bookUserLastReadPositionRepository
              .findByUser_IdAndBook_Id(currentUser.getUser().getId(), bookId)
              .isEmpty(),
          equalTo(true));
    }
  }

  @Nested
  class GetReadingPosition {
    @Test
    void returnsSavedSnapshotAfterPatch() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      controller.patchReadingPosition(nb, lastReadBody(3, 420));

      ResponseEntity<BookUserLastReadPosition> res = controller.getReadingPosition(nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(res.getBody(), notNullValue());
      assertThat(res.getBody().getPageIndex(), equalTo(3));
      assertThat(res.getBody().getNormalizedY(), equalTo(420));
    }

    @Test
    void returns204WhenNoSnapshotStored() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();

      ResponseEntity<BookUserLastReadPosition> res = controller.getReadingPosition(nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
      assertThat(res.getBody(), nullValue());
    }

    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = myNotebook();
      assertThrows(ResponseStatusException.class, () -> controller.getReadingPosition(nb));
    }

    @Test
    void rejectsNotebookWithoutReadAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.getReadingPosition(otherNb));
    }

    @Test
    void requiresLoggedInUser() {
      Notebook nb = notebookWithBook();
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.getReadingPosition(nb));
    }
  }

  @Nested
  class GetBookReadingRecords {
    @Test
    void returnsRecordForMarkedRange() throws Exception {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
      Notebook nb = notebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(nb)).getFirst();
      controller.putBlockReadingRecord(nb, range, null);

      var list = controller.getBookReadingRecords(nb);
      assertThat(list, hasSize(1));
      assertThat(list.getFirst().getBookBlockId(), equalTo(range.getId()));
      assertThat(list.getFirst().getStatus(), equalTo(BookBlockReadingRecord.STATUS_READ));
      assertThat(
          list.getFirst().getCompletedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void returnsOnlyMarkedRangesAmongSiblings() throws Exception {
      Notebook nb = myNotebook();
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};
      controller.attachBook(nb, attachRequest(node("2.1"), node("2.2")), pdfFile(pdfBytes));
      Book book = bookOf(nb);
      List<BookBlock> roots = rootBlocksSorted(book);
      BookBlock first = roots.getFirst();
      controller.putBlockReadingRecord(nb, first, null);

      var list = controller.getBookReadingRecords(nb);
      assertThat(list, hasSize(1));
      assertThat(list.getFirst().getBookBlockId(), equalTo(first.getId()));
    }

    @Test
    void doesNotIncludeAnotherUsersRecords() throws Exception {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
      Notebook nb = notebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(nb)).getFirst();
      User other = makeMe.aUser().please();
      var otherRow = new BookBlockReadingRecord();
      otherRow.setUser(other);
      otherRow.setBookBlock(range);
      otherRow.setStatus(BookBlockReadingRecord.STATUS_READ);
      otherRow.setCompletedAt(testabilitySettings.getCurrentUTCTimestamp());
      makeMe.entityPersister.save(otherRow);
      makeMe.entityPersister.flush();

      assertThat(controller.getBookReadingRecords(nb), empty());
    }

    @Test
    void returnsEmptyWhenNoRecords() throws Exception {
      Notebook nb = notebookWithBook();
      assertThat(controller.getBookReadingRecords(nb), empty());
    }
  }

  @Nested
  class PutBlockReadingRecord {
    @Test
    void persistsReadRecordForCurrentUserAndRange() throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
      Notebook nb = notebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(nb)).getFirst();

      var returned = controller.putBlockReadingRecord(nb, range, null);
      assertThat(returned, hasSize(1));
      assertThat(returned.getFirst().getBookBlockId(), equalTo(range.getId()));
      assertThat(returned.getFirst().getStatus(), equalTo(BookBlockReadingRecord.STATUS_READ));
      assertThat(
          returned.getFirst().getCompletedAt(),
          equalTo(testabilitySettings.getCurrentUTCTimestamp()));

      var stored =
          bookBlockReadingRecordRepository
              .findByUser_IdAndBookBlock_Id(currentUser.getUser().getId(), range.getId())
              .orElseThrow();
      assertThat(stored.getStatus(), equalTo(BookBlockReadingRecord.STATUS_READ));
      assertThat(stored.getCompletedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void returns404WhenNotebookHasNoBook() {
      Notebook nbEmpty = myNotebook();
      Notebook nbWith = notebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(nbWith)).getFirst();

      assertThrows(
          ResponseStatusException.class,
          () -> controller.putBlockReadingRecord(nbEmpty, range, null));
    }

    @Test
    void returns404WhenRangeBelongsToAnotherNotebooksBook() {
      Notebook otherNb = otherUsersNotebookWithBook();
      BookBlock otherRange = rootBlocksSorted(bookOf(otherNb)).getFirst();
      Notebook myNb = notebookWithBook();

      assertThrows(
          ResponseStatusException.class,
          () -> controller.putBlockReadingRecord(myNb, otherRange, null));
    }

    @Test
    void rejectsNotebookWithoutReadAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(otherNb)).getFirst();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.putBlockReadingRecord(otherNb, range, null));
    }

    @Test
    void requiresLoggedInUser() {
      Notebook nb = notebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(nb)).getFirst();
      currentUser.setUser(null);

      assertThrows(
          ResponseStatusException.class, () -> controller.putBlockReadingRecord(nb, range, null));
    }

    @Test
    void persistsSkimmedAndSkippedRejectsBadStatus() throws Exception {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
      Notebook nb = myNotebook();
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};
      controller.attachBook(nb, attachRequest(node("Block A"), node("Block B")), pdfFile(pdfBytes));
      List<BookBlock> roots = rootBlocksSorted(bookOf(nb));
      assertThat(roots, hasSize(2));
      BookBlock first = roots.getFirst();
      BookBlock second = roots.get(1);

      var skimBody = new BookBlockReadingRecordPutRequest();
      skimBody.setStatus(BookBlockReadingRecord.STATUS_SKIMMED);
      var afterSkim = controller.putBlockReadingRecord(nb, first, skimBody);
      assertThat(afterSkim, hasSize(1));
      assertThat(afterSkim.getFirst().getStatus(), equalTo(BookBlockReadingRecord.STATUS_SKIMMED));

      var skipBody = new BookBlockReadingRecordPutRequest();
      skipBody.setStatus(BookBlockReadingRecord.STATUS_SKIPPED);
      var afterSkip = controller.putBlockReadingRecord(nb, second, skipBody);
      assertThat(afterSkip, hasSize(2));
      assertThat(
          afterSkip.stream()
              .filter(i -> i.getBookBlockId().equals(first.getId()))
              .findFirst()
              .orElseThrow()
              .getStatus(),
          equalTo(BookBlockReadingRecord.STATUS_SKIMMED));
      assertThat(
          afterSkip.stream()
              .filter(i -> i.getBookBlockId().equals(second.getId()))
              .findFirst()
              .orElseThrow()
              .getStatus(),
          equalTo(BookBlockReadingRecord.STATUS_SKIPPED));

      var bad = new BookBlockReadingRecordPutRequest();
      bad.setStatus("NOT_A_STATUS");
      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> controller.putBlockReadingRecord(nb, first, bad));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
      assertThat(ex.getReason(), equalTo("Invalid reading record status"));
    }

    @Test
    void secondPutKeepsOneRowOverwritesStatusAndUpdatesCompletedAt()
        throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(0, 10).please());
      Notebook nb = notebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(nb)).getFirst();

      var skim = new BookBlockReadingRecordPutRequest();
      skim.setStatus(BookBlockReadingRecord.STATUS_SKIMMED);
      assertThat(controller.putBlockReadingRecord(nb, range, skim), hasSize(1));

      testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(1, 11).please());
      var second = controller.putBlockReadingRecord(nb, range, null);

      assertThat(bookBlockReadingRecordRepository.count(), equalTo(1L));
      assertThat(second.getFirst().getStatus(), equalTo(BookBlockReadingRecord.STATUS_READ));
      assertThat(
          second.getFirst().getCompletedAt(),
          equalTo(testabilitySettings.getCurrentUTCTimestamp()));
      var stored =
          bookBlockReadingRecordRepository
              .findByUser_IdAndBookBlock_Id(currentUser.getUser().getId(), range.getId())
              .orElseThrow();
      assertThat(stored.getStatus(), equalTo(BookBlockReadingRecord.STATUS_READ));
      assertThat(stored.getCompletedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }
  }
}
