package com.odde.donut.services.notebookExport;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import java.util.List;

/**
 * Fetches a notebook's folders and stored notes, trash included, as the flat {@link
 * ExportFolderRow} / {@link ExportNoteRow} shapes shared by every consumer that builds a
 * Portable-tree snapshot (ZIP export, Git cutover, ...).
 */
public final class NotebookExportRows {

  private NotebookExportRows() {}

  public static List<ExportFolderRow> folders(
      FolderRepository folderRepository, Notebook notebook) {
    return folderRepository.findExportRowsByNotebookId(notebook.getId());
  }

  public static List<ExportNoteRow> notes(NoteRepository noteRepository, Notebook notebook) {
    return noteRepository.findExportRowsByNotebookId(notebook.getId());
  }

  public static List<ExportNoteRow> notes(List<Note> notes) {
    return notes.stream()
        .map(
            n ->
                new ExportNoteRow(
                    n.getFolder() == null ? null : n.getFolder().getId(),
                    n.getTitle(),
                    n.getContent()))
        .toList();
  }
}
