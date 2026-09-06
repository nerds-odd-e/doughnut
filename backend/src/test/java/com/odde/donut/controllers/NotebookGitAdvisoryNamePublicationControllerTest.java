package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.dto.HealthFindingGroup;
import com.odde.donut.controllers.dto.HealthFindingItem;
import com.odde.donut.controllers.dto.NotebookHealthLintReport;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.health.HealthRuleIds;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

class NotebookGitAdvisoryNamePublicationControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String CONTENT = "---\ntype: Note\n---\nAdvisory-name content.\n";

  @Autowired NotebookHealthController notebookHealthController;

  @ParameterizedTest
  @ValueSource(strings = {"index.md", "log.md"})
  void publishesAdvisoryNamesAndReportsTheirExistingHealthWarning(String filename)
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    byte[] proposalBytes =
        proposalBundleBytes(binding, List.of(new NotebookGitProposalFile(filename, CONTENT)));

    controller.publishNotebookGitProposal(
        notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    List<Note> notes = noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId());
    assertThat(notes, hasSize(1));
    Note created = notes.getFirst();
    String displayName = filename.substring(0, filename.length() - ".md".length());
    assertThat(created.getTitle(), equalTo(displayName));

    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(notebook);
    try (InMemoryRepository accepted = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit commit =
          GitBundleTestReader.fetchSingleParentCommit(accepted, downloaded.getBody());
      assertThat(
          NotebookGitProposalBlobText.readUtf8(accepted, commit.head(), filename),
          equalTo(CONTENT));
    }

    HealthFindingGroup advisoryNames = advisoryNames(notebookHealthController.lint(notebook));
    assertThat(
        advisoryNames.getItems().stream().map(HealthFindingItem::getNoteId).toList(),
        contains(created.getId()));
    assertThat(
        advisoryNames.getItems().stream().map(HealthFindingItem::getLabel).toList(),
        contains(displayName));
  }

  private HealthFindingGroup advisoryNames(NotebookHealthLintReport report) {
    return report.getGroups().stream()
        .filter(group -> HealthRuleIds.OKF_INCOMPATIBLE_TITLES.equals(group.getRuleId()))
        .findFirst()
        .orElseThrow();
  }
}
