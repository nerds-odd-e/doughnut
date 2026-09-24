package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthoredNoteDocumentPersistence;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import java.io.IOException;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * A picture uploaded for a note's {@code image} property becomes a file in the note's folder under
 * its uploaded name. Its bytes are stored first; the pointer and the note's {@code image:} are then
 * accepted together as one web change.
 */
@Service
public class WebNoteImageUploadService {
  private final WebNoteEditService webNoteEditService;
  private final NotebookGitBindingRepository bindingRepository;
  private final NotebookAttachmentContent attachmentContent;
  private final NotebookAttachmentRepository attachmentRepository;
  private final AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence;
  private final CanonicalDonutOrigin canonicalDonutOrigin;

  public WebNoteImageUploadService(
      WebNoteEditService webNoteEditService,
      NotebookGitBindingRepository bindingRepository,
      NotebookAttachmentContent attachmentContent,
      NotebookAttachmentRepository attachmentRepository,
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence,
      CanonicalDonutOrigin canonicalDonutOrigin) {
    this.webNoteEditService = webNoteEditService;
    this.bindingRepository = bindingRepository;
    this.attachmentContent = attachmentContent;
    this.attachmentRepository = attachmentRepository;
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
  }

  /** Returns the filename written into the note's {@code image:}. */
  public String upload(Note note, MultipartFile picture, Timestamp updatedAt)
      throws IOException, UnexpectedNoAccessRightException {
    Integer notebookId = note.getNotebook().getId();
    requireLfs(notebookId);
    String filename = picture.getOriginalFilename();
    byte[] pointer = attachmentContent.storeAsLfsPointer(notebookId, picture.getBytes());
    webNoteEditService.edit(
        note.getId(),
        notebookId,
        editing -> {
          addFile(editing, filename, pointer);
          authoredNoteDocumentPersistence.persist(
              editing,
              AuthoredNoteDocument.fromContent(
                  NoteContentMarkdown.withNoteImage(editing.getContent(), filename),
                  canonicalDonutOrigin),
              updatedAt);
        },
        editing -> "Upload note image: " + filename,
        updatedAt);
    return filename;
  }

  private void requireLfs(Integer notebookId) {
    if (!bindingRepository.storesAttachmentsAsLfs(notebookId)) {
      throw new IllegalStateException(
          "Notebook " + notebookId + " does not store its files as Git LFS");
    }
  }

  private void addFile(Note note, String filename, byte[] pointer) {
    NotebookAttachment attachment = new NotebookAttachment();
    attachment.setNotebook(note.getNotebook());
    attachment.setFolder(note.getFolder());
    attachment.setFilename(filename);
    attachment.setAcceptedGitContent(pointer);
    attachmentRepository.save(attachment);
  }
}
