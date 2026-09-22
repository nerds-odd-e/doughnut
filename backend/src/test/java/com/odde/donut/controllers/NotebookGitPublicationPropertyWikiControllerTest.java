package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.NoteReferenceService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Property wiki references and aliases materialize through accepted publication. */
class NotebookGitPublicationPropertyWikiControllerTest extends NotebookGitControllerTestBase {

  private static final String TARGET_NOTE_CONTENT = "---\ntype: Note\n---\nTarget body.\n";
  private static final String CARRIER_WITH_PROPERTY_AND_ALIAS =
      """
      ---
      type: Note
      example of: "[[Target]]"
      aliases:
        - color
      ---
      Carrier body.
      """;
  private static final String CARRIER_WITH_STALE_PROPERTY_AND_ALIAS =
      """
      ---
      type: Note
      example of: "[[Old Target]]"
      aliases:
        - old-color
      ---
      Carrier body.
      """;
  private static final String ALIAS_VIEWER_CONTENT = "---\ntype: Note\n---\nSee [[color]]\n";
  private static final String STALE_ALIAS_VIEWER_CONTENT =
      "---\ntype: Note\n---\nSee [[old-color]]\n";

  @Autowired NoteController noteController;
  @Autowired NoteReferenceService noteReferenceService;

  @Test
  void publishesPropertyWikiReferencesAndAliasesVisibleAfterCommit() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note target =
        makeMe.aNote().notebook(notebook).title("Target").content(TARGET_NOTE_CONTENT).please();
    Note viewer = makeMe.aNote().notebook(notebook).title("Viewer").please();
    authorReferencingContentCommitted(viewer, ALIAS_VIEWER_CONTENT);
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Target.md", TARGET_NOTE_CONTENT),
                new NotebookGitProposalFile("Viewer.md", ALIAS_VIEWER_CONTENT),
                new NotebookGitProposalFile("Carrier.md", CARRIER_WITH_PROPERTY_AND_ALIAS))));

    Note carrier =
        noteRepository.findAllByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .filter(note -> note.getTitle().equals("Carrier"))
            .findFirst()
            .orElseThrow();
    NoteRealm published = shownAfterCommit(carrier);
    assertThat(
        wikiLink(published, "Target").getResolution(), equalTo(WikiLink.Resolution.RESOLVED));
    assertThat(wikiLink(published, "Target").getDestinationNoteId(), equalTo(target.getId()));
    NoteRealm inbound = shownAfterCommit(viewer);
    assertThat(wikiLink(inbound, "color").getResolution(), equalTo(WikiLink.Resolution.RESOLVED));
    assertThat(wikiLink(inbound, "color").getDestinationNoteId(), equalTo(carrier.getId()));
  }

  @Test
  void replacingPropertyWikiAndAliasRemovesStaleDerivedEntries() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note target =
        makeMe.aNote().notebook(notebook).title("Target").content(TARGET_NOTE_CONTENT).please();
    makeMe.aNote().notebook(notebook).title("Old Target").content(TARGET_NOTE_CONTENT).please();
    Note carrier =
        makeMe
            .aNote()
            .notebook(notebook)
            .title("Carrier")
            .content(CARRIER_WITH_STALE_PROPERTY_AND_ALIAS)
            .please();
    inCommittedTransaction(
        transactionManager,
        () -> {
          Note reloaded = noteRepository.findById(carrier.getId()).orElseThrow();
          makeMe.authorReferencingContent(reloaded, CARRIER_WITH_STALE_PROPERTY_AND_ALIAS);
          noteReferenceService.refreshDerivedIndexesForNote(reloaded);
        });
    Note staleViewer = makeMe.aNote().notebook(notebook).title("Stale Viewer").please();
    authorReferencingContentCommitted(staleViewer, STALE_ALIAS_VIEWER_CONTENT);
    Note viewer = makeMe.aNote().notebook(notebook).title("Viewer").please();
    authorReferencingContentCommitted(viewer, ALIAS_VIEWER_CONTENT);
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    controller.publishNotebookGitProposal(
        notebook.getId(),
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Target.md", TARGET_NOTE_CONTENT),
                new NotebookGitProposalFile("Old Target.md", TARGET_NOTE_CONTENT),
                new NotebookGitProposalFile("Stale Viewer.md", STALE_ALIAS_VIEWER_CONTENT),
                new NotebookGitProposalFile("Viewer.md", ALIAS_VIEWER_CONTENT),
                new NotebookGitProposalFile("Carrier.md", CARRIER_WITH_PROPERTY_AND_ALIAS))));

    NoteRealm published = shownAfterCommit(carrier);
    assertThat(wikiLink(published, "Target").getDestinationNoteId(), equalTo(target.getId()));
    assertThat(
        published.getWikiLinks().stream()
            .filter(link -> "Old Target".equals(link.getAuthoredLink()))
            .toList(),
        empty());
    assertThat(shownAfterCommit(staleViewer).getWikiLinks(), empty());
    NoteRealm inbound = shownAfterCommit(viewer);
    assertThat(wikiLink(inbound, "color").getDestinationNoteId(), equalTo(carrier.getId()));
  }

  private NoteRealm shownAfterCommit(Note note) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          try {
            return noteController.showNote(noteRepository.findById(note.getId()).orElseThrow());
          } catch (UnexpectedNoAccessRightException exception) {
            throw new IllegalStateException(exception);
          }
        });
  }

  private static WikiLink wikiLink(NoteRealm realm, String authoredLink) {
    return realm.getWikiLinks().stream()
        .filter(link -> authoredLink.equals(link.getAuthoredLink()))
        .findFirst()
        .orElseThrow();
  }
}
