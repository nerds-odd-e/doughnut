package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;

class NotebookGitMixedEditingControllerTest extends NotebookGitWebContentControllerTestBase {

  private static final String NOTE_PATH = "Mixed Note.md";
  private static final String FIRST_WEB_CONTENT = "---\ntype: Note\n---\nfirst web edit";
  private static final String LOCAL_CONTENT = "---\ntype: Note\n---\nlocal edit";
  private static final String SECOND_WEB_CONTENT = "---\ntype: Note\n---\nsecond web edit";

  @Test
  void keepsExactAncestryAcrossAlternatingWebAndLocalEditsOnTheSameNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("Mixed Note").content(ACCEPTED_CONTENT).please();
    Integer noteId = note.getId();
    NotebookGitBinding accepted = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(accepted.getAcceptedGitObjectId());

    NoteRealm firstWebSave =
        textContentController.updateNoteContent(note, contentDto(FIRST_WEB_CONTENT));
    NotebookGitBinding afterFirstWebSave = binding(notebook);
    ObjectId firstWebHead = ObjectId.fromString(afterFirstWebSave.getAcceptedGitObjectId());
    assertThat(firstWebSave.getId(), is(noteId));
    assertThat(noteRepository.findById(noteId).orElseThrow().getId(), is(noteId));

    byte[] localProposal =
        proposalBundleBytes(
            afterFirstWebSave, List.of(new NotebookGitProposalFile(NOTE_PATH, LOCAL_CONTENT)));
    ObjectId localHead;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      localHead = GitBundleTestReader.fetchHead(proposal, localProposal);
    }

    String publishedHead =
        controller.publishNotebookGitProposal(
            notebook.getId(), firstWebHead.getName(), localProposal);
    assertThat(publishedHead, is(localHead.getName()));
    assertThat(noteRepository.findById(noteId).orElseThrow().getId(), is(noteId));

    Note reloadedAfterPublication = noteRepository.findById(noteId).orElseThrow();
    NoteRealm secondWebSave =
        textContentController.updateNoteContent(
            reloadedAfterPublication, contentDto(SECOND_WEB_CONTENT));
    assertThat(secondWebSave.getId(), is(noteId));
    assertThat(noteRepository.findById(noteId).orElseThrow().getId(), is(noteId));

    byte[] downloaded =
        controller
            .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
            .getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId secondWebHead = GitBundleTestReader.fetchHead(repository, downloaded);
      assertThat(secondWebHead.getName(), is(binding(notebook).getAcceptedGitObjectId()));

      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit secondWebCommit = revWalk.parseCommit(secondWebHead);
        RevCommit localCommit = revWalk.parseCommit(localHead);
        RevCommit firstWebCommit = revWalk.parseCommit(firstWebHead);
        RevCommit acceptedCommit = revWalk.parseCommit(acceptedHead);

        assertThat(secondWebCommit.getParentCount(), is(1));
        assertThat(secondWebCommit.getParent(0).getId(), equalTo(localHead));
        assertThat(localCommit.getParentCount(), is(1));
        assertThat(localCommit.getParent(0).getId(), equalTo(firstWebHead));
        assertThat(firstWebCommit.getParentCount(), is(1));
        assertThat(firstWebCommit.getParent(0).getId(), equalTo(acceptedHead));
        assertThat(acceptedCommit.getParentCount(), is(0));
      }

      assertThat(
          NotebookGitProposalBlobText.readUtf8(repository, acceptedHead, NOTE_PATH),
          is(ACCEPTED_CONTENT));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repository, firstWebHead, NOTE_PATH),
          is(FIRST_WEB_CONTENT));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repository, localHead, NOTE_PATH),
          is(LOCAL_CONTENT));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repository, secondWebHead, NOTE_PATH),
          is(SECOND_WEB_CONTENT));
    }
  }
}
