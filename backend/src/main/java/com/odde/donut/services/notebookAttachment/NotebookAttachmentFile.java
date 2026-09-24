package com.odde.donut.services.notebookAttachment;

import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.notebookGit.NotebookGitLfsPointer;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * The file a notebook attachment row stands for: its size and exact bytes. The notebook's
 * attachment representation decides how the row's accepted Git content is read; in an LFS notebook
 * a non-empty row is a pointer whose bytes live in the content store.
 */
@Service
public class NotebookAttachmentFile {
  private final NotebookGitBindingRepository notebookGitBindingRepository;
  private final NotebookAttachmentContent notebookAttachmentContent;

  public NotebookAttachmentFile(
      NotebookGitBindingRepository notebookGitBindingRepository,
      NotebookAttachmentContent notebookAttachmentContent) {
    this.notebookGitBindingRepository = notebookGitBindingRepository;
    this.notebookAttachmentContent = notebookAttachmentContent;
  }

  public long size(NotebookAttachment attachment) {
    return lfsPointer(attachment)
        .map(NotebookGitLfsPointer.Parsed::size)
        .orElse((long) attachment.getAcceptedGitContent().length);
  }

  public byte[] bytes(NotebookAttachment attachment) {
    return lfsPointer(attachment)
        .map(
            pointer ->
                notebookAttachmentContent
                    .get(attachment.getNotebook().getId(), pointer.sha256Hex())
                    .orElseThrow(
                        () ->
                            new IllegalStateException(
                                "File content unavailable: " + attachment.getFilename())))
        .orElse(attachment.getAcceptedGitContent());
  }

  /** The row's pointer when its notebook stores attachments as LFS and the file is not empty. */
  private Optional<NotebookGitLfsPointer.Parsed> lfsPointer(NotebookAttachment attachment) {
    byte[] content = attachment.getAcceptedGitContent();
    if (NotebookGitLfsPointer.isEmptyFile(content)) return Optional.empty();
    return notebookGitBindingRepository
        .findByNotebook_Id(attachment.getNotebook().getId())
        .map(NotebookGitBinding::getAttachmentRepresentation)
        .filter(NotebookGitAttachmentRepresentation.LFS::equals)
        .map(lfs -> NotebookGitLfsPointer.parse(content).orElseThrow());
  }
}
