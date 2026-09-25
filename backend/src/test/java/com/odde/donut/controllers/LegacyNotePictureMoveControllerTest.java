package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Image;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.services.notebookGit.LegacyNotePictureMove;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.sql.Timestamp;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class LegacyNotePictureMoveControllerTest extends NotebookGitWebContentControllerTestBase {
  @Autowired LegacyNotePictureMove legacyNotePictureMove;

  @Test
  void aNotesOwnLegacyPictureBecomesAFileBesideItInOneDonutSystemCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder physics = makeMe.aFolder().notebook(notebook).name("physics").please();
    Note force = makeMe.aNote("force").folder(physics).please();
    Image legacy = makeMe.anImage().forNote(force).by(currentUser.getUser()).please();
    authorReferencingContentCommitted(
        force,
        "---\nimage: /attachments/images/"
            + legacy.getId()
            + "/example.png\nimage_mask: 10 10 20 20\n---\nbody");
    snapshotCurrentPortableTree(notebook);
    List<String> commitsBefore = acceptedHistory(notebook).commits();
    Timestamp updatedAtBefore = reloaded(force).getUpdatedAt();

    legacyNotePictureMove.move(notebook.getId());

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(commitsBefore));
    assertThat(tipAuthorName(notebook), equalTo(NotebookGitCutoverService.SYSTEM_AUTHOR_NAME));
    assertThat(
        tipContent(after, "physics/example.png"),
        equalTo(lfsPointerStoredFor(notebook, "DEADBEEF".getBytes())));
    assertThat(
        tipText(after, "physics/force.md"),
        equalTo("---\ntype: Note\nimage: example.png\nimage_mask: 10 10 20 20\n---\nbody"));
    assertThat(reloaded(force).getUpdatedAt(), equalTo(updatedAtBefore));
    assertThat(legacyImageCount(force), equalTo(1L));
    assertAcceptedTreeMatchesTheFullAssembly(notebook);
  }

  @Test
  void runningTheMoveAgainAddsNoCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note force = makeMe.aNote("force").notebook(notebook).please();
    Image legacy = makeMe.anImage().forNote(force).by(currentUser.getUser()).please();
    authorReferencingContentCommitted(
        force, "---\nimage: /attachments/images/" + legacy.getId() + "/example.png\n---\nbody");
    snapshotCurrentPortableTree(notebook);
    legacyNotePictureMove.move(notebook.getId());
    List<String> commitsAfterFirstMove = acceptedHistory(notebook).commits();

    legacyNotePictureMove.move(notebook.getId());

    assertThat(acceptedHistory(notebook).commits(), equalTo(commitsAfterFirstMove));
  }

  @Test
  void aNotebookWithoutLegacyReferencesGainsNoCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aNote("force").notebook(notebook).content(ACCEPTED_CONTENT).please();
    snapshotCurrentPortableTree(notebook);
    List<String> commitsBefore = acceptedHistory(notebook).commits();

    legacyNotePictureMove.move(notebook.getId());

    assertThat(acceptedHistory(notebook).commits(), equalTo(commitsBefore));
  }

  private Note reloaded(Note note) {
    return noteRepository.findById(note.getId()).orElseThrow();
  }

  private String tipAuthorName(Notebook notebook) throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      return revWalk
          .parseCommit(GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook)))
          .getAuthorIdent()
          .getName();
    }
  }
}
