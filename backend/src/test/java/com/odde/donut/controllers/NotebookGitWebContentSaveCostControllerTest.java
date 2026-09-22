package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/** A web save's server work depends on what changed, not on the notebook's size. */
class NotebookGitWebContentSaveCostControllerTest extends NotebookGitWebContentControllerTestBase {

  @Test
  void savingContentCostsTheSameStatementsInALargeNotebookWithAttachmentsAsInASmallOne()
      throws Throwable {
    Notebook small = createGitBackedNotebook("Small");
    Note smallNote = makeMe.aNote().notebook(small).content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(small);
    Notebook large = createGitBackedNotebook("Large");
    Note note = makeMe.aNote().notebook(large).content(ACCEPTED_CONTENT).please();
    for (int i = 0; i < 30; i++) {
      makeMe.aNote().notebook(large).title("Unrelated " + i).please();
    }
    for (int i = 0; i < 3; i++) {
      storeFolderAttachmentAndSnapshot(large, null, "attachment-" + i + ".bin", new byte[4096]);
    }
    var acceptedBefore = acceptedHistory(large);

    Statistics statistics =
        entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    long smallStatements =
        preparedStatementsOf(
            statistics,
            () -> textContentController.updateNoteContent(smallNote, contentDto(EDITED_CONTENT)));
    long largeStatements =
        preparedStatementsOf(
            statistics,
            () -> textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT)));

    assertThat(largeStatements, is(smallStatements));
    assertThat(
        List.of(statistics.getQueries()), not(hasItem(containsString("NotebookAttachment"))));
    AcceptedHistory after = acceptedHistory(large);
    assertThat(after.parents(), equalTo(acceptedBefore.commits()));
  }

  /** Statements the save prepared; the queries it ran stay readable from {@code statistics}. */
  private static long preparedStatementsOf(Statistics statistics, Executable save)
      throws Throwable {
    boolean previouslyEnabled = statistics.isStatisticsEnabled();
    statistics.setStatisticsEnabled(true);
    statistics.clear();
    try {
      save.execute();
      return statistics.getPrepareStatementCount();
    } finally {
      statistics.setStatisticsEnabled(previouslyEnabled);
    }
  }
}
