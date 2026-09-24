package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.NotebookProjectionChange;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.ProjectionRow;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Note and attachment rows of a projection change: previous paths to clear on the accepted tree,
 * and current note and attachment blobs to put after folder relocation.
 */
final class NotebookGitChangedFiles {
  private final NoteRepository noteRepository;
  private final NotebookAttachmentRepository notebookAttachmentRepository;
  private final NotebookProjectionChange change;

  NotebookGitChangedFiles(
      NoteRepository noteRepository,
      NotebookAttachmentRepository notebookAttachmentRepository,
      NotebookProjectionChange change) {
    this.noteRepository = noteRepository;
    this.notebookAttachmentRepository = notebookAttachmentRepository;
    this.change = change;
  }

  /**
   * Removes deleted files and updated files whose path moved; records touched directories from each
   * previous path.
   */
  void forgetStalePaths(
      NotebookGitDirectoryTree tree,
      NotebookGitChangedFolders folders,
      Set<String> touchedDirectories) {
    change.deleted.entrySet().stream()
        .filter(entry -> isFile(entry.getKey().kind()))
        .map(entry -> folders.previousPathOf(entry.getKey().kind(), entry.getValue()))
        .forEach(
            previousPath -> {
              tree.removeFile(previousPath);
              folders
                  .currentPathOf(NotebookGitPortablePath.directoryOf(previousPath))
                  .ifPresent(touchedDirectories::add);
            });
    change.updated.entrySet().stream()
        .filter(entry -> isFile(entry.getKey().kind()))
        .forEach(
            entry -> {
              String previousPath = folders.previousPathOf(entry.getKey().kind(), entry.getValue());
              if (!previousPath.equals(currentFilePath(entry.getKey()))) {
                tree.removeFile(previousPath);
              }
              folders
                  .currentPathOf(NotebookGitPortablePath.directoryOf(previousPath))
                  .ifPresent(touchedDirectories::add);
            });
  }

  /** The current file of every note and attachment the change updated or inserted. */
  List<PortableTreeEntry> currentEntries() {
    return Stream.concat(
            change.updated.keySet().stream().filter(row -> isFile(row.kind())).map(this::stored),
            change.inserted.stream().filter(row -> isFile(row.getClass())))
        .map(NotebookGitChangedFiles::currentEntry)
        .toList();
  }

  private String currentFilePath(ProjectionRow row) {
    return currentEntry(stored(row)).path();
  }

  /** The stored note or attachment of a file row. */
  private Object stored(ProjectionRow row) {
    return row.kind() == Note.class
        ? noteRepository.findById(row.id()).orElseThrow()
        : notebookAttachmentRepository.findById(row.id()).orElseThrow();
  }

  /** A note's Markdown or an attachment's accepted Git content at its current path. */
  private static PortableTreeEntry currentEntry(Object file) {
    if (file instanceof Note note) {
      return PortableTreeEntry.ofNote(NotebookGitPortablePath.ofNote(note), note.getContent());
    }
    NotebookAttachment attachment = (NotebookAttachment) file;
    return new PortableTreeEntry(
        NotebookGitPortablePath.ofAttachment(attachment), attachment.getAcceptedGitContent());
  }

  private static boolean isFile(Class<?> kind) {
    return kind != Folder.class && kind != Notebook.class;
  }
}
