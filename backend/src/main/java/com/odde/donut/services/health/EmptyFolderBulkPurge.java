package com.odde.donut.services.health;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.factoryServices.EntityPersister;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class EmptyFolderBulkPurge {
  private final FolderRepository folderRepository;
  private final EntityPersister entityPersister;

  public EmptyFolderBulkPurge(FolderRepository folderRepository, EntityPersister entityPersister) {
    this.folderRepository = folderRepository;
    this.entityPersister = entityPersister;
  }

  public void apply(Notebook notebook) {
    List<Folder> folders = folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId());
    Set<Integer> occupiedFolderIds =
        folderRepository.findOccupiedFolderIdsByNotebookId(notebook.getId());

    for (Folder folder :
        FolderSubtreeOccupancy.cascadeSafeFullyEmptyFolders(folders, occupiedFolderIds)) {
      entityPersister.remove(folder);
    }
  }
}
