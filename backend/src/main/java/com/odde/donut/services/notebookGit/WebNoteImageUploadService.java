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
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookGit.NotebookGitAcceptedRepositoryStore.OpenedAcceptedRepository;
import com.odde.donut.validators.AuthoredNoteContent;
import java.io.IOException;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * A picture uploaded for a note's {@code image} property becomes a file in the note's folder under
 * its uploaded name. A name that is not a plain filename, or is already taken there, is refused
 * before anything is stored; nothing is renamed or overwritten. Otherwise its bytes are stored
 * first, then the pointer and the note's {@code image:} are accepted together as one web change,
 * with the content prepared like any ordinary content save.
 */
@Service
public class WebNoteImageUploadService {
  private final WebNoteEditService webNoteEditService;
  private final NotebookGitBindingRepository bindingRepository;
  private final NotebookAttachmentContent attachmentContent;
  private final NotebookAttachmentRepository attachmentRepository;
  private final AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence;
  private final CanonicalDonutOrigin canonicalDonutOrigin;
  private final NotebookGitAcceptedRepositoryStore repositoryStore;

  public WebNoteImageUploadService(
      WebNoteEditService webNoteEditService,
      NotebookGitBindingRepository bindingRepository,
      NotebookAttachmentContent attachmentContent,
      NotebookAttachmentRepository attachmentRepository,
      AuthoredNoteDocumentPersistence authoredNoteDocumentPersistence,
      CanonicalDonutOrigin canonicalDonutOrigin,
      NotebookGitAcceptedRepositoryStore repositoryStore) {
    this.webNoteEditService = webNoteEditService;
    this.bindingRepository = bindingRepository;
    this.attachmentContent = attachmentContent;
    this.attachmentRepository = attachmentRepository;
    this.authoredNoteDocumentPersistence = authoredNoteDocumentPersistence;
    this.canonicalDonutOrigin = canonicalDonutOrigin;
    this.repositoryStore = repositoryStore;
  }

  /** Returns the saved note. */
  public Note upload(Note note, MultipartFile picture, Timestamp updatedAt)
      throws IOException, UnexpectedNoAccessRightException {
    Integer notebookId = note.getNotebook().getId();
    requireLfs(notebookId);
    String filename = picture.getOriginalFilename();
    requireFreePlainFilename(note, filename);
    byte[] pointer = attachmentContent.storeAsLfsPointer(notebookId, picture.getBytes());
    return webNoteEditService.edit(
        note.getId(),
        notebookId,
        editing -> {
          addFile(editing, filename, pointer);
          authoredNoteDocumentPersistence.persist(
              editing,
              AuthoredNoteContent.prepareDocumentForSave(
                  NoteContentMarkdown.withNoteImage(editing.getContent(), filename),
                  canonicalDonutOrigin),
              updatedAt);
        },
        editing -> "Upload note image: " + filename,
        updatedAt);
  }

  private void requireLfs(Integer notebookId) {
    if (!bindingRepository.storesAttachmentsAsLfs(notebookId)) {
      throw new IllegalStateException(
          "Notebook " + notebookId + " does not store its files as Git LFS");
    }
  }

  /**
   * A leading dot would be hidden or reserved Git metadata such as {@code .gitattributes}. A name
   * is taken when the accepted tree has a file, note or folder at that path.
   */
  private void requireFreePlainFilename(Note note, String filename) {
    String path =
        NotebookGitPortablePath.ofAttachment(
            NotebookGitPortablePath.folderPath(note.getFolder()), filename);
    if (filename.isEmpty() || filename.contains("/") || filename.startsWith(".")) {
      throw refused(path, "is not a plain filename", ApiError.ErrorType.BINDING_ERROR);
    }
    if (acceptedTreeHas(note.getNotebook().getId(), path)) {
      throw refused(
          path,
          "already exists; rename the picture and upload it again",
          ApiError.ErrorType.RESOURCE_CONFLICT);
    }
  }

  private boolean acceptedTreeHas(Integer notebookId, String path) {
    try (OpenedAcceptedRepository accepted =
        repositoryStore.open(bindingRepository.findByNotebook_Id(notebookId).orElseThrow())) {
      return NotebookGitAcceptedTree.hasPath(accepted.repository(), accepted.head(), path);
    }
  }

  private static ApiException refused(String path, String reason, ApiError.ErrorType type) {
    String message = "Cannot upload " + path + ": it " + reason;
    return new ApiException(message, type, message);
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
