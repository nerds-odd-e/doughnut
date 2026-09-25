package com.odde.donut.services.notebookGit;

import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookGit.NotebookGitAcceptedRepositoryStore.OpenedAcceptedRepository;
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
  private final NoteImageFileAttachment noteImageFileAttachment;
  private final NotebookGitAcceptedRepositoryStore repositoryStore;

  public WebNoteImageUploadService(
      WebNoteEditService webNoteEditService,
      NotebookGitBindingRepository bindingRepository,
      NotebookAttachmentContent attachmentContent,
      NoteImageFileAttachment noteImageFileAttachment,
      NotebookGitAcceptedRepositoryStore repositoryStore) {
    this.webNoteEditService = webNoteEditService;
    this.bindingRepository = bindingRepository;
    this.attachmentContent = attachmentContent;
    this.noteImageFileAttachment = noteImageFileAttachment;
    this.repositoryStore = repositoryStore;
  }

  /** Returns the saved note. */
  public Note upload(Note note, MultipartFile picture, Timestamp updatedAt)
      throws IOException, UnexpectedNoAccessRightException {
    Integer notebookId = note.getNotebook().getId();
    String filename = picture.getOriginalFilename();
    requireFreePlainFilename(note, filename);
    byte[] pointer = attachmentContent.storeAsLfsPointer(notebookId, picture.getBytes());
    return webNoteEditService.edit(
        note.getId(),
        notebookId,
        editing -> noteImageFileAttachment.attach(editing, filename, pointer, updatedAt),
        editing -> "Upload note image: " + filename,
        updatedAt);
  }

  /** A name is taken when the accepted tree has a file, note or folder at that path. */
  private void requireFreePlainFilename(Note note, String filename) {
    String path =
        NotebookGitPortablePath.ofAttachment(
            NotebookGitPortablePath.folderPath(note.getFolder()), filename);
    if (!NotebookGitPortablePath.isPlainFilename(filename)) {
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
      return NotebookGitAcceptedTree.takenPaths(accepted.repository(), accepted.head()).test(path);
    }
  }

  private static ApiException refused(String path, String reason, ApiError.ErrorType type) {
    String message = "Cannot upload " + path + ": it " + reason;
    return new ApiException(message, type, message);
  }
}
