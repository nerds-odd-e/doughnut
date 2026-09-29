package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.BookBlockReadingRecordPutRequest;
import com.odde.donut.entities.BookBlock;
import com.odde.donut.entities.BookBlockReadingRecord;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class NotebookBooksReadingRecordControllerTest extends NotebookBooksControllerTestBase {

  @Nested
  class GetBookReadingRecords {
    @Test
    void returnsRecordForMarkedRange() throws Exception {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
      Notebook nb = notebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(nb)).getFirst();
      readingController.putBlockReadingRecord(nb, range, null);

      var list = readingController.getBookReadingRecords(nb);
      assertThat(list, hasSize(1));
      assertThat(list.getFirst().getBookBlockId(), equalTo(range.getId()));
      assertThat(list.getFirst().getStatus(), equalTo(BookBlockReadingRecord.STATUS_READ));
      assertThat(
          list.getFirst().getCompletedAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void returnsOnlyMarkedRangesAmongSiblings() throws Exception {
      Notebook nb = myNotebook();
      controller.attachBook(nb, attachRequest(node("2.1"), node("2.2")), pdfFile(ONE_PAGE_PDF));
      BookBlock first = rootBlocksSorted(bookOf(nb)).getFirst();
      readingController.putBlockReadingRecord(nb, first, null);

      var list = readingController.getBookReadingRecords(nb);
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

      assertThat(readingController.getBookReadingRecords(nb), empty());
    }

    @Test
    void returnsEmptyWhenNoRecords() throws Exception {
      Notebook nb = notebookWithBook();
      assertThat(readingController.getBookReadingRecords(nb), empty());
    }
  }

  @Nested
  class PutBlockReadingRecord {
    @Test
    void persistsReadRecordForCurrentUserAndRange() throws UnexpectedNoAccessRightException {
      testabilitySettings.timeTravelTo(makeMe.aTimestamp().please());
      Notebook nb = notebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(nb)).getFirst();

      var returned = readingController.putBlockReadingRecord(nb, range, null);
      assertThat(returned, hasSize(1));
      assertThat(returned.getFirst().getBookBlockId(), equalTo(range.getId()));
      assertThat(returned.getFirst().getStatus(), equalTo(BookBlockReadingRecord.STATUS_READ));
      assertThat(
          returned.getFirst().getCompletedAt(),
          equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void returns404WhenNotebookHasNoBook() {
      Notebook nbEmpty = myNotebook();
      Notebook nbWith = notebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(nbWith)).getFirst();

      assertThrows(
          ResponseStatusException.class,
          () -> readingController.putBlockReadingRecord(nbEmpty, range, null));
    }

    @Test
    void returns404WhenRangeBelongsToAnotherNotebooksBook() {
      Notebook otherNb = otherUsersNotebookWithBook();
      BookBlock otherRange = rootBlocksSorted(bookOf(otherNb)).getFirst();
      Notebook myNb = notebookWithBook();

      assertThrows(
          ResponseStatusException.class,
          () -> readingController.putBlockReadingRecord(myNb, otherRange, null));
    }

    @Test
    void rejectsNotebookWithoutReadAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(otherNb)).getFirst();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> readingController.putBlockReadingRecord(otherNb, range, null));
    }

    @Test
    void requiresLoggedInUser() {
      Notebook nb = notebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(nb)).getFirst();
      currentUser.setUser(null);

      assertThrows(
          ResponseStatusException.class,
          () -> readingController.putBlockReadingRecord(nb, range, null));
    }

    @Test
    void persistsSkimmedAndSkippedStatuses() throws Exception {
      Notebook nb = myNotebook();
      controller.attachBook(
          nb, attachRequest(node("Block A"), node("Block B")), pdfFile(ONE_PAGE_PDF));
      List<BookBlock> roots = rootBlocksSorted(bookOf(nb));
      BookBlock first = roots.getFirst();
      BookBlock second = roots.get(1);

      var skimBody = new BookBlockReadingRecordPutRequest();
      skimBody.setStatus(BookBlockReadingRecord.STATUS_SKIMMED);
      var afterSkim = readingController.putBlockReadingRecord(nb, first, skimBody);
      assertThat(afterSkim.getFirst().getStatus(), equalTo(BookBlockReadingRecord.STATUS_SKIMMED));

      var skipBody = new BookBlockReadingRecordPutRequest();
      skipBody.setStatus(BookBlockReadingRecord.STATUS_SKIPPED);
      var afterSkip = readingController.putBlockReadingRecord(nb, second, skipBody);
      assertThat(afterSkip, hasSize(2));
      assertThat(
          afterSkip.stream()
              .filter(i -> i.getBookBlockId().equals(second.getId()))
              .findFirst()
              .orElseThrow()
              .getStatus(),
          equalTo(BookBlockReadingRecord.STATUS_SKIPPED));
    }

    @Test
    void rejectsInvalidStatus() throws Exception {
      Notebook nb = notebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(nb)).getFirst();
      var bad = new BookBlockReadingRecordPutRequest();
      bad.setStatus("NOT_A_STATUS");

      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> readingController.putBlockReadingRecord(nb, range, bad));
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
      readingController.putBlockReadingRecord(nb, range, skim);

      testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(1, 11).please());
      var second = readingController.putBlockReadingRecord(nb, range, null);

      assertThat(bookBlockReadingRecordRepository.count(), equalTo(1L));
      assertThat(second.getFirst().getStatus(), equalTo(BookBlockReadingRecord.STATUS_READ));
      assertThat(
          second.getFirst().getCompletedAt(),
          equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }
  }

  @Nested
  class DeleteBlockReadingRecord {
    @Test
    void removesOnlyTheCurrentUsersRecordForThatBlock() throws Exception {
      Notebook nb = myNotebook();
      controller.attachBook(
          nb, attachRequest(node("Block A"), node("Block B")), pdfFile(ONE_PAGE_PDF));
      List<BookBlock> roots = rootBlocksSorted(bookOf(nb));
      BookBlock first = roots.getFirst();
      BookBlock second = roots.get(1);
      readingController.putBlockReadingRecord(nb, first, null);
      readingController.putBlockReadingRecord(nb, second, null);
      var otherRow = new BookBlockReadingRecord();
      otherRow.setUser(makeMe.aUser().please());
      otherRow.setBookBlock(first);
      otherRow.setStatus(BookBlockReadingRecord.STATUS_READ);
      otherRow.setCompletedAt(testabilitySettings.getCurrentUTCTimestamp());
      makeMe.entityPersister.save(otherRow);
      makeMe.entityPersister.flush();

      var remaining = readingController.deleteBlockReadingRecord(nb, first);

      assertThat(remaining, hasSize(1));
      assertThat(remaining.getFirst().getBookBlockId(), equalTo(second.getId()));
      assertThat(bookBlockReadingRecordRepository.count(), equalTo(2L));
    }

    @Test
    void returns404WhenBlockBelongsToAnotherNotebooksBook() {
      Notebook otherNb = otherUsersNotebookWithBook();
      BookBlock otherRange = rootBlocksSorted(bookOf(otherNb)).getFirst();
      Notebook myNb = notebookWithBook();

      var ex =
          assertThrows(
              ResponseStatusException.class,
              () -> readingController.deleteBlockReadingRecord(myNb, otherRange));
      assertThat(ex.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void rejectsNotebookWithoutReadAccess() {
      Notebook otherNb = otherUsersNotebookWithBook();
      BookBlock range = rootBlocksSorted(bookOf(otherNb)).getFirst();

      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> readingController.deleteBlockReadingRecord(otherNb, range));
    }
  }
}
