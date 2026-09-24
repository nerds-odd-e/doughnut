package com.odde.donut.testability.builders;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.testability.EntityBuilder;
import com.odde.donut.testability.MakeMe;
import java.nio.charset.StandardCharsets;

public class NotebookAttachmentBuilder extends EntityBuilder<NotebookAttachment> {

  public NotebookAttachmentBuilder(MakeMe makeMe, String filename) {
    super(makeMe, new NotebookAttachment());
    entity.setFilename(filename);
    entity.setAcceptedGitContent(filename.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getNotebook() == null) {
      throw new AssertionError("notebook or folder is required for NotebookAttachment");
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
}
