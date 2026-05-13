package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import org.apache.logging.log4j.util.Strings;

public class FolderBuilder extends EntityBuilder<Folder> {
  static final TestObjectCounter nameCounter = new TestObjectCounter(n -> "folder" + n);

  private Notebook notebook;
  private Folder parentFolder;

  public FolderBuilder(MakeMe makeMe, Folder folder) {
    super(makeMe, folder != null ? folder : new Folder());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (notebook != null) {
      entity.setNotebook(notebook);
    }
    if (entity.getNotebook() == null) {
      throw new AssertionError("notebook is required for Folder");
    }
    if (Strings.isEmpty(entity.getName())) {
      entity.setName(nameCounter.generate());
    }
    Timestamp now = new Timestamp(System.currentTimeMillis());
    if (entity.getCreatedAt() == null) {
      entity.setCreatedAt(now);
    }
    if (entity.getUpdatedAt() == null) {
      entity.setUpdatedAt(now);
    }
    if (parentFolder != null) {
      entity.setParentFolder(parentFolder);
    }
    if (needPersist && parentFolder != null) {
      ensureFolderAncestorsPersisted(parentFolder);
    }
  }

  private void ensureFolderAncestorsPersisted(Folder folder) {
    if (folder.getParentFolder() != null) {
      ensureFolderAncestorsPersisted(folder.getParentFolder());
    }
    if (folder.getId() == null) {
      makeMe.entityPersister.save(folder);
    }
  }

  public FolderBuilder notebook(Notebook notebook) {
    if (this.parentFolder != null) {
      throw new AssertionError(
          "Don't set notebook and parent folder at the same time. It leads to inconsistency in test.");
    }
    this.notebook = notebook;
    return this;
  }

  public FolderBuilder name(String name) {
    entity.setName(name);
    return this;
  }

  public FolderBuilder parentFolder(Folder parentFolder) {
    if (this.notebook != null) {
      throw new AssertionError(
          "Don't set notebook and parent folder at the same time. It leads to inconsistency in test.");
    }
    this.parentFolder = parentFolder;
    if (this.parentFolder != null) {
      this.notebook = parentFolder.getNotebook();
    }
    return this;
  }

  public FolderBuilder indexContent(String content) {
    entity.setIndexContent(content);
    return this;
  }
}
