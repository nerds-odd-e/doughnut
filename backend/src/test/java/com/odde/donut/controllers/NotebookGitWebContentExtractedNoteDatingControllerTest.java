package com.odde.donut.controllers;

import static com.odde.donut.controllers.AiControllerExtractNoteTestSupport.extractionResult;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.NotePropertyIndexRepository;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Extracting a note commits both notes in one accepted change and dates the values it adds. */
class NotebookGitWebContentExtractedNoteDatingControllerTest
    extends NotebookGitWebContentControllerTestBase {
  private static final Timestamp CREATED = Timestamp.from(Instant.parse("2026-09-01T08:00:00Z"));
  private static final Timestamp T1 = Timestamp.from(Instant.parse("2026-09-08T10:00:00Z"));
  private static final Timestamp T2 = Timestamp.from(Instant.parse("2026-09-08T10:08:00Z"));

  @Autowired AiController aiController;
  @Autowired NotePropertyIndexRepository notePropertyIndexRepository;

  @Test
  void extractionCommitsBothNotesAndDatesTheValuesItAddsAtThatCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note atom = makeMe.aNote().notebook(notebook).title("Atom").createdAt(CREATED).please();
    snapshotCurrentPortableTree(notebook);
    testabilitySettings.timeTravelTo(T1);
    textContentController.updateNoteContent(
        reload(atom), contentDto("---\ntype: Note\ntopic: physics\n---\nbody"));
    AcceptedHistory before = acceptedHistory(notebook);
    testabilitySettings.timeTravelTo(T2);
    String extractedContent = "---\ntype: Note\norigin: lab\n---\nextracted body";
    String rewrittenContent =
        "---\ntype: Note\ntopic: physics\nexample of: \"[[Matter]]\"\n---\nbody";

    NoteRealm extracted =
        aiController.createExtractedNote(
            reload(atom), extractionResult("Electron", extractedContent, rewrittenContent));

    AcceptedHistory after = acceptedHistory(notebook);
    assertThat(after.parents(), equalTo(before.commits()));
    assertThat(after.tipPaths(), hasItems("Atom.md", "Electron.md"));
    assertThat(
        acceptedBytesAt(notebook, "Atom.md"),
        equalTo(rewrittenContent.getBytes(StandardCharsets.UTF_8)));
    assertThat(
        acceptedBytesAt(notebook, "Electron.md"),
        equalTo(extractedContent.getBytes(StandardCharsets.UTF_8)));
    var diagnosis = controller.diagnoseNotebookGit(notebook.getId());
    assertThat(diagnosis.acceptedTree(), equalTo(after.tipTreeId().name()));
    assertThat(diagnosis.liveTree(), equalTo(after.tipTreeId().name()));
    assertThat(acceptedTipCommitTime(notebook), equalTo(T2.toInstant()));
    assertThat(changedAt(atom.getId(), "example of"), equalTo(T2));
    assertThat(changedAt(extracted.getId(), "origin"), equalTo(T2));
    assertThat(changedAt(atom.getId(), "topic"), equalTo(T1));
  }

  private Note reload(Note target) {
    return noteRepository.findById(target.getId()).orElseThrow();
  }

  private Timestamp changedAt(Integer noteId, String key) {
    return notePropertyIndexRepository
        .findByNote_IdAndPropertyKeyAndPropertyValue(noteId, key, "")
        .getFirst()
        .getChangedAt();
  }

  private Instant acceptedTipCommitTime(Notebook notebook) throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk walk = new RevWalk(repository)) {
      return walk.parseCommit(
              GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook)))
          .getCommitterIdent()
          .getWhenAsInstant();
    }
  }
}
