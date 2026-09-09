package com.odde.donut.services.notebookGit;

import com.odde.donut.controllers.dto.FolderCreationRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.FolderConstructionService;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Creates validated Folders from proposal paths. Root Folders may be materialised with an authored
 * Readme or with absent Readme content. Nested {@code Parent/Child/README.md} hierarchies create
 * the parent without a Readme, then the child with the authored Readme, and return refreshed
 * projection rows.
 */
@Service
class NotebookGitProposalFolderMaterialization {

  private final FolderConstructionService folderConstructionService;
  private final EntityPersister entityPersister;
  private final Validator validator;
  private final NotebookGitStateLoader notebookGitStateLoader;

  NotebookGitProposalFolderMaterialization(
      FolderConstructionService folderConstructionService,
      EntityPersister entityPersister,
      Validator validator,
      NotebookGitStateLoader notebookGitStateLoader) {
    this.folderConstructionService = folderConstructionService;
    this.entityPersister = entityPersister;
    this.validator = validator;
    this.notebookGitStateLoader = notebookGitStateLoader;
  }

  Folder createRootFolderWithoutReadme(Notebook notebook, String pathForError, String folderName) {
    Folder folder =
        folderConstructionService.createFolder(
            notebook, validFolderRequest(pathForError, folderName, null));
    entityPersister.flush();
    return folder;
  }

  Folder createRootFolderWithReadme(Notebook notebook, String readmePath, String readme) {
    String folderName = readmePath.substring(0, readmePath.indexOf('/'));
    Folder folder =
        folderConstructionService.createFolder(
            notebook, validFolderRequest(readmePath, folderName, null));
    folder.setReadmeContent(readme);
    entityPersister.save(folder);
    entityPersister.flush();
    return folder;
  }

  /**
   * Creates {@code Parent} then {@code Child} for an exact nested Folder Readme path. Parent has
   * absent Readme content; Child stores the authored Readme. Returns refreshed Folder projection
   * rows.
   */
  List<ExportFolderRow> createNestedFolderWithChildReadme(
      Notebook notebook,
      String pathForError,
      String parentFolderName,
      String childFolderName,
      String readme) {
    Folder parent = createRootFolderWithoutReadme(notebook, pathForError, parentFolderName);
    Folder child =
        folderConstructionService.createFolder(
            notebook, validFolderRequest(pathForError, childFolderName, parent.getId()));
    child.setReadmeContent(readme);
    entityPersister.save(child);
    entityPersister.flush();
    return notebookGitStateLoader.foldersOf(notebook);
  }

  private FolderCreationRequest validFolderRequest(
      String path, String folderName, Integer underFolderId) {
    FolderCreationRequest request = new FolderCreationRequest();
    request.setName(folderName);
    request.setUnderFolderId(underFolderId);
    Set<ConstraintViolation<FolderCreationRequest>> violations = validator.validate(request);
    if (!violations.isEmpty()) {
      String reason =
          violations.stream()
              .map(ConstraintViolation::getMessage)
              .sorted()
              .findFirst()
              .orElseThrow();
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid folder name at path \"" + path + "\": " + reason);
    }
    return request;
  }
}
