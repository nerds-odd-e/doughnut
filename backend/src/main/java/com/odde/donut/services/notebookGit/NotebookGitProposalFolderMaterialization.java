package com.odde.donut.services.notebookGit;

import com.odde.donut.controllers.dto.FolderCreationRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.FolderConstructionService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Creates validated Folders and their ancestry from proposal paths. */
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

  Map<String, Folder> createFolderAncestry(Notebook notebook, List<String> documentPaths) {
    Map<String, Folder> folders = new LinkedHashMap<>();
    for (String documentPath : documentPaths) {
      Folder parent = null;
      int componentStart = 0;
      int separator = documentPath.indexOf('/');
      while (separator >= 0) {
        String folderPath = documentPath.substring(0, separator);
        Folder folder = folders.get(folderPath);
        if (folder == null) {
          folder =
              folderConstructionService.createFolder(
                  notebook,
                  validFolderRequest(
                      documentPath,
                      documentPath.substring(componentStart, separator),
                      parent == null ? null : parent.getId()));
          folders.put(folderPath, folder);
        }
        parent = folder;
        componentStart = separator + 1;
        separator = documentPath.indexOf('/', componentStart);
      }
    }
    entityPersister.flush();
    return folders;
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
