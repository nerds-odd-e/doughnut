package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.services.notebookGit.SqlStatementCallLog;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;

/**
 * Shared fixture and JDBC observation helpers for web content-save cost measurements: build a
 * depth/width/unrelated notebook shape, run one controller save under {@link SqlStatementCallLog},
 * and summarize Git read/write counts without asserting elapsed time.
 */
abstract class NotebookGitWebContentSaveCostTestSupport
    extends NotebookGitWebContentControllerTestBase {

  SaveCostObservation measureContentSave(
      int depth, int unrelatedFolders, int ancestorWidth, int unrelatedNotes, String label)
      throws Exception {
    Notebook notebook = createProductLfsNotebook(label);
    Folder parent = buildDepthPath(notebook, depth);
    widenAncestors(notebook, parent, ancestorWidth);
    Note note =
        (parent == null ? makeMe.aNote().notebook(notebook) : makeMe.aNote().folder(parent))
            .content(ACCEPTED_CONTENT)
            .please();
    seedUnrelatedFoldersAndNotes(notebook, parent, unrelatedFolders, unrelatedNotes, null);
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
    long started = System.nanoTime();
    try (AutoCloseable ignored = callLog.activate()) {
      textContentController.updateNoteContent(reloaded, contentDto(EDITED_CONTENT));
    }
    long elapsedMs = (System.nanoTime() - started) / 1_000_000L;

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(acceptedBefore.commits()));
    SaveCostObservation observation = observationFrom(callLog, notebook);
    System.out.printf(
        "content-save cost label=%s elapsedMs=%d treeFetches=%d objectFetches=%d"
            + " jdbcExecutions=%d objectInsertRows=%d attemptedObjectIds=%d fetchedObjectBytes=%d%n",
        label,
        elapsedMs,
        observation.treeFetches(),
        observation.objectFetches(),
        observation.jdbcExecutions(),
        observation.objectInsertRows(),
        observation.attemptedObjectIds(),
        observation.fetchedObjectBytes());
    return observation;
  }

  Folder buildDepthPath(Notebook notebook, int depth) {
    Folder parent = null;
    for (int i = 0; i < depth; i++) {
      parent =
          parent == null
              ? makeMe.aFolder().notebook(notebook).name("D" + i).please()
              : makeMe.aFolder().parentFolder(parent).name("D" + i).please();
    }
    return parent;
  }

  void widenAncestors(Notebook notebook, Folder leaf, int ancestorWidth) {
    if (ancestorWidth <= 0) {
      return;
    }
    Folder cursor = leaf;
    while (cursor != null) {
      Folder widthParent = cursor.getParentFolder();
      for (int i = 0; i < ancestorWidth; i++) {
        if (widthParent == null) {
          makeMe.aFolder().notebook(notebook).name(cursor.getName() + "W" + i).please();
        } else {
          makeMe.aFolder().parentFolder(widthParent).name(cursor.getName() + "W" + i).please();
        }
      }
      cursor = widthParent;
    }
  }

  List<Folder> seedUnrelatedFoldersAndNotes(
      Notebook notebook,
      Folder fallbackParent,
      int unrelatedFolders,
      int unrelatedNotes,
      java.util.function.IntFunction<String> noteContent) {
    List<Folder> unrelated = new ArrayList<>(unrelatedFolders);
    for (int i = 0; i < unrelatedFolders; i++) {
      unrelated.add(makeMe.aFolder().notebook(notebook).name("U" + i).please());
    }
    for (int i = 0; i < unrelatedNotes; i++) {
      var builder =
          makeMe
              .aNote()
              .folder(unrelated.isEmpty() ? fallbackParent : unrelated.get(i % unrelated.size()))
              .title("N" + i);
      if (noteContent != null) {
        builder = builder.content(noteContent.apply(i));
      }
      builder.please();
    }
    return unrelated;
  }

  SaveCostObservation observationFrom(SqlStatementCallLog callLog, Notebook notebook) {
    return new SaveCostObservation(
        notebook,
        callLog.countObjectFetches(),
        callLog.countObjectFetchesReturningType(Constants.OBJ_TREE),
        callLog.countObjectFetchesReturningType(Constants.OBJ_COMMIT),
        callLog.countExecutionsMatching("notebook_git_accepted_object", " IN ("),
        callLog.countExecutionsMatching("INSERT INTO notebook_git_accepted_object"),
        callLog.objectInsertRows(),
        callLog.attemptedObjectIds(),
        callLog.fetchedObjectBytes(),
        callLog.countExecutionsMatching("UPDATE notebook_git_binding", "accepted_git_object_id"),
        callLog.executions().size(),
        callLog.countExecutionsMatching("SELECT accepted_git_object_id FROM notebook_git_binding"),
        callLog.executions().stream()
            .dropWhile(
                e -> !e.sql().startsWith("UPDATE notebook_git_binding SET accepted_git_object_id"))
            .filter(
                e -> e.sql().startsWith("SELECT accepted_git_object_id FROM notebook_git_binding"))
            .count(),
        fetchedObjectIdsReturningType(callLog, Constants.OBJ_TREE));
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

  record SaveCostObservation(
      Notebook notebook,
      long objectFetches,
      long treeFetches,
      long commitFetches,
      long existenceCheckExecutions,
      long objectInsertExecutions,
      int objectInsertRows,
      int attemptedObjectIds,
      long fetchedObjectBytes,
      long bindingUpdateExecutions,
      int jdbcExecutions,
      long refReadExecutions,
      long postAppendRefReadExecutions,
      List<ObjectId> fetchedTreeIds) {}
}
