package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

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
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
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

  NotebookGitBinding storeFolderAttachmentAndSnapshot(
      Notebook notebook, Folder folder, String filename, byte[] content) {
    NotebookAttachment attachment = new NotebookAttachment();
    attachment.setNotebook(notebook);
    attachment.setFolder(folder);
    attachment.setFilename(filename);
    attachment.setContent(content);
    notebookAttachmentRepository.save(attachment);
    return snapshotCurrentPortableTree(notebook);
  }

  static NoteUpdateContentDTO contentDto(String content) {
    NoteUpdateContentDTO dto = new NoteUpdateContentDTO();
    dto.setContent(content);
    return dto;
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
