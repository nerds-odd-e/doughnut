package com.odde.donut.services;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport;
import com.odde.donut.testability.CommittedUserCleanup;
import com.odde.donut.testability.MakeMe;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises {@link AuthoredNoteReferenceBackfillTx} through its own committed-transaction boundary
 * (each batch is {@code REQUIRES_NEW}, like production), not the test's rollback transaction — see
 * {@code CommittedTransactionTestSupport}. Most fixtures are notes with pre-existing content but no
 * {@code authored_note_reference} rows yet (built via {@code content(...)}, which sets raw content
 * without going through {@code Note.replaceContent}), mirroring notes that existed before this
 * feature shipped.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthoredNoteReferenceBackfillTxTest {

  private static final String USER_PREFIX = "authored-ref-backfill-";

  @Autowired MakeMe makeMe;
  @Autowired EntityManager entityManager;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired AuthoredNoteReferenceBackfillTx backfillTx;

  @BeforeEach
  void resetProgressToCurrentWatermark() {
    inCommittedTransaction(transactionManager, this::resetProgressRow);
  }

  @AfterEach
  void cleanup() {
    inCommittedTransaction(transactionManager, this::resetProgressRow);
    inCommittedTransaction(
        transactionManager,
        () ->
            CommittedUserCleanup.deleteByUserExternalIdentifierLike(
                entityManager, USER_PREFIX + "%"));
  }

  @Test
  void backfillsWikiAndNoteIdUrlReferencesIncludingAZeroReferenceNote() {
    Note[] holder = new Note[2];
    inCommittedTransaction(
        transactionManager,
        () -> {
          User owner = makeMe.aUser(USER_PREFIX + UUID.randomUUID()).please();
          holder[0] =
              makeMe
                  .aNote()
                  .notebookOwnedBy(owner)
                  .content("[[Target]] [[Missing Note]] [Some Note](/n42)")
                  .please();
          holder[1] =
              makeMe
                  .aNote()
                  .underSameNotebookAs(holder[0])
                  .content("plain body, no links")
                  .please();
        });

    runBackfillToCompletion();

    List<AuthoredNoteReferenceRow> rows =
        inCommittedTransaction(
            transactionManager,
            () -> AuthoredNoteReferenceRowTestSupport.rowsFor(entityManager, holder[0]));
    assertThat(rows, hasSize(3));
    assertThat(rows.get(0).getWikiNotePortion(), equalTo("Target"));
    assertThat(rows.get(1).getWikiNotePortion(), equalTo("Missing Note"));
    assertThat(rows.get(2).getNoteIdUrlNoteId(), equalTo(42));

    List<AuthoredNoteReferenceRow> zeroReferenceRows =
        inCommittedTransaction(
            transactionManager,
            () -> AuthoredNoteReferenceRowTestSupport.rowsFor(entityManager, holder[1]));
    assertThat(zeroReferenceRows, hasSize(0));
  }

  @Test
  void backfillReplacesReferencesAlreadyIndexedByALiveSave() {
    Note[] holder = new Note[1];
    inCommittedTransaction(
        transactionManager,
        () -> {
          User owner = makeMe.aUser(USER_PREFIX + UUID.randomUUID()).please();
          holder[0] = makeMe.aNote().notebookOwnedBy(owner).please();
          makeMe.authorReferencingContent(holder[0], "[[Target]] [Some Note](/n42)");
        });

    runBackfillToCompletion();

    List<AuthoredNoteReferenceRow> rows =
        inCommittedTransaction(
            transactionManager,
            () -> AuthoredNoteReferenceRowTestSupport.rowsFor(entityManager, holder[0]));
    assertThat(rows, hasSize(2));
  }

  @Test
  void restartAfterCompletionIsANoOp() {
    inCommittedTransaction(
        transactionManager,
        () -> {
          User owner = makeMe.aUser(USER_PREFIX + UUID.randomUUID()).please();
          makeMe.aNote().notebookOwnedBy(owner).content("[[Some Target]]").please();
        });

    runBackfillToCompletion();
    boolean completeAfterFirstRun =
        inCommittedTransaction(transactionManager, backfillTx::isComplete);
    assertThat(completeAfterFirstRun, is(true));

    boolean hasMoreOnRestart =
        inCommittedTransaction(transactionManager, () -> backfillTx.processNextBatch(200));
    assertThat(hasMoreOnRestart, is(false));
  }

  @Test
  void resumesFromTheLastCommittedBatchAfterASimulatedRestart() {
    Note[] holder = new Note[2];
    inCommittedTransaction(
        transactionManager,
        () -> {
          User owner = makeMe.aUser(USER_PREFIX + UUID.randomUUID()).please();
          holder[0] = makeMe.aNote().notebookOwnedBy(owner).content("[[First]]").please();
          holder[1] = makeMe.aNote().underSameNotebookAs(holder[0]).content("[[Second]]").please();
        });

    boolean hasMoreAfterFirstBatch =
        inCommittedTransaction(transactionManager, () -> backfillTx.processNextBatch(1));
    assertThat(hasMoreAfterFirstBatch, is(true));
    List<AuthoredNoteReferenceRow> secondNoteRowsBeforeResume =
        inCommittedTransaction(
            transactionManager,
            () -> AuthoredNoteReferenceRowTestSupport.rowsFor(entityManager, holder[1]));
    assertThat(secondNoteRowsBeforeResume, hasSize(0));

    boolean hasMoreAfterSecondBatch =
        inCommittedTransaction(transactionManager, () -> backfillTx.processNextBatch(1));
    assertThat(hasMoreAfterSecondBatch, is(true));
    List<AuthoredNoteReferenceRow> secondNoteRowsAfterResume =
        inCommittedTransaction(
            transactionManager,
            () -> AuthoredNoteReferenceRowTestSupport.rowsFor(entityManager, holder[1]));
    assertThat(secondNoteRowsAfterResume, hasSize(1));

    boolean hasMoreAfterFinalEmptyBatch =
        inCommittedTransaction(transactionManager, () -> backfillTx.processNextBatch(1));
    assertThat(hasMoreAfterFinalEmptyBatch, is(false));
    assertThat(inCommittedTransaction(transactionManager, backfillTx::isComplete), is(true));
  }

  private void runBackfillToCompletion() {
    boolean hasMore = true;
    while (hasMore) {
      hasMore = inCommittedTransaction(transactionManager, () -> backfillTx.processNextBatch(200));
    }
  }

  /**
   * Sets the watermark to the current max note id (not NULL) so each test only ever backfills the
   * notes it creates itself, regardless of whatever else already exists in this shared test
   * database.
   */
  private void resetProgressRow() {
    entityManager
        .createNativeQuery(
            "UPDATE authored_note_reference_backfill_progress "
                + "SET last_processed_note_id = (SELECT COALESCE(MAX(id), 0) FROM note), "
                + "completed_at = NULL "
                + "WHERE id = 1")
        .executeUpdate();
  }
}
