package com.odde.donut.services.notebookGit;

import com.odde.donut.controllers.dto.FolderCreationRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.FolderConstructionService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Creates validated Folders from proposal paths. Root Folders may be materialised with an authored
 * Readme or with absent Readme content.
 */
@Service
class NotebookGitProposalFolderMaterialization {

  private final FolderConstructionService folderConstructionService;
  private final EntityPersister entityPersister;
  private final Validator validator;

  NotebookGitProposalFolderMaterialization(
      FolderConstructionService folderConstructionService,
      EntityPersister entityPersister,
      Validator validator) {
    this.folderConstructionService = folderConstructionService;
    this.entityPersister = entityPersister;
    this.validator = validator;
  }

  Folder createRootFolderWithoutReadme(Notebook notebook, String pathForError, String folderName) {
    Folder folder =
        folderConstructionService.createFolder(
            notebook, validRootFolderRequest(pathForError, folderName));
    entityPersister.flush();
    return folder;
  }

  Folder createRootFolderWithReadme(Notebook notebook, String readmePath, String readme) {
    String folderName = readmePath.substring(0, readmePath.indexOf('/'));
    Folder folder =
        folderConstructionService.createFolder(
            notebook, validRootFolderRequest(readmePath, folderName));
    folder.setReadmeContent(readme);
    entityPersister.save(folder);
    entityPersister.flush();
    return folder;
  }

  private FolderCreationRequest validRootFolderRequest(String path, String folderName) {
    FolderCreationRequest request = new FolderCreationRequest();
    request.setName(folderName);
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
