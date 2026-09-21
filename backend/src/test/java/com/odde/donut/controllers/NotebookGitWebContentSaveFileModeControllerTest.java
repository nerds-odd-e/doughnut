package com.odde.donut.controllers;

import static com.odde.donut.services.notebookTree.PortableTreeEntry.ofText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.FileMode;
import org.junit.jupiter.api.Test;

/** Accepted file modes do not count as drift: only a path's content decides a note save. */
class NotebookGitWebContentSaveFileModeControllerTest
    extends NotebookGitWebContentControllerTestBase {

  @Test
  void anExecutableAcceptedFileStillMatchesItsUnchangedNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("Root Note").content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);
    String executableHead;
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      executableHead =
          seedAcceptedHistory(
                  notebook,
                  repository,
                  commitOnTopOf(
                      repository,
                      List.of(),
                      List.of(
                          new NotebookGitProposalFile(
                              "Root Note.md", ACCEPTED_CONTENT, FileMode.EXECUTABLE_FILE)),
                      "Executable"))
              .getAcceptedGitObjectId();
    }

    textContentController.updateNoteContent(
        note, contentDto("---\ntype: note\n---\naccepted content"));
    assertThat(binding(notebook).getAcceptedGitObjectId(), is(executableHead));

    textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT));
    AcceptedHistory saved = acceptedHistory(notebook);
    assertThat(saved.commits(), hasSize(2));
    assertThat(saved.tipContent(), contains(ofText("Root Note.md", EDITED_CONTENT)));
  }
}
