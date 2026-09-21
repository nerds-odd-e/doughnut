package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.util.Comparator;
import java.util.List;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Reads what a notebook's live projection actually holds after a controller has committed: its root
 * Attachments and its surviving notes. Each read runs in its own committed transaction, so it sees
 * the controller's own writes rather than the test's uncommitted view.
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
}
