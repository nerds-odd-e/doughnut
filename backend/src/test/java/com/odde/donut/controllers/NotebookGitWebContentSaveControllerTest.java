package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class NotebookGitWebContentSaveControllerTest extends NotebookGitBundleControllerTestBase {
  private static final String ACCEPTED_CONTENT = "---\ntype: Note\n---\naccepted content";
  private static final String EDITED_CONTENT = "---\ntype: Note\n---\nedited content";

  @Autowired TextContentController textContentController;
  @Autowired NoteController noteController;

  @Test
  void savesARootNoteAsOneAcceptedRevisionWithoutChangingItsLearningIdentity() throws Exception {
    Timestamp editedAt = Timestamp.from(Instant.parse("2026-09-06T03:04:05Z"));
    testabilitySettings.timeTravelTo(editedAt);
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("Root Note").content(ACCEPTED_CONTENT).please();
    MemoryTracker tracker =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
                    .difficulty(7f)
                    .please());
    NotebookGitBinding accepted = snapshotCurrentPortableTree(notebook);
    ObjectId acceptedHead = ObjectId.fromString(accepted.getAcceptedGitObjectId());

    NoteRealm response = textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT));

    Note reloaded = noteRepository.findById(note.getId()).orElseThrow();
    NoteRealm readBackNote = noteController.showNote(reloaded);
    assertThat(response.getId(), is(note.getId()));
    assertThat(readBackNote.getId(), is(note.getId()));
    assertThat(readBackNote.getNote().getContent(), is(EDITED_CONTENT));
    assertThat(reloaded.getUpdatedAt(), is(editedAt));
    assertThat(
        noteController.getNoteInfo(reloaded).getMemoryTrackers().getFirst().getId(),
        is(tracker.getId()));
    assertThat(
        noteController.getNoteInfo(reloaded).getMemoryTrackers().getFirst().getDifficulty(),
        is(7f));

    byte[] downloaded =
        controller
            .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
            .getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId editedHead = GitBundleTestReader.fetchHead(repository, downloaded);
      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit commit = revWalk.parseCommit(editedHead);
        assertThat(commit.getParentCount(), is(1));
        assertThat(commit.getParent(0).getId(), is(acceptedHead));
        assertThat(
            commit.getAuthorIdent().getName(), is(NotebookGitCutoverService.SYSTEM_AUTHOR_NAME));
        assertThat(
            commit.getAuthorIdent().getEmailAddress(),
            is(NotebookGitCutoverService.SYSTEM_AUTHOR_EMAIL));
        assertThat(commit.getCommitterIdent(), is(commit.getAuthorIdent()));
        assertThat(commit.getFullMessage(), is("Edit note content: Root Note"));
        assertThat(commit.getCommitTime(), is((int) editedAt.toInstant().getEpochSecond()));
        assertThat(revWalk.parseCommit(acceptedHead).getId(), is(acceptedHead));
      }
      assertThat(portablePaths(repository, editedHead), contains("Root Note.md"));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repository, editedHead, "Root Note.md"),
          is(EDITED_CONTENT));
      NotebookGitBinding storedBinding = binding(notebook);
      assertThat(storedBinding.getAcceptedGitObjectId(), is(editedHead.getName()));
      assertThat(storedBinding.getUpdatedAt(), is(editedAt));
      assertThat(storedBinding.getBundleBytes(), equalTo(downloaded));
    }
  }

  @Test
  void preExistingPortableDriftKeepsTheWebSaveAndAcceptedHistoryUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("Root Note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding accepted = snapshotCurrentPortableTree(notebook);
    makeMe.aNote().notebook(notebook).title("Unsynchronized").content(ACCEPTED_CONTENT).please();

    textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT));

    NotebookGitBinding after = binding(notebook);
    assertThat(
        noteRepository.findById(note.getId()).orElseThrow().getContent(), is(EDITED_CONTENT));
    assertThat(after.getAcceptedGitObjectId(), is(accepted.getAcceptedGitObjectId()));
    assertThat(after.getBundleBytes(), equalTo(accepted.getBundleBytes()));
  }

  @Test
  void missingBindingKeepsTheExistingWebSave() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("Root Note").content(ACCEPTED_CONTENT).please();
    notebookGitBindingRepository.delete(binding(notebook));

    textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT));

    assertThat(
        noteRepository.findById(note.getId()).orElseThrow().getContent(), is(EDITED_CONTENT));
    assertThat(
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).isEmpty(), is(true));
  }

  @Test
  void deniedAndInvalidSavesDoNotAdvanceAcceptedHistory() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("Root Note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding accepted = snapshotCurrentPortableTree(notebook);
    User owner = currentUser.getUser();

    currentUser.setUser(createFixtureUser());
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT)));
    currentUser.setUser(owner);
    assertThrows(
        ApiException.class,
        () ->
            textContentController.updateNoteContent(
                note, contentDto("---\ntype: Note\naliases: invalid\n---\nbody")));

    NotebookGitBinding after = binding(notebook);
    assertThat(after.getAcceptedGitObjectId(), is(accepted.getAcceptedGitObjectId()));
    assertThat(after.getBundleBytes(), equalTo(accepted.getBundleBytes()));
    assertThat(
        noteRepository.findById(note.getId()).orElseThrow().getContent(), is(ACCEPTED_CONTENT));
  }

  @Test
  void corruptStoredAcceptedBundleFailsLoudlyWithoutSavingTheNote() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note =
        makeMe.aNote().notebook(notebook).title("Root Note").content(ACCEPTED_CONTENT).please();
    NotebookGitBinding accepted = snapshotCurrentPortableTree(notebook);
    byte[] corruptBundle = Arrays.copyOf(accepted.getBundleBytes(), 12);
    accepted.setBundleBytes(corruptBundle);
    notebookGitBindingRepository.save(accepted);

    RuntimeException failure =
        assertThrows(
            RuntimeException.class,
            () -> textContentController.updateNoteContent(note, contentDto(EDITED_CONTENT)));

    assertThat(failure instanceof ResponseStatusException, is(false));
    assertThat(
        noteRepository.findById(note.getId()).orElseThrow().getContent(), is(ACCEPTED_CONTENT));
    assertThat(binding(notebook).getBundleBytes(), equalTo(corruptBundle));
  }

  private NotebookGitBinding binding(Notebook notebook) {
    return notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
  }

  private static NoteUpdateContentDTO contentDto(String content) {
    NoteUpdateContentDTO dto = new NoteUpdateContentDTO();
    dto.setContent(content);
    return dto;
  }

  private static List<String> portablePaths(InMemoryRepository repository, ObjectId head)
      throws Exception {
    try (RevWalk revWalk = new RevWalk(repository);
        TreeWalk treeWalk = new TreeWalk(repository)) {
      treeWalk.addTree(revWalk.parseCommit(head).getTree());
      treeWalk.setRecursive(true);
      List<String> paths = new ArrayList<>();
      while (treeWalk.next()) {
        paths.add(treeWalk.getPathString());
      }
      return paths;
    }
  }
}
