package com.odde.donut.services.notebookTree;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Builds a notebook's current Portable tree from its stored folders, notes (trash included) and
 * attachments. Every full assembly of live Portable content — Git cutover, history reset,
 * publication drift detection — reads the tree from here, so what a notebook's Portable tree
 * contains is decided in one place; accepted web changes derive their tree from the accepted head
 * instead.
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
        folderRepository.findPortableTreeRowsByNotebookId(notebook.getId()),
        noteRepository.findPortableTreeRowsByNotebookId(notebook.getId()));
  }

  /** The same tree from folders and notes a caller already holds, as a locked publication does. */
  public List<PortableTreeEntry> entriesOf(
      Notebook notebook, List<PortableTreeFolderRow> folders, List<Note> storedNotes) {
    return entries(notebook, folders, noteRows(storedNotes));
  }

  private List<PortableTreeEntry> entries(
      Notebook notebook, List<PortableTreeFolderRow> folders, List<PortableTreeNoteRow> notes) {
    return PortableTreeSnapshot.build(
        notebook.getReadmeContent(),
        folders,
        notes,
        notebookAttachmentRepository.findPortableTreeRowsByNotebookId(notebook.getId()));
  }

  private static List<PortableTreeNoteRow> noteRows(List<Note> notes) {
    return notes.stream()
        .map(
            note ->
                new PortableTreeNoteRow(
                    note.getFolder() == null ? null : note.getFolder().getId(),
                    note.getTitle(),
                    note.getContent()))
        .toList();
  }
}
