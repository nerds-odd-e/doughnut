package com.odde.donut.services.notebookAttachment;

import com.odde.donut.entities.NotebookAttachment;
import org.springframework.stereotype.Service;

/** The file a notebook attachment row stands for: its size and exact bytes. */
@Service
public class NotebookAttachmentFile {

  public long size(NotebookAttachment attachment) {
    return bytes(attachment).length;
  }

  public byte[] bytes(NotebookAttachment attachment) {
    return attachment.getAcceptedGitContent();
  }
}
