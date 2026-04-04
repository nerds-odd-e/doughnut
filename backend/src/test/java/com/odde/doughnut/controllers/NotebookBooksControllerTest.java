package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookRange;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.BookRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.book.BookReadingWireConstants;
import com.odde.doughnut.services.book.BookStorage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

class NotebookBooksControllerTest extends ControllerTestBase {

  private static final byte[] STUB_PDF_BYTES = new byte[] {1};

  @Autowired NotebookBooksController controller;
  @Autowired BookRepository bookRepository;
  @Autowired BookStorage bookStorage;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  private static MultipartFile pdfFile(byte[] content) {
    return new MockMultipartFile("file", "book.pdf", "application/pdf", content);
  }

  private static List<BookRange> rootRangesSorted(Book book) {
    return book.getRanges().stream()
        .filter(r -> r.getParentRangeId() == null)
        .sorted(Comparator.comparingLong(BookRange::getSiblingOrder))
        .toList();
  }

  private static List<BookRange> childrenOf(Book book, BookRange parent) {
    return book.getRanges().stream()
        .filter(r -> Objects.equals(r.getParentRangeId(), parent.getId()))
        .sorted(Comparator.comparingLong(BookRange::getSiblingOrder))
        .toList();
  }

  private static AttachBookAnchorRequest anchor(String value) {
    AttachBookAnchorRequest a = new AttachBookAnchorRequest();
    a.setAnchorFormat(BookReadingWireConstants.ANCHOR_FORMAT_PDF_MINERU_OUTLINE_V1);
    a.setValue(value);
    return a;
  }

  private static AttachBookLayoutNodeRequest node(
      String title, AttachBookLayoutNodeRequest... kids) {
    AttachBookLayoutNodeRequest n = new AttachBookLayoutNodeRequest();
    n.setTitle(title);
    n.setStartAnchor(anchor("{\"p\":1}"));
    n.setEndAnchor(anchor("{\"p\":2}"));
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

  @Nested
  class AttachBook {
    @Test
    void persistsNestedOutlineAndReturnsBookWithRanges() throws Exception {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
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
      assertThat(created.getRanges(), hasSize(3));

      BookRange outRoot = rootRangesSorted(created).getFirst();
      assertThat(outRoot.getStructuralTitle(), equalTo("Chapter 1"));
      assertThat(outRoot.getId(), notNullValue());
      List<BookRange> children = childrenOf(created, outRoot);
      assertThat(children, hasSize(2));
      assertThat(children.getFirst().getStructuralTitle(), equalTo("Section 1.1"));
      assertThat(children.get(1).getStructuralTitle(), equalTo("Section 1.2"));

      Book detail = controller.getBook(nb);
      assertThat(detail.getRanges(), hasSize(3));
      BookRange detailRoot = rootRangesSorted(detail).getFirst();
      assertThat(detailRoot.getId(), equalTo(outRoot.getId()));
      assertThat(childrenOf(detail, detailRoot), hasSize(2));

      ResponseEntity<byte[]> fileRes = controller.getBookFile(nb);
      assertThat(fileRes.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(fileRes.getBody(), equalTo(pdfBytes));
    }

    @Test
    void rejectsSecondAttachForSameNotebook() throws Exception {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      controller.attachBook(nb, attachRequest(node("First")), pdfFile(STUB_PDF_BYTES));
      assertThrows(
          ResponseStatusException.class,
          () -> controller.attachBook(nb, attachRequest(node("Second")), pdfFile(STUB_PDF_BYTES)));
    }

    @Test
    void rejectsUnauthorizedNotebook() {
      User other = makeMe.aUser().please();
      Notebook otherNb = makeMe.aNotebook().creatorAndOwner(other).please();
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.attachBook(otherNb, attachRequest(node("A")), pdfFile(STUB_PDF_BYTES)));
    }

    @Test
    void rejectsNonPdfFormat() {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      AttachBookRequest req = attachRequest(node("A"));
      req.setFormat("epub");
      assertThrows(
          ResponseStatusException.class,
          () -> controller.attachBook(nb, req, pdfFile(STUB_PDF_BYTES)));
    }

    @Test
    void rejectsEmptyRoots() {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      AttachBookRequest req = attachRequest();
      req.getLayout().setRoots(new ArrayList<>());
      assertThrows(
          ResponseStatusException.class,
          () -> controller.attachBook(nb, req, pdfFile(STUB_PDF_BYTES)));
    }

    @Test
    void rejectsBadAnchorFormat() {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      AttachBookLayoutNodeRequest n = node("A");
      n.getStartAnchor().setAnchorFormat("unknown");
      assertThrows(
          ResponseStatusException.class,
          () -> controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES)));
    }

    @Test
    void rejectsExcessiveDepth() {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      AttachBookLayoutNodeRequest deep = node("leaf");
      for (int i = 0; i < BookReadingWireConstants.MAX_LAYOUT_DEPTH; i++) {
        deep = node("d" + i, deep);
      }
      AttachBookLayoutNodeRequest root = deep;
      assertThrows(
          ResponseStatusException.class,
          () -> controller.attachBook(nb, attachRequest(root), pdfFile(STUB_PDF_BYTES)));
    }

    @Test
    void rejectsEmptyFile() throws Exception {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      MultipartFile empty =
          new MockMultipartFile("file", "book.pdf", "application/pdf", new byte[0]);
      assertThrows(
          ResponseStatusException.class,
          () -> controller.attachBook(nb, attachRequest(node("A")), empty));
    }
  }

  @Nested
  class GetBook {
    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      assertThrows(ResponseStatusException.class, () -> controller.getBook(nb));
    }

    @Test
    void hasSourceFileTrueWhenPdfStored() throws Exception {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      controller.attachBook(nb, attachRequest(node("X")), pdfFile(STUB_PDF_BYTES));
      Book book = controller.getBook(nb);
      assertThat(book.getHasSourceFile(), equalTo(true));
    }

    @Test
    void hasSourceFileFalseWhenSourceFileRefCleared() throws Exception {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      controller.attachBook(nb, attachRequest(node("X")), pdfFile(STUB_PDF_BYTES));
      Book book = bookRepository.findByNotebook_Id(nb.getId()).orElseThrow();
      book.setSourceFileRef(null);
      makeMe.entityPersister.save(book);
      makeMe.entityPersister.flush();
      assertThat(controller.getBook(nb).getHasSourceFile(), equalTo(false));
    }

    @Test
    void doesNotReturnAnotherNotebooksBook() throws Exception {
      Notebook nb1 = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Notebook nb2 = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      controller.attachBook(nb1, attachRequest(node("X")), pdfFile(STUB_PDF_BYTES));
      assertThrows(ResponseStatusException.class, () -> controller.getBook(nb2));
    }
  }

  @Nested
  class GetBookFile {
    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(nb));
    }

    @Test
    void returns404WhenBookHasNoSourceFile() throws Exception {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      controller.attachBook(nb, attachRequest(node("X")), pdfFile(STUB_PDF_BYTES));
      Book book = bookRepository.findByNotebook_Id(nb.getId()).orElseThrow();
      book.setSourceFileRef(null);
      makeMe.entityPersister.save(book);
      makeMe.entityPersister.flush();

      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(nb));
    }

    @Test
    void rejectsNotebookWithoutReadAccess() {
      User other = makeMe.aUser().please();
      Notebook otherNb = makeMe.aNotebook().creatorAndOwner(other).please();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.getBookFile(otherNb));
    }

    @Test
    void returnsPdfWhenSourceFileRefPointsAtBlob() throws Exception {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      controller.attachBook(nb, attachRequest(node("X")), pdfFile(STUB_PDF_BYTES));
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};
      String ref = bookStorage.put(pdfBytes);
      Book book = bookRepository.findByNotebook_Id(nb.getId()).orElseThrow();
      book.setSourceFileRef(ref);
      makeMe.entityPersister.save(book);
      makeMe.entityPersister.flush();

      ResponseEntity<byte[]> res = controller.getBookFile(nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(res.getBody(), equalTo(pdfBytes));
      assertThat(res.getHeaders().getContentType(), equalTo(MediaType.APPLICATION_PDF));
      assertThat(
          res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION),
          equalTo("attachment; filename=\"Linear Algebra.pdf\""));
    }

    @Test
    void returns404WhenSourceFileRefIsNotNumeric() throws Exception {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      controller.attachBook(nb, attachRequest(node("X")), pdfFile(STUB_PDF_BYTES));
      Book book = bookRepository.findByNotebook_Id(nb.getId()).orElseThrow();
      book.setSourceFileRef("not-an-id");
      makeMe.entityPersister.save(book);
      makeMe.entityPersister.flush();

      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(nb));
    }

    @Test
    void returns404WhenSourceFileRefBlobMissing() throws Exception {
      Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      controller.attachBook(nb, attachRequest(node("X")), pdfFile(STUB_PDF_BYTES));
      Book book = bookRepository.findByNotebook_Id(nb.getId()).orElseThrow();
      book.setSourceFileRef(String.valueOf(Integer.MAX_VALUE));
      makeMe.entityPersister.save(book);
      makeMe.entityPersister.flush();

      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(nb));
    }
  }
}
