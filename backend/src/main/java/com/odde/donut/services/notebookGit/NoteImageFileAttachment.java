package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.services.AuthoredNoteDocumentPersistence;
import com.odde.donut.validators.AuthoredNoteContent;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;

/**
 * Inside an accepted web change: a stored picture's pointer becomes a file in the note's folder,
 * and the note's {@code image:} names it, with the content prepared like any ordinary content save.
 */
@Service
public class NoteImageFileAttachment {
  private final NotebookAttachmentRepository attachmentRepository;
  private final AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence;
  private final CanonicalDonutOrigin canonicalDonutOrigin;

  public NoteImageFileAttachment(
      NotebookAttachmentRepository attachmentRepository,
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence,
      CanonicalDonutOrigin canonicalDonutOrigin) {
    this.attachmentRepository = attachmentRepository;
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
  }

  public void attach(Note note, String filename, byte[] pointer, Timestamp updatedAt) {
    NotebookAttachment attachment = new NotebookAttachment();
    attachment.setNotebook(note.getNotebook());
    attachment.setFolder(note.getFolder());
    attachment.setFilename(filename);
    attachment.setAcceptedGitContent(pointer);
    attachmentRepository.save(attachment);
    authoredNoteDocumentPersistence.persist(
        note,
        AuthoredNoteContent.prepareDocumentForSave(
            NoteContentMarkdown.withNoteImage(note.getContent(), filename), canonicalDonutOrigin),
        updatedAt);
  }
}
