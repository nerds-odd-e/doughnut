package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.services.notebookGit.SqlStatementCallLog;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Local-only representative notebook measurement. Not part of ordinary {@code backend:test_only}:
 * enable with {@code DONUT_MEASURE_REPRESENTATIVE_SAVE_COST=true}. Does not assert elapsed time.
 */
class NotebookGitWebContentSaveCostRepresentativeExperimentTest
    extends NotebookGitWebContentSaveCostTestSupport {

  private static final int REPRESENTATIVE_DEPTH = 12;
  private static final int REPRESENTATIVE_FOLDERS = 4_000;
  private static final int REPRESENTATIVE_NOTES = 11_000;
  private static final int REPRESENTATIVE_SAVE_SAMPLES = 20;

  @ParameterizedTest
  @ValueSource(ints = {0, REPRESENTATIVE_DEPTH})
  void representativeNotebookShapeColdSaveCostExperiment(int depth) throws Exception {
    assumeTrue(
        "true".equals(System.getenv("DONUT_MEASURE_REPRESENTATIVE_SAVE_COST")),
        "set DONUT_MEASURE_REPRESENTATIVE_SAVE_COST=true to run the local representative experiment");

    int depthFolders = depth;
    int unrelatedFolders = REPRESENTATIVE_FOLDERS - depthFolders;
    int unrelatedNotes = REPRESENTATIVE_NOTES - 1;
    Notebook notebook = createGitBackedNotebook("representative-4000f-11000n-depth" + depth);
    Folder parent = buildDepthPath(notebook, depthFolders);
    Note note =
        (parent == null ? makeMe.aNote().notebook(notebook) : makeMe.aNote().folder(parent))
            .content(ACCEPTED_CONTENT)
            .please();
    seedUnrelatedFoldersAndNotes(
        notebook,
        parent,
        unrelatedFolders,
        unrelatedNotes,
        i -> "---\ntype: Note\n---\nnoise " + i);
    storeFolderAttachmentAndSnapshot(
        notebook, null, "noise.bin", new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    Integer noteId = note.getId();
    Integer notebookId = notebook.getId();

    assertThat(TransactionSynchronizationManager.isActualTransactionActive(), equalTo(false));
    try (var connection = dataSource.getConnection()) {
      System.out.printf(
          "representative-environment depth=%d engine=%s version=%s isolation=%d autoCommit=%s url=%s%n",
          depth,
          connection.getMetaData().getDatabaseProductName(),
          connection.getMetaData().getDatabaseProductVersion(),
          connection.getTransactionIsolation(),
          connection.getAutoCommit(),
          connection.getMetaData().getURL());
    }
    var before = acceptedHistory(notebook);
    long[] samplesUs = new long[REPRESENTATIVE_SAVE_SAMPLES];
    SaveCostObservation last = null;
    for (int sample = 0; sample < REPRESENTATIVE_SAVE_SAMPLES; sample++) {
      Note reloaded =
          inCommittedTransaction(
              transactionManager,
              () -> {
                entityManager.clear();
                return entityManager.find(Note.class, noteId);
              });
      String edited =
          "---\ntype: Note\n---\nedited content sample " + sample + " " + System.nanoTime();
      SqlStatementCallLog callLog = new SqlStatementCallLog();
      long started = System.nanoTime();
      try (AutoCloseable ignored = callLog.activate()) {
        var returned = textContentController.updateNoteContent(reloaded, contentDto(edited));
        samplesUs[sample] = (System.nanoTime() - started) / 1_000L;
        assertThat(returned.getNote().getContent(), equalTo(edited));
      }
      assertThat(TransactionSynchronizationManager.isActualTransactionActive(), equalTo(false));
      assertThat(noteRepository.findById(noteId).orElseThrow().getContent(), equalTo(edited));
      var after = acceptedHistory(notebook);
      assertThat(after.parents(), equalTo(before.commits()));
      before = after;
      if (sample == 0) {
        callLog
            .executions()
            .forEach(
                execution ->
                    System.out.printf(
                        "representative-sql depth=%d method=%s sql=%s%n",
                        depth, execution.method(), execution.sql()));
      }
      last = observationFrom(callLog, entityManager.find(Notebook.class, notebookId));
      System.out.printf(
          "representative-save depth=%d sample=%d elapsedUs=%d treeFetches=%d objectFetches=%d"
              + " commitFetches=%d jdbcExecutions=%d existenceChecks=%d objectInsertExecutions=%d"
              + " objectInsertRows=%d attemptedObjectIds=%d fetchedObjectBytes=%d"
              + " bindingUpdates=%d%n",
          depth,
          sample,
          samplesUs[sample],
          last.treeFetches(),
          last.objectFetches(),
          last.commitFetches(),
          last.jdbcExecutions(),
          last.existenceCheckExecutions(),
          last.objectInsertExecutions(),
          last.objectInsertRows(),
          last.attemptedObjectIds(),
          last.fetchedObjectBytes(),
          last.bindingUpdateExecutions());
    }

    Arrays.sort(samplesUs);
    System.out.printf(
        "representative-save distribution revision=%s fixture=depth-%d-folders-%d-notes-%d"
            + " samples=%d elapsedUs min=%d median=%.1f max=%d"
            + " final treeFetches=%d objectFetches=%d objectInsertRows=%d attemptedObjectIds=%d"
            + " fetchedObjectBytes=%d%n",
        System.getenv().getOrDefault("DONUT_MEASURE_REVISION", "local"),
        depth,
        REPRESENTATIVE_FOLDERS,
        REPRESENTATIVE_NOTES,
        REPRESENTATIVE_SAVE_SAMPLES,
        samplesUs[0],
        (samplesUs[(REPRESENTATIVE_SAVE_SAMPLES - 1) / 2]
                + samplesUs[REPRESENTATIVE_SAVE_SAMPLES / 2])
            / 2.0,
        samplesUs[REPRESENTATIVE_SAVE_SAMPLES - 1],
        last.treeFetches(),
        last.objectFetches(),
        last.objectInsertRows(),
        last.attemptedObjectIds(),
        last.fetchedObjectBytes());

    assertThat(last.treeFetches(), lessThanOrEqualTo(13L));
    assertThat(last.existenceCheckExecutions(), equalTo(1L));
    assertThat(last.objectInsertExecutions(), equalTo(1L));
    assertThat(last.objectInsertRows(), lessThanOrEqualTo(15));
    assertThat(last.fetchedTreeIds().size(), equalTo(new HashSet<>(last.fetchedTreeIds()).size()));
    assertAcceptedTreeMatchesTheFullAssembly(last.notebook());
  }
}
