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
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookExport.ExportReadmeMarkdown;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies Git publication of a folder relocation leaves path-qualified referrer bytes exact and
 * stops resolving the old Portable path. Note relocation referrers are covered in {@link
 * NotebookGitProposalRelocationReferrerControllerTest}. Placement is covered in {@link
 * NotebookGitProposalFolderRelocationControllerTest}. Web folder-move rewrite is out of scope.
 */
class NotebookGitProposalFolderRelocationReferrerControllerTest
    extends NotebookGitBundleControllerTestBase {

  private static final String README_BODY = "readme";
  private static final String README = ExportReadmeMarkdown.assemble(README_BODY);
  private static final String TARGET_CONTENT = "---\ntype: Note\n---\nOriginal authored bytes.\n";
  private static final String REFERRER_CONTENT =
      """
      ---
      type: Note
      example of: "[[Topics/Cell]]"
      ---
      Body [[Topics/Cell]]
      """;

  @Autowired NoteController noteController;

  @Test
  void leavesPathQualifiedReferringBodyAndPropertyLinksAuthoredWhenPublishingFolderRelocation()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aFolder().notebook(notebook).name("Archive").readmeContent(README_BODY).please();
    Folder topics =
        makeMe.aFolder().notebook(notebook).name("Topics").readmeContent(README_BODY).please();
    makeMe.aNote().folder(topics).title("Cell").content(TARGET_CONTENT).please();
    Note insideReferrer =
        makeMe.aNote().folder(topics).title("Inside").content(REFERRER_CONTENT).please();
    Note outsideReferrer =
        makeMe.aNote().notebook(notebook).title("Outside").content(REFERRER_CONTENT).please();
    inCommittedTransaction(
        transactionManager,
        () -> {
          makeMe.authorReferencingContent(
              noteRepository.findById(insideReferrer.getId()).orElseThrow(), REFERRER_CONTENT);
          makeMe.authorReferencingContent(
              noteRepository.findById(outsideReferrer.getId()).orElseThrow(), REFERRER_CONTENT);
        });
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    assertTopicsCellResolved(insideReferrer);
    assertTopicsCellResolved(outsideReferrer);
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Archive/README.md", README),
                new NotebookGitProposalFile("Archive/Topics/README.md", README),
                new NotebookGitProposalFile("Archive/Topics/Cell.md", TARGET_CONTENT),
                new NotebookGitProposalFile("Archive/Topics/Inside.md", REFERRER_CONTENT),
                new NotebookGitProposalFile("Outside.md", REFERRER_CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    assertAuthoredTopicsCellUnresolved(insideReferrer);
    assertAuthoredTopicsCellUnresolved(outsideReferrer);
  }

  private void assertTopicsCellResolved(Note referrer) throws UnexpectedNoAccessRightException {
    List<WikiLink.Resolution> resolutions = topicsCellResolutions(shown(referrer));
    assertThat(resolutions, not(empty()));
    assertThat(resolutions, everyItem(equalTo(WikiLink.Resolution.RESOLVED)));
  }

  private void assertAuthoredTopicsCellUnresolved(Note referrer)
      throws UnexpectedNoAccessRightException {
    NoteRealm shown = shown(referrer);
    assertThat(shown.getNote().getContent(), equalTo(REFERRER_CONTENT));
    assertThat(topicsCellResolutions(shown), empty());
  }

  private NoteRealm shown(Note referrer) throws UnexpectedNoAccessRightException {
    return noteController.showNote(noteRepository.findById(referrer.getId()).orElseThrow());
  }

  private static List<WikiLink.Resolution> topicsCellResolutions(NoteRealm shown) {
    return shown.getWikiLinks().stream()
        .filter(link -> "Topics/Cell".equals(link.getTarget()))
        .map(WikiLink::getResolution)
        .toList();
  }
}
