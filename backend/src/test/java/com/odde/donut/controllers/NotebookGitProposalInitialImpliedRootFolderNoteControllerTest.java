package com.odde.donut.controllers;

import static com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport.rowsForNotebook;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.algorithms.FrontmatterNoteLevel;
import com.odde.donut.entities.AuthoredNoteReferenceRow;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/** Verifies publication of one or two Notes in an implied root Folder with no Folder Readme. */
class NotebookGitProposalInitialImpliedRootFolderNoteControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String FIRST_NOTE =
      "---\ntype: Note\n---\nPrecisely preserved first note.\n";
  private static final String SECOND_NOTE =
      "---\ntype: Note\n---\nPrecisely preserved second note.\n";

  @Autowired FolderRepository folderRepository;

  @Test
  void publishesTwoNotesInImpliedRootFolderAsTheExactAuthoredCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("New Folder/First note.md", FIRST_NOTE),
                new NotebookGitProposalFile("New Folder/Second note.md", SECOND_NOTE)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    assertThat(acceptedNotebook.getReadmeContent(), nullValue());
    List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(folders, hasSize(1));
    Folder created = folders.getFirst();
    assertThat(created.getName(), equalTo("New Folder"));
    assertThat(created.getParentFolderId(), nullValue());
    assertThat(created.getReadmeContent(), nullValue());
    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(
        notes.stream().map(Note::getTitle).toList(),
        containsInAnyOrder("First note", "Second note"));
    Map<String, Note> byTitle =
        notes.stream().collect(Collectors.toMap(Note::getTitle, Function.identity()));
    assertThat(byTitle.get("First note").getContent(), equalTo(FIRST_NOTE));
    assertThat(byTitle.get("First note").getFolder().getId(), equalTo(created.getId()));
    assertThat(byTitle.get("Second note").getContent(), equalTo(SECOND_NOTE));
    assertThat(byTitle.get("Second note").getFolder().getId(), equalTo(created.getId()));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));

    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
    }
  }

  @Test
  void publishesOneNoteInImpliedRootFolder() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("New Folder/First note.md", FIRST_NOTE)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(notes, hasSize(1));
    Note createdNote = notes.getFirst();
    assertThat(createdNote.getTitle(), equalTo("First note"));
    assertThat(createdNote.getContent(), equalTo(FIRST_NOTE));
  }

  @Test
  void rejectsALaterInvalidContainedNoteWithoutPartialImpliedFolderPublication() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    List<Folder> foldersBefore =
        inCommittedTransaction(
            transactionManager,
            () -> folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()));
    List<Note> notesBefore =
        inCommittedTransaction(
            transactionManager,
            () -> noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()));
    List<AuthoredNoteReferenceRow> referencesBefore =
        inCommittedTransaction(
            transactionManager, () -> rowsForNotebook(entityManager, notebook.getId()));
    assertThat(foldersBefore, empty());
    assertThat(notesBefore, empty());
    assertThat(referencesBefore, empty());
    String laterPath = "New Folder/Second note.md";
    byte[] proposal =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile(
                    "New Folder/First note.md", "---\ntype: Note\n---\nSee [[rollback target]].\n"),
                new NotebookGitProposalFile(
                    laterPath, "---\ntype: Note\nnote_level: 7\n---\ninvalid content")));

    ApiException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposal, ApiException.class);

    assertThat(exception.getErrorBody().getMessage(), containsString(laterPath));
    assertThat(
        exception.getErrorBody().getMessage(),
        containsString(FrontmatterNoteLevel.AUTHORED_NOTE_LEVEL_MESSAGE));
    assertThat(
        exception.getErrorBody().getErrors().get("note_level"),
        equalTo(FrontmatterNoteLevel.AUTHORED_NOTE_LEVEL_MESSAGE));
    inCommittedTransaction(
        transactionManager,
        () -> {
          assertThat(
              folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()),
              equalTo(foldersBefore));
          assertThat(
              noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()),
              equalTo(notesBefore));
          assertThat(rowsForNotebook(entityManager, notebook.getId()), equalTo(referencesBefore));
        });
  }
}
