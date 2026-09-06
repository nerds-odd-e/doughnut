package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;

class NotebookGitWebContentFolderSaveControllerTest
    extends NotebookGitWebContentControllerTestBase {

  @Test
  void preservesNestedFolderPathAndCanonicalAuthoredMarkdownOnTheSameLearnedNote()
      throws Exception {
    String authoredContent =
        "---\n"
            + "type: note\n"
            + "aliases:\n"
            + "  - Alpine seedling\n"
            + "custom_field: \"kept exactly\"\n"
            + "---\n"
            + "# Leading heading\n\n"
            + "Authored body.\n";
    String canonicalContent = authoredContent.replace("type: note", "type: Note");
    Notebook notebook = createGitBackedNotebook();
    Folder fieldNotes = makeMe.aFolder().notebook(notebook).name("Field Notes").please();
    Folder alpine = makeMe.aFolder().parentFolder(fieldNotes).name("Alpine").please();
    Note note =
        makeMe.aNote().folder(alpine).title("Observation").content(ACCEPTED_CONTENT).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
                    .difficulty(6f)
                    .please());
    NotebookGitBinding accepted = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(accepted.getAcceptedGitObjectId());

    NoteRealm response = textContentController.updateNoteContent(note, contentDto(authoredContent));

    Note reloaded = noteRepository.findById(note.getId()).orElseThrow();
    NoteRealm readBackNote = noteController.showNote(reloaded);
    assertThat(response.getId(), is(note.getId()));
    assertThat(readBackNote.getId(), is(note.getId()));
    assertThat(readBackNote.getNote().getContent(), is(canonicalContent));
    assertThat(
        noteController.getNoteInfo(reloaded).getMemoryTrackers().getFirst().getId(),
        is(tracker.getId()));
    assertThat(
        noteController.getNoteInfo(reloaded).getMemoryTrackers().getFirst().getDifficulty(),
        is(6f));

    byte[] downloaded =
        controller
            .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
            .getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId editedHead = GitBundleTestReader.fetchHead(repository, downloaded);
      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit commit = revWalk.parseCommit(editedHead);
        assertThat(commit.getParentCount(), is(1));
        assertThat(commit.getParent(0).getId(), is(acceptedHead));
      }
      assertThat(
          portablePaths(repository, editedHead), contains("Field Notes/Alpine/Observation.md"));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(
              repository, editedHead, "Field Notes/Alpine/Observation.md"),
          is(canonicalContent));
    }
  }
}
