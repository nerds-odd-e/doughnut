package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies Git publication of a filename-preserving relocation leaves path-qualified referrer bytes
 * exact and stops resolving the old Portable path. Unqualified same-parent rename referrers are
 * covered in {@link NotebookGitProposalRenameControllerTest}. Placement is covered in {@link
 * NotebookGitProposalRelocationControllerTest}. Web folder-move rewrite is out of scope.
 */
class NotebookGitProposalRelocationReferrerControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String TYPED_NOTE_CONTENT = "---\ntype: Note\n---\noriginal content";
  private static final String TARGET_CONTENT = "---\ntype: Note\n---\nOriginal authored bytes.\n";
  private static final String REFERRER_CONTENT =
      """
      ---
      type: Note
      example of: "[[Inbox/Cell]]"
      ---
      Body [[Inbox/Cell]]
      """;

  @Autowired NoteController noteController;

  @Test
  void leavesPathQualifiedReferringBodyAndPropertyLinksAuthoredWhenPublishingRelocation()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder inbox = makeMe.aFolder().notebook(notebook).name("Inbox").please();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    makeMe.aNote().folder(inbox).title("keep").content(TYPED_NOTE_CONTENT).please();
    makeMe.aNote().folder(biology).title("other").content(TYPED_NOTE_CONTENT).please();
    makeMe.aNote().folder(inbox).title("Cell").content(TARGET_CONTENT).please();
    Note referrer =
        makeMe.aNote().notebook(notebook).title("Referrer").content(REFERRER_CONTENT).please();
    inCommittedTransaction(
        transactionManager,
        () ->
            makeMe.authorReferencingContent(
                noteRepository.findById(referrer.getId()).orElseThrow(), REFERRER_CONTENT));
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    List<WikiLink.Resolution> resolutionsBeforePublish =
        inboxCellResolutions(
            noteController.showNote(noteRepository.findById(referrer.getId()).orElseThrow()));
    assertThat(resolutionsBeforePublish, not(empty()));
    assertThat(resolutionsBeforePublish, everyItem(equalTo(WikiLink.Resolution.RESOLVED)));
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Inbox/keep.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("Biology/other.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("Biology/Cell.md", TARGET_CONTENT),
                new NotebookGitProposalFile("Referrer.md", REFERRER_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    NoteRealm shown =
        noteController.showNote(noteRepository.findById(referrer.getId()).orElseThrow());
    assertThat(shown.getNote().getContent(), equalTo(REFERRER_CONTENT));
    assertThat(inboxCellResolutions(shown), empty());
  }

  private static List<WikiLink.Resolution> inboxCellResolutions(NoteRealm shown) {
    return shown.getWikiLinks().stream()
        .filter(link -> "Inbox/Cell".equals(link.getTarget()))
        .map(WikiLink::getResolution)
        .toList();
  }
}
