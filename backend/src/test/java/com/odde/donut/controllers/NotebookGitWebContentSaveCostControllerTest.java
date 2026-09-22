package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.services.notebookGit.SqlStatementCallLog;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.util.List;
import org.eclipse.jgit.lib.Constants;
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
}
