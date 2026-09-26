package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.controllers.dto.FolderMoveRequest;
import com.odde.donut.controllers.dto.FolderRenameRequest;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteRecallInfo;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookAttachment.VerifiedNotebookAttachmentBytes;
import com.odde.donut.services.notebookGit.NotebookGitLfsPointer;
import com.odde.donut.services.notebookGit.NotebookGitTreeContent;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader.AcceptedHistory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.lib.ObjectId;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;

abstract class NotebookGitWebContentControllerTestBase extends NotebookGitControllerTestBase {
  static final String ACCEPTED_CONTENT = "---\ntype: Note\n---\naccepted content";
  static final String EDITED_CONTENT = "---\ntype: Note\n---\nedited content";

  @Autowired TextContentController textContentController;
  @Autowired NoteController noteController;
  @Autowired MemoryTrackerRepository memoryTrackerRepository;
  @Autowired NotebookAttachmentRepository notebookAttachmentRepository;

  NotebookGitBinding binding(Notebook notebook) {
    return notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
  }

  /** Seeds an attachment as the product stores it: payload in the content store, pointer row. */
  NotebookAttachment storeFolderAttachmentAndSnapshot(
      Notebook notebook, Folder folder, String filename, byte[] payload) throws IOException {
    NotebookAttachment attachment = new NotebookAttachment();
    attachment.setNotebook(notebook);
    attachment.setFolder(folder);
    attachment.setFilename(filename);
    attachment.setAcceptedGitContent(pointerFor(notebook, payload));
    notebookAttachmentRepository.save(attachment);
    snapshotCurrentPortableTree(notebook);
    return attachment;
  }

  /** The exact bytes at {@code path} in the notebook's accepted tip. */
  byte[] acceptedBytesAt(Notebook notebook, String path) throws Exception {
    return acceptedHistory(notebook).content().stream()
        .filter(entry -> entry.path().equals(path))
        .findFirst()
        .orElseThrow()
        .content();
  }

  static NoteUpdateContentDTO contentDto(String content) {
    NoteUpdateContentDTO dto = new NoteUpdateContentDTO();
    dto.setContent(content);
    return dto;
  }

  static FolderRenameRequest renameTo(String name) {
    FolderRenameRequest req = new FolderRenameRequest();
    req.setName(name);
    return req;
  }

  static FolderMoveRequest folderMove(Integer newParentFolderId) {
    FolderMoveRequest req = new FolderMoveRequest();
    req.setNewParentFolderId(newParentFolderId);
    return req;
  }

  static FolderMoveRequest folderMoveTo(Notebook destination, boolean merge) {
    FolderMoveRequest req = new FolderMoveRequest();
    req.setDestinationNotebookId(destination.getId());
    req.setMerge(merge);
    return req;
  }

  /**
   * The accepted head's tree, derived from the change, equals a full assembly of the stored
   * projection (the tree a history reset would produce), including native root tree id.
   */
  void assertAcceptedTreeMatchesTheFullAssembly(Notebook notebook) throws Exception {
    Map<String, ObjectId> derived = acceptedBlobIds(notebook);
    ObjectId derivedRoot = acceptedHistory(notebook).tipTreeId();
    snapshotCurrentPortableTree(notebookRepository.findById(notebook.getId()).orElseThrow());
    assertThat(derived, equalTo(acceptedBlobIds(notebook)));
    assertThat(derivedRoot, equalTo(acceptedHistory(notebook).tipTreeId()));
  }

  Map<String, ObjectId> acceptedBlobIds(Notebook notebook) throws Exception {
    return NotebookGitTreeContent.of(acceptedHistory(notebook).exactTree()).blobIds();
  }

  /** The queries {@code operation} alone executed. */
  List<String> queriesOf(Executable operation) throws Throwable {
    return List.of(hibernateStatisticsOf(operation).getQueries());
  }

  /** Hibernate statistics of {@code operation} alone; its counts and queries stay readable. */
  Statistics hibernateStatisticsOf(Executable operation) throws Throwable {
    Statistics statistics =
        entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    boolean previouslyEnabled = statistics.isStatisticsEnabled();
    statistics.setStatisticsEnabled(true);
    statistics.clear();
    try {
      operation.execute();
      return statistics;
    } finally {
      statistics.setStatisticsEnabled(previouslyEnabled);
    }
  }

  MemoryTracker learnedTracker(Note note, float difficulty) {
    return learnedTracker(note, difficulty, 0);
  }

  MemoryTracker learnedTracker(Note note, float difficulty, int recallCount) {
    return inCommittedTransaction(
        transactionManager,
        () ->
            makeMe
                .aMemoryTrackerFor(noteRepository.findById(note.getId()).orElseThrow())
                .difficulty(difficulty)
                .recallCount(recallCount)
                .please());
  }

  void assertShownContentAndRetainedLearning(
      Note original, MemoryTracker tracker, String expectedContent)
      throws UnexpectedNoAccessRightException {
    Note reloaded = noteRepository.findById(original.getId()).orElseThrow();
    NoteRealm view = noteController.showNote(reloaded);
    assertThat(view.getId(), equalTo(original.getId()));
    assertThat(view.getNote().getContent(), equalTo(expectedContent));
    NoteRecallInfo recallInfo = noteController.getNoteInfo(reloaded);
    assertThat(recallInfo.getMemoryTrackers(), hasSize(1));
    assertThat(recallInfo.getMemoryTrackers().getFirst().getId(), equalTo(tracker.getId()));
    MemoryTracker retained = memoryTrackerRepository.findById(tracker.getId()).orElseThrow();
    assertThat(retained.getDifficulty(), equalTo(tracker.getDifficulty()));
    assertThat(retained.getStability(), equalTo(tracker.getStability()));
    assertThat(retained.getLastRecalledAt(), equalTo(tracker.getLastRecalledAt()));
    assertThat(retained.getNextRecallAt(), equalTo(tracker.getNextRecallAt()));
    assertThat(retained.getAssimilatedAt(), equalTo(tracker.getAssimilatedAt()));
    assertThat(retained.getRemovedFromTracking(), equalTo(tracker.getRemovedFromTracking()));
    assertThat(retained.getType(), equalTo(tracker.getType()));
    assertThat(retained.getPropertyKey(), equalTo(tracker.getPropertyKey()));
  }

  long countRecallLogsByTrackerId(Integer trackerId) {
    return ((Number)
            entityManager
                .createNativeQuery("SELECT COUNT(*) FROM recall_log WHERE memory_tracker_id = :id")
                .setParameter("id", trackerId)
                .getSingleResult())
        .longValue();
  }

  /** The pointer for {@code bytes}, after checking the content store holds them exactly. */
  byte[] lfsPointerStoredFor(Notebook notebook, byte[] bytes) {
    String digest = VerifiedNotebookAttachmentBytes.sha256Hex(bytes);
    assertThat(
        notebookAttachmentContent.get(notebook.getId(), digest).orElseThrow(), equalTo(bytes));
    return NotebookGitLfsPointer.format(digest, bytes.length);
  }

  static byte[] tipContent(AcceptedHistory history, String path) {
    return history.content().stream()
        .filter(entry -> entry.path().equals(path))
        .map(PortableTreeEntry::content)
        .findFirst()
        .orElseThrow(() -> new AssertionError(path + " not in " + history.tipPaths()));
  }

  static String tipText(AcceptedHistory history, String path) {
    return new String(tipContent(history, path), StandardCharsets.UTF_8);
  }
}
