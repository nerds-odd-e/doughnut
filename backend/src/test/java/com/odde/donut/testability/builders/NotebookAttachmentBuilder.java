package com.odde.donut.testability.builders;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.testability.EntityBuilder;
import com.odde.donut.testability.MakeMe;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * A notebook file as the product stores it: a non-empty file's bytes in the notebook's content
 * store and its LFS pointer as the row's accepted Git content.
 */
public class NotebookAttachmentBuilder extends EntityBuilder<NotebookAttachment> {
  private byte[] content;

  public NotebookAttachmentBuilder(MakeMe makeMe, String filename) {
    super(makeMe, new NotebookAttachment());
    entity.setFilename(filename);
    content = filename.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getNotebook() == null) {
      throw new AssertionError("notebook or folder is required for NotebookAttachment");
    }
    if (entity.getAcceptedGitContent() != null) return;
    try {
      entity.setAcceptedGitContent(
          content.length == 0
              ? content
              : makeMe.notebookAttachmentContent.storeAsLfsPointer(
                  entity.getNotebook().getId(), content));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public NotebookAttachmentBuilder atRootOf(Notebook notebook) {
    entity.setNotebook(notebook);
    return this;
  }

  public NotebookAttachmentBuilder in(Folder folder) {
    entity.setNotebook(folder.getNotebook());
    entity.setFolder(folder);
    return this;
  }

  public NotebookAttachmentBuilder content(byte[] content) {
    this.content = content;
    return this;
  }

  /** The row's accepted Git content verbatim, without storing any bytes behind it. */
  public NotebookAttachmentBuilder pointerOnly(byte[] pointer) {
    entity.setAcceptedGitContent(pointer);
    return this;
  }
}
