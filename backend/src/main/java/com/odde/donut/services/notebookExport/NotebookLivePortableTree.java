package com.odde.donut.services.notebookExport;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Builds a notebook's current Portable tree from its stored folders, notes (trash included) and
 * root attachments. Every consumer of live Portable content — ZIP export, Git cutover, accepted web
 * changes, projection-drift detection — reads the tree from here, so what a notebook's Portable
 * tree contains is decided in one place.
 */
@Service
public class NotebookLivePortableTree {
  private final FolderRepository folderRepository;
  private final NoteRepository noteRepository;
  private final NotebookAttachmentRepository notebookAttachmentRepository;

  public NotebookLivePortableTree(
      FolderRepository folderRepository,
      NoteRepository noteRepository,
      NotebookAttachmentRepository notebookAttachmentRepository) {
    this.folderRepository = folderRepository;
    this.noteRepository = noteRepository;
    this.notebookAttachmentRepository = notebookAttachmentRepository;
  }

  public List<PortableTreeEntry> entriesOf(Notebook notebook) {
    return entries(
        notebook,
        folderRepository.findExportRowsByNotebookId(notebook.getId()),
        noteRepository.findExportRowsByNotebookId(notebook.getId()));
  }

  /** The same tree from folders and notes a caller already holds, as a locked publication does. */
  public List<PortableTreeEntry> entriesOf(
      Notebook notebook, List<ExportFolderRow> folders, List<Note> storedNotes) {
    return entries(notebook, folders, noteRows(storedNotes));
  }

  private List<PortableTreeEntry> entries(
      Notebook notebook, List<ExportFolderRow> folders, List<ExportNoteRow> notes) {
    return PortableTreeSnapshot.build(
        notebook.getReadmeContent(),
        folders,
        notes,
        notebookAttachmentRepository.findExportRowsByNotebookId(notebook.getId()));
  }

  private static List<ExportNoteRow> noteRows(List<Note> notes) {
    return notes.stream()
        .map(
            note ->
                new ExportNoteRow(
                    note.getFolder() == null ? null : note.getFolder().getId(),
                    note.getTitle(),
                    note.getContent()))
        .toList();
  }
}
