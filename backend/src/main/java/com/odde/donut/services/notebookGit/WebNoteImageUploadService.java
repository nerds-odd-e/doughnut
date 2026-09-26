package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthoredNoteDocumentPersistence;
import com.odde.donut.services.FolderSiblingNameValidation;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookAttachment.PictureFile;
import com.odde.donut.validators.AuthoredNoteContent;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * A picture uploaded for a note's {@code image} property becomes a file in the note's folder under
 * its uploaded name. A name that is not a plain filename, is already taken there, or is not a
 * {@link PictureFile} within its limit is refused before anything is stored; nothing is renamed or
 * overwritten. Otherwise its bytes are stored first, then the pointer and the note's {@code image:}
 * are accepted together as one web change, with the content prepared like any ordinary content
 * save. A notebook without a Git binding refuses the upload.
 */
@Service
public class WebNoteImageUploadService {
  private final WebNoteEditService webNoteEditService;
  private final NotebookGitBindingRepository bindingRepository;
  private final NotebookAttachmentContent attachmentContent;
  private final NotebookAttachmentRepository attachmentRepository;
  private final AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence;
  private final CanonicalDonutOrigin canonicalDonutOrigin;
  private final FolderSiblingNameValidation folderSiblingNameValidation;

  public WebNoteImageUploadService(
      WebNoteEditService webNoteEditService,
      NotebookGitBindingRepository bindingRepository,
      NotebookAttachmentContent attachmentContent,
      NotebookAttachmentRepository attachmentRepository,
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence,
      CanonicalDonutOrigin canonicalDonutOrigin,
      FolderSiblingNameValidation folderSiblingNameValidation) {
    this.webNoteEditService = webNoteEditService;
    this.bindingRepository = bindingRepository;
    this.attachmentContent = attachmentContent;
    this.attachmentRepository = attachmentRepository;
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
    this.folderSiblingNameValidation = folderSiblingNameValidation;
  }

  /** Returns the saved note. */
  public Note upload(Note note, MultipartFile picture, Timestamp updatedAt)
      throws IOException, UnexpectedNoAccessRightException {
    Integer notebookId = note.getNotebook().getId();
    String filename = picture.getOriginalFilename();
    if (bindingRepository.findByNotebook_Id(notebookId).isEmpty()) {
      throw NotebookGitBindingMissing.refusal();
    }
    requireFreePlainFilename(note, filename);
    PictureFile.admit(filename, picture.getSize());
    byte[] pointer = attachmentContent.storeAsLfsPointer(notebookId, picture.getBytes());
    return webNoteEditService.edit(
        note.getId(),
        notebookId,
        editing -> attach(editing, filename, pointer, updatedAt),
        editing -> "Upload note image: " + filename,
        updatedAt);
  }

  private void attach(Note note, String filename, byte[] pointer, Timestamp updatedAt) {
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

  /** A name is taken when a file, note or folder in the note's folder holds it, ignoring case. */
  private void requireFreePlainFilename(Note note, String filename) {
    String path =
        NotebookGitPortablePath.ofAttachment(
            NotebookGitPortablePath.folderPath(note.getFolder()), filename);
    if (!NotebookGitPortablePath.isPlainFilename(filename)) {
      throw refused(path, "is not a plain filename", ApiError.ErrorType.BINDING_ERROR);
    }
    if (folderSiblingNameValidation
        .entryHolding(note.getNotebook(), note.getFolder(), filename, Set.of())
        .isPresent()) {
      throw refused(
          path,
          "already exists; rename the picture and upload it again",
          ApiError.ErrorType.RESOURCE_CONFLICT);
    }
  }

  private static ApiException refused(String path, String reason, ApiError.ErrorType type) {
    String message = "Cannot upload " + path + ": it " + reason;
    return new ApiException(message, type, message);
  }
}
