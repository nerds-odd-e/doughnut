package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
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
 * and current note blobs to put after folder relocation.
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

  /** The current file of every note the change updated or inserted. */
  List<PortableTreeEntry> currentNoteEntries() {
    Stream<Note> updated =
        change.updated.keySet().stream()
            .filter(row -> row.kind() == Note.class)
            .map(row -> noteRepository.findById(row.id()).orElseThrow());
    Stream<Note> inserted =
        change.inserted.stream().filter(Note.class::isInstance).map(Note.class::cast);
    return Stream.concat(updated, inserted)
        .map(
            note ->
                PortableTreeEntry.ofNote(NotebookGitPortablePath.ofNote(note), note.getContent()))
        .toList();
  }

  private String currentFilePath(ProjectionRow row) {
    if (row.kind() == Note.class) {
      return NotebookGitPortablePath.ofNote(noteRepository.findById(row.id()).orElseThrow());
    }
    return NotebookGitPortablePath.ofAttachment(
        notebookAttachmentRepository.findById(row.id()).orElseThrow());
  }

  private static boolean isFile(Class<?> kind) {
    return kind != Folder.class && kind != Notebook.class;
  }
}
