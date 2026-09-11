package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.dto.HealthFindingGroup;
import com.odde.donut.controllers.dto.HealthFindingItem;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NotebookHealthLintReport;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.health.HealthRuleIds;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

class NotebookGitAdvisoryNamePublicationControllerTest extends NotebookGitBundleControllerTestBase {

  private static final String CONTENT = "---\ntype: Note\n---\nAdvisory-name content.\n";

  @Autowired NotebookHealthController notebookHealthController;
  @Autowired NoteController noteController;

  @Test
  void publishesAndReimportsAPipeTitledFileWithItsReferencesUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder folder = makeMe.aFolder().notebook(notebook).name("folder").please();
    Note target = makeMe.aNote().folder(folder).title("Target").content(CONTENT).please();
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);
    String pipeContent =
        """
        ---
        type: Note
        see: '[[Target|From YAML]]'
        ---
        Body [[Target|From body]].
        """;
    byte[] proposalBytes =
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("folder/A|B.md", pipeContent),
                new NotebookGitProposalFile("folder/Target.md", CONTENT)));

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), proposalBytes);

    Note created = liveNoteNamed(notebook, "A|B");
    assertContentAndReferenceDestinations(created, target, pipeContent);

    ResponseEntity<byte[]> downloaded = controller.downloadNotebookGitBundle(notebook);
    try (InMemoryRepository accepted = new InMemoryRepository(new DfsRepositoryDescription())) {
      GitBundleTestReader.SingleParentGitCommit commit =
          GitBundleTestReader.fetchSingleParentCommit(accepted, downloaded.getBody());
      assertThat(commit.head().getName(), equalTo(publishedHead));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(accepted, commit.head(), "folder/A|B.md"),
          equalTo(pipeContent));
    }

    String reimportedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), binding.getAcceptedGitObjectId(), downloaded.getBody());
    assertThat(reimportedHead, equalTo(publishedHead));
    Note reimported = liveNoteNamed(notebook, "A|B");
    assertThat(reimported.getId(), equalTo(created.getId()));
    assertContentAndReferenceDestinations(reimported, target, pipeContent);

    HealthFindingGroup pipeNames =
        healthGroup(notebookHealthController.lint(notebook), HealthRuleIds.PIPE_NAME_COMPATIBILITY);
    assertThat(
        pipeNames.getItems().stream().map(HealthFindingItem::getNoteId).toList(),
        contains(created.getId()));
  }

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
    return healthGroup(report, HealthRuleIds.OKF_INCOMPATIBLE_TITLES);
  }

  private HealthFindingGroup healthGroup(NotebookHealthLintReport report, String ruleId) {
    return report.getGroups().stream()
        .filter(group -> ruleId.equals(group.getRuleId()))
        .findFirst()
        .orElseThrow();
  }

  private Note liveNoteNamed(Notebook notebook, String title) {
    List<Note> matches =
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()).stream()
            .filter(note -> note.getTitle().equals(title))
            .toList();
    assertThat(matches, hasSize(1));
    return matches.getFirst();
  }

  private void assertContentAndReferenceDestinations(Note note, Note target, String content)
      throws Exception {
    NoteRealm shown = noteController.showNote(note);
    assertThat(shown.getNote().getContent(), equalTo(content));
    assertThat(shown.getWikiLinks(), hasSize(2));
    assertThat(
        shown.getWikiLinks().stream().map(WikiLink::getDestinationNoteId).toList(),
        contains(target.getId(), target.getId()));
  }
}
