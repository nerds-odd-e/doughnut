package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.BookBlockReadingRecordPutRequest;
import com.odde.doughnut.entities.BookBlock;
import com.odde.doughnut.entities.BookBlockReadingRecord;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
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
      List<BookBlock> roots = rootBlocksSorted(bookOf(nb));
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
