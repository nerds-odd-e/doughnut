package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import jakarta.persistence.EntityManager;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Reads what a notebook's live projection actually holds after a controller has committed. Database
 * reads that need isolation run in their own committed transaction, so they see the controller's
 * writes rather than the test's uncommitted view.
 */
final class NotebookLiveProjectionTestReader {

  private NotebookLiveProjectionTestReader() {}

  /** Sorted by filename, because the projection's own row order is insertion order. */
  static List<PortableTreeEntry> rootAttachments(
      PlatformTransactionManager transactionManager,
      NotebookAttachmentRepository notebookAttachmentRepository,
      Integer notebookId) {
    return inCommittedTransaction(
        transactionManager,
        () ->
            notebookAttachmentRepository.findPortableTreeRowsByNotebookId(notebookId).stream()
                .map(row -> new PortableTreeEntry(row.filename(), row.content()))
                .sorted(Comparator.comparing(PortableTreeEntry::path))
                .toList());
  }

  static List<PortableTreeEntry> attachmentTree(
      PlatformTransactionManager transactionManager,
      NotebookAttachmentRepository notebookAttachmentRepository,
      FolderRepository folderRepository,
      Integer notebookId) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          Map<Integer, Folder> folders = foldersById(folderRepository, notebookId);
          return notebookAttachmentRepository.findPortableTreeRowsByNotebookId(notebookId).stream()
              .map(
                  row ->
                      new PortableTreeEntry(
                          path(folders, row.folderId(), row.filename()), row.content()))
              .sorted(Comparator.comparing(PortableTreeEntry::path))
              .toList();
        });
  }

  static List<String> folderPaths(
      PlatformTransactionManager transactionManager,
      FolderRepository folderRepository,
      Integer notebookId) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          Map<Integer, Folder> folders = foldersById(folderRepository, notebookId);
          return folders.keySet().stream()
              .map(folderId -> path(folders, folderId, ""))
              .map(folderPath -> folderPath.substring(0, folderPath.length() - 1))
              .sorted()
              .toList();
        });
  }

  static List<String> noteTitles(
      PlatformTransactionManager transactionManager,
      NoteRepository noteRepository,
      Integer notebookId) {
    return inCommittedTransaction(
        transactionManager,
        () ->
            noteRepository.findAllByNotebookIdOrderByIdAsc(notebookId).stream()
                .map(Note::getTitle)
                .toList());
  }

  static long memoryTrackerCount(EntityManager entityManager, Integer notebookId) {
    return ((Number)
            entityManager
                .createNativeQuery(
                    "SELECT COUNT(*) FROM memory_tracker mt "
                        + "JOIN note n ON mt.note_id = n.id WHERE n.notebook_id = :notebookId")
                .setParameter("notebookId", notebookId)
                .getSingleResult())
        .longValue();
  }

  private static Map<Integer, Folder> foldersById(
      FolderRepository folderRepository, Integer notebookId) {
    return folderRepository.findByNotebookIdOrderByIdAsc(notebookId).stream()
        .collect(Collectors.toMap(Folder::getId, Function.identity()));
  }

  private static String path(Map<Integer, Folder> folders, Integer folderId, String filename) {
    if (folderId == null) {
      return filename;
    }
    Folder folder = folders.get(folderId);
    return path(folders, folder.getParentFolderId(), folder.getName() + "/" + filename);
  }
}
