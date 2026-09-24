package com.odde.donut.controllers;

import static com.odde.donut.services.notebookAttachment.VerifiedNotebookAttachmentBytes.sha256Hex;
import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

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
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookGit.NotebookGitLfsPointer;
import com.odde.donut.services.notebookGit.NotebookGitTreeContent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
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
  @Autowired NotebookAttachmentContent notebookAttachmentContent;

  NotebookGitBinding binding(Notebook notebook) {
    return notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
  }

  /** Seeds an attachment as the product stores it: payload in the content store, pointer row. */
  NotebookAttachment storeFolderAttachmentAndSnapshot(
      Notebook notebook, Folder folder, String filename, byte[] payload) throws IOException {
    return storeLegacyRawAttachmentAndSnapshot(
        notebook, folder, filename, pointerFor(notebook, payload));
  }

  /** Seeds exact accepted bytes; only for raw-subject tests on a legacy raw notebook. */
  NotebookAttachment storeLegacyRawAttachmentAndSnapshot(
      Notebook notebook, Folder folder, String filename, byte[] content) {
    NotebookAttachment attachment = new NotebookAttachment();
    attachment.setNotebook(notebook);
    attachment.setFolder(folder);
    attachment.setFilename(filename);
    attachment.setAcceptedGitContent(content);
    notebookAttachmentRepository.save(attachment);
    snapshotCurrentPortableTree(notebook);
    return attachment;
  }

  /** Stores {@code payload} in the notebook's content store and returns its LFS pointer. */
  byte[] pointerFor(Notebook notebook, byte[] payload) throws IOException {
    String oid = sha256Hex(payload);
    assertThat(
        notebookAttachmentContent.store(
            notebook.getId(), oid, payload.length, new ByteArrayInputStream(payload)),
        is(true));
    return NotebookGitLfsPointer.format(oid, payload.length);
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
    return NotebookGitTreeContent.of(acceptedHistory(notebook).tipContent()).blobIds();
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

  static List<String> portablePaths(InMemoryRepository repository, ObjectId head) throws Exception {
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
}
