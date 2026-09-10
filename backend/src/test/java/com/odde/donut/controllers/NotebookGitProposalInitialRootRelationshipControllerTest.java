package com.odde.donut.controllers;

import static com.odde.donut.entities.repositories.AuthoredNoteReferenceRowTestSupport.rowsFor;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.WikiLink;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * Verifies publication of initial notebook Readme plus one root Relationship, with or without
 * exactly two root ordinary Notes, and that same-commit wiki links resolve to those Notes. Invalid
 * Relationship properties in the mixed layout are covered in {@link
 * NotebookGitProposalInitialRootRelationshipRejectionControllerTest}.
 */
class NotebookGitProposalInitialRootRelationshipControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String NOTEBOOK_README =
      "---\ntype: Readme\nsource: local\n---\nPrecisely preserved notebook readme.\n";
  private static final String FIRST_NOTE =
      "---\ntype: Note\n---\nPrecisely preserved first note.\n";
  private static final String SECOND_NOTE =
      "---\ntype: Note\n---\nPrecisely preserved second note.\n";
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
  private static final String ROOT_RELATIONSHIP_WITH_CUSTOM_PROPERTY =
      """
      ---
      type: Relationship
      relation: related-to
      source: "[[A]]"
      target: "[[B]]"
      custom_authored: preserved
      ---
      Precisely preserved mixed relationship body.
      """;

  @Autowired FolderRepository folderRepository;
  @Autowired NoteController noteController;

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

  @Test
  void publishesInitialNotebookReadmeTwoRootNotesAndRootRelationshipAsTheExactAuthoredCommit()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(binding, initialReadmeTwoRootNotesAndRootRelationshipFiles());

    GitBundleTestReader.SingleParentGitCommit proposedCommit;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      proposedCommit = GitBundleTestReader.fetchSingleParentCommit(proposal, proposalBytes);
    }

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Notebook acceptedNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
    assertThat(acceptedNotebook.getReadmeContent(), equalTo(NOTEBOOK_README));
    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(
        notes.stream().map(Note::getTitle).toList(),
        containsInAnyOrder("A", "B", "A-related-to-B"));
    Map<String, Note> byTitle =
        notes.stream().collect(Collectors.toMap(Note::getTitle, Function.identity()));
    assertThat(byTitle.get("A").getContent(), equalTo(FIRST_NOTE));
    assertThat(byTitle.get("A").getFolder(), nullValue());
    assertThat(byTitle.get("B").getContent(), equalTo(SECOND_NOTE));
    assertThat(byTitle.get("B").getFolder(), nullValue());
    assertThat(
        byTitle.get("A-related-to-B").getContent(),
        equalTo(ROOT_RELATIONSHIP_WITH_CUSTOM_PROPERTY));
    assertThat(byTitle.get("A-related-to-B").getFolder(), nullValue());

    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(acceptedNotebook);
    try (InMemoryRepository readBack = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit downloadedCommit =
          GitBundleTestReader.fetchSingleParentCommit(readBack, downloaded.getBody());
      assertThat(downloadedCommit.head(), equalTo(proposedCommit.head()));
      assertThat(downloadedCommit.tree(), equalTo(proposedCommit.tree()));
    }
  }

  @Test
  void publishedRootRelationshipWikiLinksResolveToNewlyPublishedNotes() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(binding, initialReadmeTwoRootNotesAndRootRelationshipFiles());

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Map<String, Note> byTitle =
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .collect(Collectors.toMap(Note::getTitle, Function.identity()));
    NoteRealm shown = noteController.showNote(byTitle.get("A-related-to-B"));
    WikiLink source = wikiLink(shown, "A");
    WikiLink target = wikiLink(shown, "B");
    assertThat(source.getResolution(), equalTo(WikiLink.Resolution.RESOLVED));
    assertThat(source.getDestinationNoteId(), equalTo(byTitle.get("A").getId()));
    assertThat(target.getResolution(), equalTo(WikiLink.Resolution.RESOLVED));
    assertThat(target.getDestinationNoteId(), equalTo(byTitle.get("B").getId()));
  }

  private static List<NotebookGitProposalFile> initialReadmeTwoRootNotesAndRootRelationshipFiles() {
    return List.of(
        new NotebookGitProposalFile("A-related-to-B.md", ROOT_RELATIONSHIP_WITH_CUSTOM_PROPERTY),
        new NotebookGitProposalFile("README.md", NOTEBOOK_README),
        new NotebookGitProposalFile("B.md", SECOND_NOTE),
        new NotebookGitProposalFile("A.md", FIRST_NOTE));
  }

  private static WikiLink wikiLink(NoteRealm shown, String authoredLink) {
    return shown.getWikiLinks().stream()
        .filter(link -> authoredLink.equals(link.getAuthoredLink()))
        .findFirst()
        .orElseThrow();
  }
}
