package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.util.List;
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
}
