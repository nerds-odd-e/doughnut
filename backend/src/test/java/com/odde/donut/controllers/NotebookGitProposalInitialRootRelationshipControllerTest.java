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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

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
  void publishedNestedRelationshipResolvesPathQualifiedNotesCreatedLater() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile(
                    "A/Relationships/Related.md",
                    ROOT_RELATIONSHIP
                        .replace("[[A]]", "[[Z/First]]")
                        .replace("[[B]]", "[[Z/Nested/Second]]")),
                new NotebookGitProposalFile("Z/First.md", FIRST_NOTE),
                new NotebookGitProposalFile("Z/Nested/Second.md", SECOND_NOTE)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Map<String, Note> byTitle =
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .collect(Collectors.toMap(Note::getTitle, Function.identity()));
    NoteRealm shown = noteController.showNote(byTitle.get("Related"));
    WikiLink source = wikiLink(shown, "Z/First");
    WikiLink target = wikiLink(shown, "Z/Nested/Second");
    assertThat(source.getTarget(), equalTo("Z/First"));
    assertThat(target.getTarget(), equalTo("Z/Nested/Second"));
    assertThat(source.getResolution(), equalTo(WikiLink.Resolution.RESOLVED));
    assertThat(source.getDestinationNoteId(), equalTo(byTitle.get("First").getId()));
    assertThat(target.getResolution(), equalTo(WikiLink.Resolution.RESOLVED));
    assertThat(target.getDestinationNoteId(), equalTo(byTitle.get("Second").getId()));
  }

  @ParameterizedTest
  @CsvSource({"Missing, UNRESOLVED", "Endpoint, AMBIGUOUS"})
  void leavesMissingOrAmbiguousPublishedTargetsUnresolvedWithoutCreatingEndpoints(
      String authoredTarget, WikiLink.Resolution resolution) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String relationshipContent = ROOT_RELATIONSHIP.replace("[[B]]", "[[" + authoredTarget + "]]");
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("A/Relationships/Related.md", relationshipContent),
                new NotebookGitProposalFile("Z/Endpoint.md", FIRST_NOTE),
                new NotebookGitProposalFile("Z/Nested/Endpoint.md", SECOND_NOTE)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    Note relationship =
        notes.stream().filter(note -> "Related".equals(note.getTitle())).findFirst().orElseThrow();
    NoteRealm shown = noteController.showNote(relationship);
    if (resolution == WikiLink.Resolution.UNRESOLVED) {
      assertThat(shown.getWikiLinks(), hasSize(0));
    } else {
      WikiLink target = wikiLink(shown, authoredTarget);
      assertThat(target.getResolution(), equalTo(resolution));
      assertThat(target.getDestinationNoteId(), nullValue());
      assertThat(target.getTarget(), equalTo(authoredTarget));
    }
    assertThat(relationship.getContent(), equalTo(relationshipContent));
    assertThat(
        notes.stream().map(Note::getTitle).toList(),
        containsInAnyOrder("Related", "Endpoint", "Endpoint"));
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
