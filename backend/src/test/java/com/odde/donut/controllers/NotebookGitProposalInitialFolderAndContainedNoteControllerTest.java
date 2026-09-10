package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/** Verifies publication of an initial root Folder Readme with one or two contained Notes. */
class NotebookGitProposalInitialFolderAndContainedNoteControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String FOLDER_README =
      "---\ntype: Readme\nsource: local\n---\nPrecisely preserved folder readme.\n";
  private static final String FIRST_NOTE =
      "---\ntype: Note\n---\nPrecisely preserved first note.\n";
  private static final String SECOND_NOTE =
      "---\ntype: Note\n---\nPrecisely preserved second note.\n";

  @Autowired FolderRepository folderRepository;

  @Test
  void publishesInitialRootFolderAndTwoContainedNotesAsTheExactAuthoredCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("New Folder/README.md", FOLDER_README),
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
    assertThat(created.getReadmeContent(), equalTo(FOLDER_README));
    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(notes, hasSize(2));
    Map<String, Note> byTitle =
        notes.stream().collect(Collectors.toMap(Note::getTitle, Function.identity()));
    Note first = byTitle.get("First note");
    Note second = byTitle.get("Second note");
    assertThat(first.getContent(), equalTo(FIRST_NOTE));
    assertThat(first.getFolder().getId(), equalTo(created.getId()));
    assertThat(second.getContent(), equalTo(SECOND_NOTE));
    assertThat(second.getFolder().getId(), equalTo(created.getId()));
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
  void publishesInitialRootFolderAndContainedNoteAsTheExactAuthoredCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("New Folder/README.md", FOLDER_README),
                new NotebookGitProposalFile("New Folder/First note.md", FIRST_NOTE)));

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
    assertThat(created.getReadmeContent(), equalTo(FOLDER_README));
    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(notes, hasSize(1));
    Note createdNote = notes.getFirst();
    assertThat(createdNote.getTitle(), equalTo("First note"));
    assertThat(createdNote.getContent(), equalTo(FIRST_NOTE));
    assertThat(createdNote.getFolder().getId(), equalTo(created.getId()));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));

    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"Relationship", "Readme", "CustomType"})
  void publishesInitialFolderConceptWithAnyValidType(String documentType) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String notePath = "New Folder/First note.md";
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("New Folder/README.md", FOLDER_README),
                new NotebookGitProposalFile(
                    notePath, "---\ntype: " + documentType + "\n---\nBody.\n")));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);
    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(notes, hasSize(1));
    assertThat(
        notes.getFirst().getContent(), equalTo("---\ntype: " + documentType + "\n---\nBody.\n"));
  }
}
