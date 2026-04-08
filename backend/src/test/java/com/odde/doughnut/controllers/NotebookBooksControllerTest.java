package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Book;
import com.odde.doughnut.entities.BookRange;
import com.odde.doughnut.entities.BookRangeReadingRecord;
import com.odde.doughnut.entities.BookUserLastReadPosition;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.BookRangeReadingRecordRepository;
import com.odde.doughnut.entities.repositories.BookRepository;
import com.odde.doughnut.entities.repositories.BookUserLastReadPositionRepository;
import com.odde.doughnut.exceptions.ApiException;
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
  @Autowired BookUserLastReadPositionRepository bookUserLastReadPositionRepository;
  @Autowired BookRangeReadingRecordRepository bookRangeReadingRecordRepository;
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
    void persistsNestedOutlineAndReturnsBookWithRanges() throws Exception {
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
    void rejectsBadAnchorFormat() {
      Notebook nb = myNotebook();
      AttachBookLayoutNodeRequest n = node("A");
      n.getStartAnchor().setAnchorFormat("unknown");
      assertThrows(
          ApiException.class,
          () -> controller.attachBook(nb, attachRequest(n), pdfFile(STUB_PDF_BYTES)));
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
  }

  @Nested
  class GetBook {
    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = myNotebook();
      assertThrows(ResponseStatusException.class, () -> controller.getBook(nb));
    }

    @Test
    void hasSourceFileTrueWhenPdfStored() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      assertThat(controller.getBook(nb).getHasSourceFile(), equalTo(true));
    }

    @Test
    void hasSourceFileFalseWhenSourceFileRefCleared() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      setSourceFileRef(nb, null);
      assertThat(controller.getBook(nb).getHasSourceFile(), equalTo(false));
    }

    @Test
    void doesNotReturnAnotherNotebooksBook() {
      notebookWithBook();
      Notebook nb2 = myNotebook();
      assertThrows(ResponseStatusException.class, () -> controller.getBook(nb2));
    }
  }

  @Nested
  class GetBookFile {
    @Test
    void returns404WhenNotebookHasNoBook() throws UnexpectedNoAccessRightException {
      Notebook nb = myNotebook();
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(nb));
    }

    @Test
    void returns404WhenBookHasNoSourceFile() {
      Notebook nb = notebookWithBook();
      setSourceFileRef(nb, null);
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(nb));
    }

    @Test
    void rejectsNotebookWithoutReadAccess() {
      Notebook otherNb = otherUsersNotebook();
      assertThrows(UnexpectedNoAccessRightException.class, () -> controller.getBookFile(otherNb));
    }

    @Test
    void returnsPdfWhenSourceFileRefPointsAtBlob() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};
      setSourceFileRef(nb, bookStorage.put(pdfBytes));

      ResponseEntity<byte[]> res = controller.getBookFile(nb);

      assertThat(res.getStatusCode(), equalTo(HttpStatus.OK));
      assertThat(res.getBody(), equalTo(pdfBytes));
      assertThat(res.getHeaders().getContentType(), equalTo(MediaType.APPLICATION_PDF));
      assertThat(
          res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION),
          equalTo("attachment; filename=\"Linear Algebra.pdf\""));
    }

    @Test
    void returns404WhenSourceFileRefIsNotNumeric() {
      Notebook nb = notebookWithBook();
      setSourceFileRef(nb, "not-an-id");
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(nb));
    }

    @Test
    void returns404WhenSourceFileRefBlobMissing() {
      Notebook nb = notebookWithBook();
      setSourceFileRef(nb, String.valueOf(Integer.MAX_VALUE));
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(nb));
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
      assertThrows(ResponseStatusException.class, () -> controller.getBookFile(nb));
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

    @Test
    void deletesBookWhenSourceFileRefMissing() throws UnexpectedNoAccessRightException {
      Notebook nb = notebookWithBook();
      setSourceFileRef(nb, null);

      controller.deleteBook(nb);

      assertThat(bookRepository.findByNotebook_Id(nb.getId()).isEmpty(), equalTo(true));
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
      BookRange range = rootRangesSorted(bookOf(nb)).getFirst();
      controller.putRangeReadingRecord(nb, range);

      var list = controller.getBookReadingRecords(nb);
      assertThat(list, hasSize(1));
      assertThat(list.getFirst().getBookRangeId(), equalTo(range.getId()));
      assertThat(list.getFirst().getStatus(), equalTo(BookRangeReadingRecord.STATUS_READ));
      assertThat(
          list.getFirst().getCompletedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void returnsOnlyMarkedRangesAmongSiblings() throws Exception {
      Notebook nb = myNotebook();
      byte[] pdfBytes = new byte[] {0x25, 0x50, 0x44, 0x46};
      controller.attachBook(nb, attachRequest(node("2.1"), node("2.2")), pdfFile(pdfBytes));
      Book book = bookOf(nb);
      List<BookRange> roots = rootRangesSorted(book);
      BookRange first = roots.getFirst();
      controller.putRangeReadingRecord(nb, first);

      var list = controller.getBookReadingRecords(nb);
      assertThat(list, hasSize(1));
      assertThat(list.getFirst().getBookRangeId(), equalTo(first.getId()));
    }

    @Test
    void doesNotIncludeAnotherUsersRecords() throws Exception {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
      Notebook nb = notebookWithBook();
      BookRange range = rootRangesSorted(bookOf(nb)).getFirst();
      User other = makeMe.aUser().please();
      var otherRow = new BookRangeReadingRecord();
      otherRow.setUser(other);
      otherRow.setBookRange(range);
      otherRow.setStatus(BookRangeReadingRecord.STATUS_READ);
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
  class PutRangeReadingRecord {
    @Test
    void persistsReadRecordForCurrentUserAndRange() throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
      Notebook nb = notebookWithBook();
      BookRange range = rootRangesSorted(bookOf(nb)).getFirst();

      var returned = controller.putRangeReadingRecord(nb, range);
      assertThat(returned, hasSize(1));
      assertThat(returned.getFirst().getBookRangeId(), equalTo(range.getId()));
      assertThat(returned.getFirst().getStatus(), equalTo(BookRangeReadingRecord.STATUS_READ));
      assertThat(
          returned.getFirst().getCompletedAt(),
          equalTo(testabilitySettings.getCurrentUTCTimestamp()));

      var stored =
          bookRangeReadingRecordRepository
              .findByUser_IdAndBookRange_Id(currentUser.getUser().getId(), range.getId())
              .orElseThrow();
      assertThat(stored.getStatus(), equalTo(BookRangeReadingRecord.STATUS_READ));
      assertThat(stored.getCompletedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void secondPutUpdatesCompletedAtAndKeepsSingleRow() throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(0, 10).please());
      Notebook nb = notebookWithBook();
      BookRange range = rootRangesSorted(bookOf(nb)).getFirst();

      var firstResponse = controller.putRangeReadingRecord(nb, range);
      assertThat(firstResponse, hasSize(1));
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(1, 11).please());
      var secondResponse = controller.putRangeReadingRecord(nb, range);
      assertThat(secondResponse, hasSize(1));
      assertThat(
          secondResponse.getFirst().getCompletedAt(),
          equalTo(testabilitySettings.getCurrentUTCTimestamp()));

      assertThat(bookRangeReadingRecordRepository.count(), equalTo(1L));
      var stored =
          bookRangeReadingRecordRepository
              .findByUser_IdAndBookRange_Id(currentUser.getUser().getId(), range.getId())
              .orElseThrow();
      assertThat(stored.getCompletedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void returns404WhenNotebookHasNoBook() {
      Notebook nbEmpty = myNotebook();
      Notebook nbWith = notebookWithBook();
      BookRange range = rootRangesSorted(bookOf(nbWith)).getFirst();

      assertThrows(
          ResponseStatusException.class, () -> controller.putRangeReadingRecord(nbEmpty, range));
    }

    @Test
    void returns404WhenRangeBelongsToAnotherNotebooksBook() {
      Notebook otherNb = otherUsersNotebookWithBook();
      BookRange otherRange = rootRangesSorted(bookOf(otherNb)).getFirst();
      Notebook myNb = notebookWithBook();

      assertThrows(
          ResponseStatusException.class, () -> controller.putRangeReadingRecord(myNb, otherRange));
    }

    @Test
    void rejectsNotebookWithoutReadAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();
      BookRange range = rootRangesSorted(bookOf(otherNb)).getFirst();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.putRangeReadingRecord(otherNb, range));
    }

    @Test
    void requiresLoggedInUser() {
      Notebook nb = notebookWithBook();
      BookRange range = rootRangesSorted(bookOf(nb)).getFirst();
      currentUser.setUser(null);

      assertThrows(
          ResponseStatusException.class, () -> controller.putRangeReadingRecord(nb, range));
    }
  }
}
