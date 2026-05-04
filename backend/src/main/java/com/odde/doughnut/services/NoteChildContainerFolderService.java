package com.odde.doughnut.services;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RelationshipNotePlacement;
import com.odde.doughnut.entities.repositories.FolderRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NoteChildContainerFolderService {

  private static final String RELATIONS_SUBFOLDER_NAME = "relations";

  private final FolderRepository folderRepository;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;

  public NoteChildContainerFolderService(
      FolderRepository folderRepository,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings) {
    this.folderRepository = folderRepository;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
  }

  public Folder resolveForParent(Note parentNote) {
    return findOrCreateChildContainer(parentNote);
  }

  public Folder resolveFolderForRelationshipNote(
      Note sourceNote, RelationshipNotePlacement placement) {
    return switch (placement) {
      case RELATIONS_SUBFOLDER ->
          findOrCreateChildFolder(sourceNote, sourceNote.getFolder(), RELATIONS_SUBFOLDER_NAME);
      case SAME_LEVEL_AS_SOURCE -> sourceNote.getFolder();
      case NAMED_AFTER_SOURCE_NOTE -> resolveForParent(sourceNote);
    };
  }

  private Folder findOrCreateChildContainer(Note parentNote) {
    return findOrCreateChildFolder(
        parentNote, parentNote.getFolder(), folderNameForNote(parentNote));
  }

  private Folder findOrCreateChildFolder(
      Note anchorNote, Folder parentFolderContext, String childFolderName) {
    Integer parentFolderId = parentFolderContext == null ? null : parentFolderContext.getId();
    List<Folder> candidates =
        folderRepository.findCandidateChildContainers(
            anchorNote.getNotebook().getId(), parentFolderId, childFolderName);
    if (!candidates.isEmpty()) {
      return candidates.getFirst();
    }
    return createFolder(anchorNote, parentFolderContext, childFolderName);
  }

  private Folder createFolder(Note anchorNote, Folder parentFolderContext, String name) {
    Folder folder = new Folder();
    folder.setNotebook(anchorNote.getNotebook());
    folder.setParentFolder(parentFolderContext);
    folder.setName(name);
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    folder.setCreatedAt(now);
    folder.setUpdatedAt(now);
    entityPersister.save(folder);
    return folder;
  }

  private static String folderNameForNote(Note note) {
    String title = note.getTitle();
    if (title == null || title.isEmpty()) {
      return " ";
    }
    return title;
  }
}
