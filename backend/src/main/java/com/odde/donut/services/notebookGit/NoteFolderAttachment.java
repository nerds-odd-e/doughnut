package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * The notebook file a note refers to by a path relative to the note's own folder. The path is taken
 * literally: the file whose portable path equals the note's folder prefix plus the path.
 */
@Service
public class NoteFolderAttachment {
  private final NotebookAttachmentRepository notebookAttachmentRepository;

  public NoteFolderAttachment(NotebookAttachmentRepository notebookAttachmentRepository) {
    this.notebookAttachmentRepository = notebookAttachmentRepository;
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
}
