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
  private static final String CREATED_PREFIX =
      "---\r\ntype: Note\r\n# Author annotation\r\ncustom:\r\n  source: 'local'\r\n---\r\n";
  private static final String CREATED_CONTENT = CREATED_PREFIX + "created locally";
  private static final String FIRST_WEB_CONTENT = "---\ntype: Note\n---\nfirst web edit";
  private static final String LOCAL_CONTENT = "---\ntype: Note\n---\nlocal edit";
  private static final String SECOND_WEB_CONTENT = "---\ntype: Note\n---\nsecond web edit";

  @Test
  void savesAWebEditOnTheSameLocallyCreatedNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    String createdHead = publishLocalCreation(notebook);
    Note createdNote =
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()).getFirst();

    String editedContent = CREATED_PREFIX + "first web edit";
    NoteRealm saved =
        textContentController.updateNoteContent(createdNote, contentDto(editedContent));

    assertThat(saved.getId(), is(createdNote.getId()));
    byte[] downloaded =
        controller
            .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
            .getBody();
    try (InMemoryRepository accepted = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId webEditHead = GitBundleTestReader.fetchHead(accepted, downloaded);
      try (RevWalk revWalk = new RevWalk(accepted)) {
        RevCommit webEditCommit = revWalk.parseCommit(webEditHead);
        assertThat(webEditCommit.getParent(0).getId().getName(), is(createdHead));
      }
      assertThat(
          NotebookGitProposalBlobText.readUtf8(accepted, webEditHead, NOTE_PATH),
          is(editedContent));
    }
  }

  @Test
  void publishesALaterLocalEditOnTheSameCreatedNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    String createdHead = publishLocalCreation(notebook);
    Integer createdNoteId =
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()).getFirst().getId();

    NotebookGitBinding afterCreation = binding(notebook);
    byte[] editProposal =
        proposalBundleBytes(
            afterCreation, List.of(new NotebookGitProposalFile(NOTE_PATH, LOCAL_CONTENT)));
    ObjectId localEditHead;
    try (InMemoryRepository proposal = new InMemoryRepository(new DfsRepositoryDescription())) {
      localEditHead = GitBundleTestReader.fetchHead(proposal, editProposal);
    }

    controller.publishNotebookGitProposal(
        notebook.getId(), afterCreation.getAcceptedGitObjectId(), editProposal);

    Note editedNote =
        noteRepository.findLiveNotesByNotebookIdOrderByIdAsc(notebook.getId()).getFirst();
    NotebookGitBinding afterEdit = binding(notebook);
    assertThat(editedNote.getId(), is(createdNoteId));
    assertThat(editedNote.getContent(), is(LOCAL_CONTENT));
    assertThat(afterEdit.getAcceptedGitObjectId(), is(localEditHead.getName()));

    try (InMemoryRepository accepted = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId acceptedHead = GitBundleTestReader.fetchHead(accepted, afterEdit.getBundleBytes());
      try (RevWalk revWalk = new RevWalk(accepted)) {
        RevCommit acceptedCommit = revWalk.parseCommit(acceptedHead);
        assertThat(acceptedCommit.getParent(0).getId().getName(), is(createdHead));
      }
      assertThat(
          NotebookGitProposalBlobText.readUtf8(accepted, acceptedHead, NOTE_PATH),
          is(LOCAL_CONTENT));
    }
  }

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

  private String publishLocalCreation(Notebook notebook) throws Exception {
    NotebookGitBinding initialBinding = binding(notebook);
    byte[] creationProposal =
        proposalBundleBytes(
            initialBinding, List.of(new NotebookGitProposalFile(NOTE_PATH, CREATED_CONTENT)));
    return controller.publishNotebookGitProposal(
        notebook.getId(), initialBinding.getAcceptedGitObjectId(), creationProposal);
  }
}
