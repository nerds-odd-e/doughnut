package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.MemoryTrackerType;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** Verifies folder README and contained-note projection from accepted local Git content. */
class NotebookGitProposalFolderContentControllerTest
    extends NotebookGitProposalFolderControllerTestBase {

  private static final String NOTE_C = "---\ntype: Note\n---\nPrecisely preserved C.\n";

  @Test
  void publishesAFolderReadmeAndNotesTogetherPreservingExistingIdentityAndLearning()
      throws Exception {
    LearnedNotebook fixture = boundNotebookWithLearnedNote();
    byte[] proposalBytes =
        proposalBundleBytes(
            fixture.binding(),
            withExisting(
                List.of(
                    new NotebookGitProposalFile(FOLDER_README_PATH, FOLDER_README),
                    new NotebookGitProposalFile(NOTE_A_PATH, NOTE_A),
                    new NotebookGitProposalFile(NOTE_B_PATH, NOTE_B))));
    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            fixture.notebook().getId(), fixture.binding().getAcceptedGitObjectId(), proposalBytes);

    List<Folder> folders =
        folderRepository.findByNotebookIdOrderByIdAsc(fixture.notebook().getId());
    assertThat(folders, hasSize(1));
    Folder created = folders.getFirst();
    assertThat(created.getName(), equalTo("例文"));
    assertThat(created.getParentFolderId(), nullValue());
    assertThat(created.getReadmeContent(), equalTo(FOLDER_README));
    List<Note> notes =
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(fixture.notebook().getId());
    assertThat(notes, hasSize(3));
    Note reloadedExisting = noteRepository.findById(fixture.existing().getId()).orElseThrow();
    assertThat(reloadedExisting.getContent(), equalTo(EXISTING_CONTENT));
    assertThat(reloadedExisting.getTitle(), equalTo("Existing"));
    MemoryTracker retained =
        memoryTrackerRepository.findById(fixture.tracker().getId()).orElseThrow();
    assertThat(retained.getNote().getId(), equalTo(fixture.existing().getId()));
    assertThat(retained.getType(), equalTo(MemoryTrackerType.SPELLING));
    assertThat(retained.isActive(), is(true));
    Note noteA = noteByTitle(notes, "A");
    Note noteB = noteByTitle(notes, "B");
    assertThat(noteA.getId(), not(equalTo(fixture.existing().getId())));
    assertThat(noteB.getId(), not(equalTo(fixture.existing().getId())));
    assertThat(noteA.getFolder().getId(), equalTo(created.getId()));
    assertThat(noteB.getFolder().getId(), equalTo(created.getId()));
    assertThat(noteA.getContent(), equalTo(NOTE_A));
    assertThat(noteB.getContent(), equalTo(NOTE_B));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));

    Notebook acceptedNotebook =
        notebookRepository.findById(fixture.notebook().getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(
              readBack, downloadedCommit.head(), FOLDER_README_PATH),
          equalTo(FOLDER_README));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(readBack, downloadedCommit.head(), NOTE_A_PATH),
          equalTo(NOTE_A));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(readBack, downloadedCommit.head(), NOTE_B_PATH),
          equalTo(NOTE_B));
    }
  }

  @Test
  void publishesConceptsBeneathAnImpliedFolderWithoutAFolderReadme() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder examples = makeMe.aFolder().notebook(notebook).name("例文").please();
    makeMe.aNote().folder(examples).title("Existing").content(EXISTING_CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String ordinary = "---\ntype: Note\n---\nA body.\n";
    String relationship =
        "---\ntype: Relationship\nrelation: related-to\nsource: \"[[例文/111/A]]\"\ntarget: \"[[例文/111/B]]\"\n---\nRelation body.\n";
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("例文/Existing.md", EXISTING_CONTENT),
                new NotebookGitProposalFile("例文/111/A.md", ordinary),
                new NotebookGitProposalFile("例文/111/A-related-to-B.md", relationship)));
    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(folders, hasSize(2));
    Folder created =
        folders.stream().filter(folder -> folder.getName().equals("111")).findFirst().orElseThrow();
    assertThat(created.getParentFolderId(), equalTo(examples.getId()));
    assertThat(created.getReadmeContent(), nullValue());
    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    Note noteA = noteByTitle(notes, "A");
    Note related = noteByTitle(notes, "A-related-to-B");
    assertThat(noteA.getFolder().getId(), equalTo(created.getId()));
    assertThat(related.getFolder().getId(), equalTo(created.getId()));
    assertThat(noteA.getContent(), equalTo(ordinary));
    assertThat(related.getContent(), equalTo(relationship));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));
    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
    }
  }

  @Test
  void publishesAFolderReadmeWithOneOrdinaryNote() throws Exception {
    LearnedNotebook fixture = boundNotebookWithLearnedNote();
    byte[] proposalBytes =
        proposalBundleBytes(
            fixture.binding(),
            withExisting(
                List.of(
                    new NotebookGitProposalFile(FOLDER_README_PATH, FOLDER_README),
                    new NotebookGitProposalFile(NOTE_A_PATH, NOTE_A))));

    controller.publishNotebookGitProposal(
        fixture.notebook().getId(), fixture.binding().getAcceptedGitObjectId(), proposalBytes);

    List<Note> notes =
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(fixture.notebook().getId());
    assertThat(notes, hasSize(2));
    assertThat(notes.stream().map(Note::getTitle).toList(), containsInAnyOrder("Existing", "A"));
  }

  @Test
  void publishesSeveralOrdinaryNotesRegardlessOfProposalEntryOrder() throws Exception {
    LearnedNotebook fixture = boundNotebookWithLearnedNote();
    byte[] proposalBytes =
        proposalBundleBytes(
            fixture.binding(),
            withExisting(
                List.of(
                    new NotebookGitProposalFile("例文/C.md", NOTE_C),
                    new NotebookGitProposalFile(NOTE_B_PATH, NOTE_B),
                    new NotebookGitProposalFile(FOLDER_README_PATH, FOLDER_README),
                    new NotebookGitProposalFile(NOTE_A_PATH, NOTE_A))));

    controller.publishNotebookGitProposal(
        fixture.notebook().getId(), fixture.binding().getAcceptedGitObjectId(), proposalBytes);

    List<Note> notes =
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(fixture.notebook().getId());
    assertThat(notes, hasSize(4));
    assertThat(
        notes.stream().map(Note::getTitle).toList(), containsInAnyOrder("Existing", "A", "B", "C"));
  }
}
