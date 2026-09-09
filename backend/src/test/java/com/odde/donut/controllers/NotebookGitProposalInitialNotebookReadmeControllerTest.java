package com.odde.donut.controllers;

import static com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport.rowsFor;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.AuthoredNoteReferenceRow;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

/** Verifies publication of initial root notebook Readme proposal shapes. */
class NotebookGitProposalInitialNotebookReadmeControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String NOTEBOOK_README =
      "---\ntype: Readme\nsource: local\n---\nPrecisely preserved notebook readme.\n";
  private static final String FIRST_NOTE =
      "---\ntype: Note\n---\nPrecisely preserved first note.\n";
  private static final String SECOND_NOTE =
      "---\ntype: Note\n---\nPrecisely preserved second note.\n";
  private static final String THIRD_NOTE =
      "---\ntype: Note\n---\nPrecisely preserved third note.\n";
  private static final String ROOT_RELATIONSHIP =
      """
      ---
      type: Relationship
      relation: related-to
      source: "[[A]]"
      target: "[[B]]"
      ---
      Precisely preserved relationship body.
      """;

  @Autowired FolderRepository folderRepository;

  @Test
  void publishesInitialNotebookReadmeAndThreeRootNotesAsTheExactAuthoredCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("README.md", NOTEBOOK_README),
                new NotebookGitProposalFile("First note.md", FIRST_NOTE),
                new NotebookGitProposalFile("Second note.md", SECOND_NOTE),
                new NotebookGitProposalFile("Third note.md", THIRD_NOTE)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    assertThat(acceptedNotebook.getReadmeContent(), equalTo(NOTEBOOK_README));
    assertThat(folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(0));
    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(
        notes.stream().map(Note::getTitle).toList(),
        containsInAnyOrder("First note", "Second note", "Third note"));
    Map<String, Note> byTitle =
        notes.stream().collect(Collectors.toMap(Note::getTitle, Function.identity()));
    assertThat(byTitle.get("First note").getContent(), equalTo(FIRST_NOTE));
    assertThat(byTitle.get("First note").getFolder(), nullValue());
    assertThat(byTitle.get("Second note").getContent(), equalTo(SECOND_NOTE));
    assertThat(byTitle.get("Second note").getFolder(), nullValue());
    assertThat(byTitle.get("Third note").getContent(), equalTo(THIRD_NOTE));
    assertThat(byTitle.get("Third note").getFolder(), nullValue());

    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
    }
  }

  @Test
  void publishesInitialNotebookReadmeAndTwoRootNotes() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("README.md", NOTEBOOK_README),
                new NotebookGitProposalFile("First note.md", FIRST_NOTE),
                new NotebookGitProposalFile("Second note.md", SECOND_NOTE)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(notes, hasSize(2));
    Map<String, Note> byTitle =
        notes.stream().collect(Collectors.toMap(Note::getTitle, Function.identity()));
    assertThat(byTitle.get("First note").getContent(), equalTo(FIRST_NOTE));
    assertThat(byTitle.get("Second note").getContent(), equalTo(SECOND_NOTE));
  }

  @Test
  void publishesInitialNotebookReadmeAndRootNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("README.md", NOTEBOOK_README),
                new NotebookGitProposalFile("First note.md", FIRST_NOTE)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(notes, hasSize(1));
    Note createdNote = notes.getFirst();
    assertThat(createdNote.getTitle(), equalTo("First note"));
    assertThat(createdNote.getContent(), equalTo(FIRST_NOTE));
  }

  @Test
  void publishesInitialNotebookReadmeAndRootRelationshipAsTheExactAuthoredCommit()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("README.md", NOTEBOOK_README),
                new NotebookGitProposalFile("A-related-to-B.md", ROOT_RELATIONSHIP)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    assertThat(acceptedNotebook.getReadmeContent(), equalTo(NOTEBOOK_README));
    assertThat(folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(0));
    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(notes, hasSize(1));
    Note relationship = notes.getFirst();
    assertThat(relationship.getTitle(), equalTo("A-related-to-B"));
    assertThat(relationship.getContent(), equalTo(ROOT_RELATIONSHIP));
    assertThat(relationship.getFolder(), nullValue());
    List<String> authoredLinks =
        inCommittedTransaction(
            transactionManager,
            () ->
                rowsFor(entityManager, relationship).stream()
                    .map(AuthoredNoteReferenceRow::getAuthoredLink)
                    .toList());
    assertThat(authoredLinks, containsInAnyOrder("A", "B"));

    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"Readme", "CustomType"})
  void refusesInitialNotebookReadmeAndRootNoteWhenNoteIsNotAnOrdinaryNote(String documentType)
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String readmeBefore =
        notebookRepository.findById(notebook.getId()).orElseThrow().getReadmeContent();
    String notePath = "First note.md";
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("README.md", NOTEBOOK_README),
                new NotebookGitProposalFile(
                    notePath, "---\ntype: " + documentType + "\n---\nBody.\n")));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            notebook, binding.getAcceptedGitObjectId(), proposalBytes, HttpStatus.BAD_REQUEST);

    assertThat(exception.getReason(), containsString(notePath));
    assertThat(exception.getReason(), containsString(documentType));
    assertThat(exception.getReason(), containsString("must have type: Note"));
    Notebook after = notebookRepository.findById(notebook.getId()).orElseThrow();
    assertThat(after.getReadmeContent(), equalTo(readmeBefore));
    inCommittedTransaction(
        transactionManager,
        () -> {
          assertThat(folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()), empty());
          assertThat(
              noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), empty());
        });
  }

  @Test
  void publishesInitialNotebookReadmeAsTheExactAuthoredCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding, List.of(new NotebookGitProposalFile("README.md", NOTEBOOK_README)));

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    assertThat(acceptedNotebook.getReadmeContent(), equalTo(NOTEBOOK_README));
    assertThat(folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(0));
    assertThat(noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()), hasSize(0));
    assertThat(publishedHead, equalTo(proposedCommit.head().getName()));

    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
    }
  }
}
