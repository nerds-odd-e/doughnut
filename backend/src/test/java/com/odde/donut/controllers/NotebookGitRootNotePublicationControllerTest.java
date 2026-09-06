package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/** Verifies root-note creation across Note projection and accepted Git history. */
class NotebookGitRootNotePublicationControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String ORIGINAL_CONTENT = "---\ntype: Note\n---\nOriginal authored bytes.\n";
  private static final String CREATED_CONTENT =
      "---\n"
          + "type: FieldObservation\n"
          + "title: Author-owned title\n"
          + "confidence: 7\n"
          + "unknown-key: keep me\n"
          + "---\n"
          + "# Author heading\n"
          + "Precisely preserved authored bytes linking [[Created Note|itself]].\n";

  @Autowired NoteController noteController;

  @Test
  void publishesANewRootNoteAsTheExactAuthoredCommitAndMakesItDownloadable() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("Created Note.md", CREATED_CONTENT)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(notes, hasSize(1));
    Note created = notes.getFirst();
    NoteRealm shown = noteController.showNote(created);
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));
    assertThat(created.getTitle(), equalTo("Created Note"));
    assertThat(shown.getNote().getContent(), equalTo(CREATED_CONTENT));
    assertThat(shown.getWikiLinks(), hasSize(1));
    assertThat(shown.getWikiLinks().getFirst().getAuthoredLink(), equalTo("Created Note|itself"));
    assertThat(
        shown.getWikiLinks().getFirst().getResolution(), equalTo(WikiLink.Resolution.RESOLVED));
    assertThat(shown.getWikiLinks().getFirst().getDestinationNoteId(), equalTo(created.getId()));

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(
              readBack, downloadedCommit.head(), "Created Note.md"),
          equalTo(CREATED_CONTENT));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(downloadedCommit.parent(), equalTo(acceptedHead));
    }
  }

  @Test
  void publishesANewRootNoteWithoutChangingExistingNotes() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note existing =
        makeMe.aNote().notebook(notebook).title("Existing").content(ORIGINAL_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Existing.md", ORIGINAL_CONTENT),
                new NotebookGitProposalFile("Created Note.md", CREATED_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(notes, hasSize(2));
    Note reloadedExisting = noteRepository.findById(existing.getId()).orElseThrow();
    assertThat(reloadedExisting.getContent(), equalTo(ORIGINAL_CONTENT));
  }

  @Test
  void retriesAnAcceptedRootNoteAdditionWithoutCreatingAnotherNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding initialBinding = snapshotCurrentPortableTree(notebook);
    String initialHead = initialBinding.getAcceptedGitObjectId();
    byte[] proposalBytes =
        proposalBundleBytes(
            initialBinding,
            List.of(new NotebookGitProposalFile("Created Note.md", CREATED_CONTENT)));

    String publishedHead =
        controller.publishNotebookGitProposal(notebook.getId(), initialHead, proposalBytes);
    RootAdditionPublicationState stateAfterPublication = rootAdditionPublicationState(notebook);

    String retriedHead =
        controller.publishNotebookGitProposal(notebook.getId(), initialHead, proposalBytes);

    assertThat(retriedHead, equalTo(publishedHead));
    assertThat(rootAdditionPublicationState(notebook), equalTo(stateAfterPublication));
  }

  private RootAdditionPublicationState rootAdditionPublicationState(Notebook notebook) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          NotebookGitBinding binding =
              notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
          List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
          Note createdNote = notes.getFirst();
          return new RootAdditionPublicationState(
              binding.getAcceptedGitObjectId(),
              binding.getUpdatedAt(),
              notes.size(),
              createdNote.getId(),
              createdNote.getTitle(),
              createdNote.getContent(),
              createdNote.getCreatedAt(),
              createdNote.getUpdatedAt(),
              createdNote.getDeletedAt());
        });
  }

  private record RootAdditionPublicationState(
      String acceptedHead,
      Timestamp bindingUpdatedAt,
      int noteCount,
      Integer noteId,
      String noteTitle,
      String noteContent,
      Timestamp noteCreatedAt,
      Timestamp noteUpdatedAt,
      Timestamp noteDeletedAt) {}
}
