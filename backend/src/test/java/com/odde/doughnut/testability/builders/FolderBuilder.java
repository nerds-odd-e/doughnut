package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.services.WikiSlugPathAssignment;
import com.odde.doughnut.services.WikiSlugPathService;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.Set;
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
    if (entity.getSlug() == null || entity.getSlug().isEmpty()) {
      assignSlugForFolder(needPersist);
    }
  }

  private void ensureFolderAncestorsPersisted(Folder folder) {
    if (folder.getParentFolder() != null) {
      ensureFolderAncestorsPersisted(folder.getParentFolder());
    }
    if (folder.getId() == null) {
      assignSlugForFolderOnEntity(folder, true);
      makeMe.entityPersister.save(folder);
    }
  }

  private void assignSlugForFolder(boolean needPersist) {
    assignSlugForFolderOnEntity(entity, needPersist);
  }

  private void assignSlugForFolderOnEntity(Folder folder, boolean needPersist) {
    if (folder.getSlug() != null && !folder.getSlug().isEmpty()) {
      return;
    }
    WikiSlugPathService service = makeMe.wikiSlugPathService;
    if (service != null
        && needPersist
        && folder.getNotebook() != null
        && folder.getNotebook().getId() != null) {
      service.assignSlugForNewFolder(folder);
    } else if (service != null) {
      service.assignSlugForNewFolderSkippingSiblingQuery(folder);
    } else {
      WikiSlugPathAssignment.setFolderSlug(folder, Set.of());
    }
  }

  public FolderBuilder notebook(Notebook notebook) {
    this.notebook = notebook;
    return this;
  }

  public FolderBuilder name(String name) {
    entity.setName(name);
    return this;
  }

  public FolderBuilder parentFolder(Folder parentFolder) {
    this.parentFolder = parentFolder;
    return this;
  }
}
