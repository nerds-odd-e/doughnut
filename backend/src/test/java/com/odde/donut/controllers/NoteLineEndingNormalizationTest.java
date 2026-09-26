package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.services.notebookGit.NoteLineEndingNormalization;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NoteLineEndingNormalizationTest extends NotebookGitWebContentControllerTestBase {
  @Autowired NoteLineEndingNormalization noteLineEndingNormalization;

  @Test
  void crlfNotesBecomeLfInOneDonutSystemCommitPerNotebookAndARerunAddsNothing() throws Exception {
    Notebook crlfNotebook = createGitBackedNotebook("CRLF");
    Folder physics = makeMe.aFolder().notebook(crlfNotebook).name("physics").please();
    Note force = makeMe.aNote("force").folder(physics).content("line1\r\nline2").please();
    Note mass = makeMe.aNote("mass").folder(physics).content("a\r\nb\r\n").please();
    makeMe.aNote("speed").folder(physics).content("already\nLF").please();
    snapshotCurrentPortableTree(crlfNotebook);
    Notebook lfNotebook = createGitBackedNotebook("LF");
    makeMe.aNote("plain").notebook(lfNotebook).content("x\ny").please();
    snapshotCurrentPortableTree(lfNotebook);
    AcceptedHistory crlfBefore = acceptedHistory(crlfNotebook);
    List<String> lfCommitsBefore = acceptedHistory(lfNotebook).commits();

    runNormalization();

    AcceptedHistory crlfAfter = acceptedHistory(crlfNotebook);
    assertThat(crlfAfter.parents(), equalTo(crlfBefore.commits()));
    assertThat(tipAuthorName(crlfNotebook), equalTo(NotebookGitCutoverService.SYSTEM_AUTHOR_NAME));
    assertThat(
        changedPaths(crlfBefore, crlfAfter),
        containsInAnyOrder("physics/force.md", "physics/mass.md"));
    assertThat(tipText(crlfAfter, "physics/force.md"), not(containsString("\r")));
    assertThat(reloaded(force).getContent(), equalTo("line1\nline2"));
    assertThat(reloaded(mass).getContent(), equalTo("a\nb\n"));
    assertThat(acceptedHistory(lfNotebook).commits(), equalTo(lfCommitsBefore));

    runNormalization();

    assertThat(acceptedHistory(crlfNotebook).commits(), equalTo(crlfAfter.commits()));
  }

  private void runNormalization() throws Exception {
    for (Integer notebookId : noteLineEndingNormalization.notebookIdsWithCrlfNotes()) {
      noteLineEndingNormalization.normalize(notebookId);
    }
  }

  private static List<String> changedPaths(AcceptedHistory before, AcceptedHistory after) {
    return after.content().stream()
        .filter(entry -> !before.content().contains(entry))
        .map(PortableTreeEntry::path)
        .toList();
  }

  private Note reloaded(Note note) {
    return noteRepository.findById(note.getId()).orElseThrow();
  }

  private String tipAuthorName(Notebook notebook) throws Exception {
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      return revWalk
          .parseCommit(GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(notebook)))
          .getAuthorIdent()
          .getName();
    }
  }
}
