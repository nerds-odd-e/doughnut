package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.RecallLog;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.entities.repositories.RecallLogRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookGit.NotebookGitCutoverService;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import java.sql.Timestamp;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotebookGitWebContentHistoryControllerTest
    extends NotebookGitWebContentHistoryControllerTestSupport {

  @Autowired RecallLogRepository recallLogRepository;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;

  private static final String NOTE_PATH = "Root Note.md";
  private static final String CONTENT_1000 = "---\ntype: Note\n---\ncontent at 10:00";
  private static final String CONTENT_1008 = "---\ntype: Note\n---\ncontent at 10:08";
  private static final String REFERENCE_CONTENT = content("See [[Target]].");

  @Test
  void unchangedTitleKeepsAcceptedHistoryWhileRetainingTimestampSemantics() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note note = makeMe.aNote().notebook(notebook).title("Original").please();
    NotebookGitBinding accepted = snapshotCurrentPortableTree(notebook);
    String acceptedHead = accepted.getAcceptedGitObjectId();
    byte[] acceptedBundle = accepted.getBundleBytes().clone();
    testabilitySettings.timeTravelTo(Timestamp.from(T1008));

    textContentController.updateNoteTitle(note, titleDto("Original"));

    assertAcceptedHistoryUnchanged(notebook.getId(), acceptedHead, acceptedBundle);
    assertThat(
        noteRepository.findById(note.getId()).orElseThrow().getUpdatedAt(),
        is(Timestamp.from(T1008)));
  }

  @Test
  void missingReferenceChoiceKeepsAcceptedHistoryAndStoredNotesUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note target = makeMe.aNote().notebook(notebook).title("Target").please();
    Note referrer = makeMe.aNote().notebook(notebook).please();
    textContentController.updateNoteContent(referrer, contentDto(REFERENCE_CONTENT));
    NotebookGitBinding accepted = snapshotCurrentPortableTree(notebook);
    String acceptedHead = accepted.getAcceptedGitObjectId();
    byte[] acceptedBundle = accepted.getBundleBytes().clone();

    ApiException thrown =
        assertThrows(
            ApiException.class,
            () -> textContentController.updateNoteTitle(target, titleDto("Renamed")));

    assertThat(thrown.getErrorBody().getErrorType(), is(ApiError.ErrorType.BINDING_ERROR));
    assertAcceptedHistoryUnchanged(notebook.getId(), acceptedHead, acceptedBundle);
    assertThat(noteRepository.findById(target.getId()).orElseThrow().getTitle(), is("Target"));
    assertThat(
        noteRepository.findById(referrer.getId()).orElseThrow().getContent(),
        is(REFERENCE_CONTENT));
  }

  @Test
  void deniedOwnerKeepsAcceptedHistoryAndStoredNotesUnchanged() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Note target = makeMe.aNote().notebook(notebook).title("Target").please();
    Note referrer = makeMe.aNote().notebook(notebook).please();
    textContentController.updateNoteContent(referrer, contentDto(REFERENCE_CONTENT));
    NotebookGitBinding accepted = snapshotCurrentPortableTree(notebook);
    String acceptedHead = accepted.getAcceptedGitObjectId();
    byte[] acceptedBundle = accepted.getBundleBytes().clone();
    currentUser.setUser(createFixtureUser());

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> textContentController.updateNoteTitle(target, titleDto("Renamed")));

    assertAcceptedHistoryUnchanged(notebook.getId(), acceptedHead, acceptedBundle);
    assertThat(noteRepository.findById(target.getId()).orElseThrow().getTitle(), is("Target"));
    assertThat(
        noteRepository.findById(referrer.getId()).orElseThrow().getContent(),
        is(REFERENCE_CONTENT));
  }

  @Test
  void webRenameAppendsTheRenamedPortableTreeAndPreservesLearningIdentity() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder biology = makeMe.aFolder().notebook(notebook).name("Biology").please();
    String authoredContent = "---\ntype: Note\nauthor: Linnaeus\n---\n# Cells\nMembranes";
    Note note = makeMe.aNote().folder(biology).title("Cells").content(authoredContent).please();
    int[] learningIds =
        inCommittedTransaction(
            transactionManager,
            () -> {
              MemoryTracker tracker =
                  makeMe
                      .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
                      .recallCount(2)
                      .please();
              RecallLog log = makeMe.aRecallLogFor(tracker).please();
              return new int[] {tracker.getId(), log.getId()};
            });
    int recallCountBeforeRename =
        memoryTrackerRepository.findById(learningIds[0]).orElseThrow().getRecallCount();
    String recallOutcomeBeforeRename =
        recallLogRepository.findById(learningIds[1]).orElseThrow().getProductOutcome();
    NotebookGitBinding accepted = snapshotCurrentPortableTree(notebook);
    ObjectId preRenameParent = ObjectId.fromString(accepted.getAcceptedGitObjectId());
    NoteUpdateTitleDTO title = new NoteUpdateTitleDTO();
    title.setNewTitle("Cell structure");

    NoteRealm response =
        inCommittedTransaction(
            transactionManager,
            () ->
                assertDoesNotThrow(
                    () ->
                        textContentController.updateNoteTitle(
                            noteRepository.findById(note.getId()).orElseThrow(), title)));

    Note reloaded = noteRepository.findById(note.getId()).orElseThrow();
    assertThat(response.getNote().getId(), is(note.getId()));
    assertThat(reloaded.getTitle(), is("Cell structure"));
    MemoryTracker reloadedTracker = memoryTrackerRepository.findById(learningIds[0]).orElseThrow();
    assertThat(reloadedTracker.getNote().getId(), is(note.getId()));
    assertThat(reloadedTracker.getRecallCount(), is(recallCountBeforeRename));
    RecallLog reloadedLog = recallLogRepository.findById(learningIds[1]).orElseThrow();
    assertThat(reloadedLog.getMemoryTracker().getId(), is(learningIds[0]));
    assertThat(reloadedLog.getProductOutcome(), is(recallOutcomeBeforeRename));

    byte[] downloadedBundle =
        controller
            .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
            .getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId tip = GitBundleTestReader.fetchHead(repository, downloadedBundle);
      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit renamed = revWalk.parseCommit(tip);
        assertThat(
            NotebookGitProposalBlobText.readUtf8(repository, renamed, "Biology/Cell structure.md"),
            is(authoredContent));
        assertThat(
            renamed.getId(), is(ObjectId.fromString(binding(notebook).getAcceptedGitObjectId())));
        assertThat(renamed.getParent(0).getId(), is(preRenameParent));
        assertThat(
            portablePaths(repository, renamed), equalTo(List.of("Biology/Cell structure.md")));
      }
    }
  }

  @Test
  void rapidSameNoteSavesAppendEachRevisionWithContent() throws Exception {
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
    ObjectId preEditParent = ObjectId.fromString(accepted.getAcceptedGitObjectId());

    saveAt(note.getId(), CONTENT_1000, T1000);
    ObjectId firstEdit = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());
    saveAt(note.getId(), CONTENT_1008, T1008);
    ObjectId secondEdit = ObjectId.fromString(binding(notebook).getAcceptedGitObjectId());

    Note reloaded = noteRepository.findById(note.getId()).orElseThrow();
    assertThat(reloaded.getContent(), is(CONTENT_1008));
    assertThat(reloaded.getUpdatedAt(), is(Timestamp.from(T1008)));
    assertThat(
        noteController.getNoteInfo(reloaded).getMemoryTrackers().getFirst().getId(),
        is(tracker.getId()));
    assertThat(
        noteController.getNoteInfo(reloaded).getMemoryTrackers().getFirst().getDifficulty(),
        is(7f));

    History history = downloadHistory(notebook, NOTE_PATH);
    assertThat(
        history.contentsNewestFirst(),
        equalTo(List.of(CONTENT_1008, CONTENT_1000, ACCEPTED_CONTENT)));
    assertThat(history.headsNewestFirst(), equalTo(List.of(secondEdit, firstEdit, preEditParent)));
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      ObjectId tip = GitBundleTestReader.fetchHead(repository, history.bundleBytes());
      try (RevWalk revWalk = new RevWalk(repository)) {
        RevCommit commit = revWalk.parseCommit(tip);
        assertThat(commit.getParentCount(), is(1));
        assertThat(commit.getParent(0).getId(), equalTo(firstEdit));
        assertThat(
            commit.getAuthorIdent().getName(), is(NotebookGitCutoverService.SYSTEM_AUTHOR_NAME));
        assertThat(
            commit.getAuthorIdent().getEmailAddress(),
            is(NotebookGitCutoverService.SYSTEM_AUTHOR_EMAIL));
        assertThat(commit.getAuthorIdent().getWhenAsInstant(), equalTo(T1008));
        assertThat(commit.getCommitterIdent(), equalTo(commit.getAuthorIdent()));
        assertThat(commit.getFullMessage(), is("Edit note content: Root Note"));
      }
    }
  }

  private static NoteUpdateTitleDTO titleDto(String title) {
    NoteUpdateTitleDTO dto = new NoteUpdateTitleDTO();
    dto.setNewTitle(title);
    return dto;
  }

  private void assertAcceptedHistoryUnchanged(
      Integer notebookId, String acceptedHead, byte[] acceptedBundle) {
    NotebookGitBinding reloaded = bindingById(notebookId);
    assertThat(reloaded.getAcceptedGitObjectId(), is(acceptedHead));
    assertThat(reloaded.getBundleBytes(), is(acceptedBundle));
  }
}
