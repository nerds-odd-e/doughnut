package com.odde.donut.services.notebookAttachment;

import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.services.notebookGit.NotebookGitLfsPointer;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * The file a notebook attachment row stands for: its size and exact bytes. A non-empty row is an
 * LFS pointer whose bytes live in the content store.
 */
@Service
public class NotebookAttachmentFile {
  private final NotebookAttachmentContent notebookAttachmentContent;

  public NotebookAttachmentFile(NotebookAttachmentContent notebookAttachmentContent) {
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

  /** The row's pointer when the file is not empty. */
  private Optional<NotebookGitLfsPointer.Parsed> lfsPointer(NotebookAttachment attachment) {
    byte[] content = attachment.getAcceptedGitContent();
    if (NotebookGitLfsPointer.isEmptyFile(content)) {
      return Optional.empty();
    }
    return Optional.of(NotebookGitLfsPointer.parse(content).orElseThrow());
  }
}
