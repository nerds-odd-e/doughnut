package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.controllers.dto.NoteTopology;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * The notebook file a note refers to by a path relative to the note's own folder. The path is taken
 * literally: the file whose portable path equals the note's folder prefix plus the path.
 */
@Service
public class NoteFolderAttachment {
  private final NotebookAttachmentRepository notebookAttachmentRepository;
  private final NoteRepository noteRepository;

  public NoteFolderAttachment(
      NotebookAttachmentRepository notebookAttachmentRepository, NoteRepository noteRepository) {
    this.notebookAttachmentRepository = notebookAttachmentRepository;
    this.noteRepository = noteRepository;
  }

  public Optional<NotebookAttachment> at(Note note, String relativePath) {
    String target = NotebookGitPortablePath.folderPath(note.getFolder()) + relativePath;
    String directory = NotebookGitPortablePath.directoryOf(target);
    return notebookAttachmentRepository
        .findPlacementsByNotebookIdAndFilename(
            note.getNotebook().getId(), target.substring(directory.length()))
        .stream()
        .filter(
            placement -> NotebookGitPortablePath.folderPath(placement.folder()).equals(directory))
        .findFirst()
        .flatMap(placement -> notebookAttachmentRepository.findById(placement.id()));
  }

  /**
   * Available notes in the notebook whose {@code image:} resolves to that file, ordered by note id.
   */
  public List<NoteTopology> referencingNotes(NotebookAttachment attachment) {
    return noteRepository
        .findAvailableNotesByNotebookIdAndContentContaining(
            attachment.getNotebook().getId(), attachment.getFilename())
        .stream()
        .filter(note -> references(note, attachment))
        .map(Note::getNoteTopology)
        .toList();
  }

  private boolean references(Note note, NotebookAttachment attachment) {
    return NoteContentMarkdown.noteImage(note.getContent())
        .flatMap(image -> at(note, image))
        .filter(found -> found.getId().equals(attachment.getId()))
        .isPresent();
  }
}
