package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.services.notebookGit.SqlStatementCallLog;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.util.HashSet;
import java.util.List;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;

/** A web save's server work depends on what changed, not on the notebook's size. */
class NotebookGitWebContentSaveCostControllerTest extends NotebookGitWebContentControllerTestBase {

  @Test
  void savingContentInALargeNotebookWithAttachmentsDoesNotQueryAttachmentsOrPortableTreeRows()
      throws Throwable {
    Notebook large = createGitBackedNotebook("Large");
    Note note = makeMe.aNote().notebook(large).content(ACCEPTED_CONTENT).please();
    for (int i = 0; i < 30; i++) {
      makeMe.aNote().notebook(large).title("Unrelated " + i).please();
    }
    for (int i = 0; i < 3; i++) {
      storeFolderAttachmentAndSnapshot(large, null, "attachment-" + i + ".bin", new byte[4096]);
    }
    var acceptedBefore = acceptedHistory(large);

    Statistics contentSave =
        hibernateStatisticsOf(
            () -> textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT)));

    var queries = List.of(contentSave.getQueries());
    assertThat(queries, not(hasItem(containsString("NotebookAttachment"))));
    assertThat(queries, not(hasItem(containsString("PortableTreeNoteRow"))));
    AcceptedHistory after = acceptedHistory(large);
    assertThat(after.parents(), equalTo(acceptedBefore.commits()));
  }

  /**
   * Observes JDBC executions for one content save on a nested accepted tree. Counts are JDBC
   * execute* calls, not wire round trips; tree fetches stay within one whole-tree walk for this
   * fixture.
   */
  @Test
  void contentSaveJdbcObjectFetchesAreScopedToTheControllerCall() throws Throwable {
    Notebook notebook = createGitBackedNotebook("Jdbc Cost");
    Folder parent = null;
    for (int i = 0; i < 6; i++) {
      parent =
          parent == null
              ? makeMe.aFolder().notebook(notebook).name("Depth" + i).please()
              : makeMe.aFolder().parentFolder(parent).name("Depth" + i).please();
    }
    Note note = makeMe.aNote().folder(parent).content(ACCEPTED_CONTENT).please();
    for (int i = 0; i < 4; i++) {
      makeMe.aFolder().notebook(notebook).name("Sibling" + i).please();
    }
    snapshotCurrentPortableTree(notebook);
    AcceptedHistory acceptedBefore = acceptedHistory(notebook);

    SqlStatementCallLog callLog = new SqlStatementCallLog();
    long started = System.nanoTime();
    try (AutoCloseable ignored = callLog.activate()) {
      textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT));
    }
    long elapsedMs = (System.nanoTime() - started) / 1_000_000L;

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(acceptedBefore.commits()));
    long objectFetches = callLog.countObjectFetches();
    long treeFetches = callLog.countObjectFetchesReturningType(Constants.OBJ_TREE);
    long commitFetches = callLog.countObjectFetchesReturningType(Constants.OBJ_COMMIT);
    long objectInserts =
        callLog.countExecutionsMatching("INSERT INTO notebook_git_accepted_object");
    long bindingUpdates =
        callLog.countExecutionsMatching("UPDATE notebook_git_binding", "accepted_git_object_id");
    System.out.printf(
        "content-save JDBC observation revision-local fixture=depth-6-plus-4-sibling-folders"
            + " elapsedMs=%d objectFetches=%d treeFetches=%d commitFetches=%d"
            + " objectInsertExecutions=%d bindingUpdateExecutions=%d jdbcExecutions=%d%n",
        elapsedMs,
        objectFetches,
        treeFetches,
        commitFetches,
        objectInserts,
        bindingUpdates,
        callLog.executions().size());
    assertThat(objectFetches, greaterThan(0L));
    assertThat(treeFetches, greaterThan(0L));
    assertThat(objectInserts, greaterThan(0L));
    assertThat(treeFetches, lessThanOrEqualTo(12L));
  }

  @Test
  void contentSaveTreeFetchesStayBoundedWhenUnrelatedFoldersGrowFromDozensToThousands()
      throws Exception {
    SaveCostObservation shallow = measureContentSaveAtDepth(12, 40, "cost-depth12-unrelated-40");
    SaveCostObservation deep = measureContentSaveAtDepth(12, 3000, "cost-depth12-unrelated-3000");

    assertThat(shallow.treeFetches(), equalTo(deep.treeFetches()));
    assertThat(shallow.treeFetches(), lessThanOrEqualTo(13L));
    assertThat(deep.existenceCheckExecutions(), equalTo(1L));
    assertThat(deep.objectInsertExecutions(), equalTo(1L));
    assertThat(deep.objectInsertRows(), lessThanOrEqualTo(15));
    assertThat(deep.fetchedTreeIds(), hasSize((int) deep.treeFetches()));
    assertThat(
        "editor opens each ancestor tree once",
        deep.fetchedTreeIds().size(),
        equalTo(new HashSet<>(deep.fetchedTreeIds()).size()));
    assertAcceptedTreeMatchesTheFullAssembly(deep.notebook());
  }

  @Test
  void unchangedContentSaveDoesNotInsertGitObjectsOrAdvanceTheRef() throws Exception {
    Notebook notebook = createGitBackedNotebook("Unchanged Save");
    Note note = makeMe.aNote().notebook(notebook).content(EDITED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);
    AcceptedHistory before = acceptedHistory(notebook);
    Integer noteId = note.getId();

    Note reloaded =
        inCommittedTransaction(
            transactionManager,
            () -> {
              entityManager.clear();
              return entityManager.find(Note.class, noteId);
            });

    SqlStatementCallLog callLog = new SqlStatementCallLog();
    try (AutoCloseable ignored = callLog.activate()) {
      textContentController.updateNoteContent(reloaded, contentDto(EDITED_CONTENT));
    }

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.commits(), equalTo(before.commits()));
    assertThat(
        callLog.countExecutionsMatching("INSERT INTO notebook_git_accepted_object"), equalTo(0L));
    assertThat(
        callLog.countExecutionsMatching("UPDATE notebook_git_binding", "accepted_git_object_id"),
        equalTo(0L));
  }

  private SaveCostObservation measureContentSaveAtDepth(
      int depth, int unrelatedFolders, String label) throws Exception {
    Notebook notebook = createGitBackedNotebook(label);
    Folder parent = null;
    for (int i = 0; i < depth; i++) {
      parent =
          parent == null
              ? makeMe.aFolder().notebook(notebook).name("D" + i).please()
              : makeMe.aFolder().parentFolder(parent).name("D" + i).please();
    }
    Note note = makeMe.aNote().folder(parent).content(ACCEPTED_CONTENT).please();
    for (int i = 0; i < unrelatedFolders; i++) {
      makeMe.aFolder().notebook(notebook).name("U" + i).please();
    }
    storeFolderAttachmentAndSnapshot(
        notebook, null, "noise.bin", new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
    AcceptedHistory acceptedBefore = acceptedHistory(notebook);
    Integer noteId = note.getId();

    Note reloaded =
        inCommittedTransaction(
            transactionManager,
            () -> {
              entityManager.clear();
              return entityManager.find(Note.class, noteId);
            });

    SqlStatementCallLog callLog = new SqlStatementCallLog();
    try (AutoCloseable ignored = callLog.activate()) {
      textContentController.updateNoteContent(reloaded, contentDto(EDITED_CONTENT));
    }

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(acceptedBefore.commits()));
    List<ObjectId> fetchedTrees = fetchedObjectIdsReturningType(callLog, Constants.OBJ_TREE);
    int insertRows =
        callLog.executions().stream()
            .filter(e -> e.sql().contains("INSERT INTO notebook_git_accepted_object"))
            .mapToInt(SqlStatementCallLog.Execution::result)
            .sum();
    return new SaveCostObservation(
        notebook,
        callLog.countObjectFetchesReturningType(Constants.OBJ_TREE),
        callLog.countExecutionsMatching("notebook_git_accepted_object", " IN ("),
        callLog.countExecutionsMatching("INSERT INTO notebook_git_accepted_object"),
        insertRows,
        fetchedTrees);
  }

  private static List<ObjectId> fetchedObjectIdsReturningType(
      SqlStatementCallLog callLog, int objectType) {
    return callLog.executions().stream()
        .filter(e -> "executeQuery".equals(e.method()))
        .filter(e -> e.sql().contains("notebook_git_accepted_object"))
        .filter(e -> e.sql().contains("git_object_id = ?"))
        .filter(e -> e.objectTypes().contains(objectType))
        .map(e -> ObjectId.fromString(e.parameters().get(1).toString()))
        .toList();
  }

  private record SaveCostObservation(
      Notebook notebook,
      long treeFetches,
      long existenceCheckExecutions,
      long objectInsertExecutions,
      int objectInsertRows,
      List<ObjectId> fetchedTreeIds) {}
}
