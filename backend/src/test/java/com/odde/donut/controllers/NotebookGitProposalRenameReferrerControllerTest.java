package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies Git publication of an ordinary-note rename leaves unqualified referrer bytes exact and
 * stops resolving the renamed target, whether the rename is detected by path alone or inferred from
 * changed content. Path-qualified relocation referrers are covered in {@link
 * NotebookGitProposalRelocationReferrerControllerTest}. Rename acceptance and identity preservation
 * are covered in {@link NotebookGitProposalRenameControllerTest}.
 */
class NotebookGitProposalRenameReferrerControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String TARGET_CONTENT = "---\ntype: Note\n---\nOriginal authored bytes.\n";
  private static final String REFERRER_CONTENT =
      "---\n" + "type: Note\n" + "example of: \"[[Target]]\"\n" + "---\n" + "Body [[Target]]\n";

  @Autowired NoteController noteController;

  @Test
  void leavesReferringBodyAndPropertyLinksAuthoredWhenPublishingTheTargetsRename()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note target =
        makeMe.aNote().notebook(notebook).title("Target").content(TARGET_CONTENT).please();
    Note referrer =
        makeMe.aNote().notebook(notebook).title("Referrer").content(REFERRER_CONTENT).please();
    inCommittedTransaction(
        transactionManager,
        () ->
            makeMe.authorReferencingContent(
                noteRepository.findById(referrer.getId()).orElseThrow(), REFERRER_CONTENT));
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    List<WikiLink.Resolution> resolutionsBeforePublish =
        targetResolutions(
            noteController.showNote(noteRepository.findById(referrer.getId()).orElseThrow()));
    assertThat(resolutionsBeforePublish, not(empty()));
    assertThat(resolutionsBeforePublish, everyItem(equalTo(WikiLink.Resolution.RESOLVED)));
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Target Renamed.md", TARGET_CONTENT),
                new NotebookGitProposalFile("Referrer.md", REFERRER_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    NoteRealm shown =
        noteController.showNote(noteRepository.findById(referrer.getId()).orElseThrow());
    assertThat(shown.getNote().getContent(), equalTo(REFERRER_CONTENT));
    assertThat(targetResolutions(shown), empty());
    Note renamedTarget = noteRepository.findById(target.getId()).orElseThrow();
    assertThat(renamedTarget.getTitle(), equalTo("Target Renamed"));
  }

  @Test
  void leavesReferringBodyAuthoredWhenPublishingAnInferredRenameWithEditedContent()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note target =
        makeMe
            .aNote()
            .notebook(notebook)
            .title("Target")
            .content(SUBSTANTIAL_ORIGINAL_BODY)
            .please();
    Note referrer =
        makeMe.aNote().notebook(notebook).title("Referrer").content(REFERRER_CONTENT).please();
    inCommittedTransaction(
        transactionManager,
        () ->
            makeMe.authorReferencingContent(
                noteRepository.findById(referrer.getId()).orElseThrow(), REFERRER_CONTENT));
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    List<WikiLink.Resolution> resolutionsBeforePublish =
        targetResolutions(
            noteController.showNote(noteRepository.findById(referrer.getId()).orElseThrow()));
    assertThat(resolutionsBeforePublish, not(empty()));
    assertThat(resolutionsBeforePublish, everyItem(equalTo(WikiLink.Resolution.RESOLVED)));
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Target Renamed.md", MODERATE_EDIT_BODY),
                new NotebookGitProposalFile("Referrer.md", REFERRER_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    NoteRealm shown =
        noteController.showNote(noteRepository.findById(referrer.getId()).orElseThrow());
    assertThat(shown.getNote().getContent(), equalTo(REFERRER_CONTENT));
    assertThat(targetResolutions(shown), empty());
    Note renamedTarget = noteRepository.findById(target.getId()).orElseThrow();
    assertThat(renamedTarget.getId(), equalTo(target.getId()));
    assertThat(renamedTarget.getTitle(), equalTo("Target Renamed"));
    assertThat(renamedTarget.getContent(), equalTo(MODERATE_EDIT_BODY));
  }

  private static List<WikiLink.Resolution> targetResolutions(NoteRealm shown) {
    return shown.getWikiLinks().stream()
        .filter(link -> "Target".equals(link.getTarget()))
        .map(WikiLink::getResolution)
        .toList();
  }
}
