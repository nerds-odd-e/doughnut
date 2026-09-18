package com.odde.donut.services.notebookGit;

import com.odde.donut.controllers.dto.FolderCreationRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
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

/** Materializes proposal folder paths from live destination folders and missing ancestry. */
@Service
class NotebookGitProposalFolderMaterialization {

  private final FolderConstructionService folderConstructionService;
  private final EntityPersister entityPersister;
  private final FolderRepository folderRepository;
  private final Validator validator;

  NotebookGitProposalFolderMaterialization(
      FolderConstructionService folderConstructionService,
      EntityPersister entityPersister,
      FolderRepository folderRepository,
      Validator validator) {
    this.folderConstructionService = folderConstructionService;
    this.entityPersister = entityPersister;
    this.folderRepository = folderRepository;
    this.validator = validator;
  }

  Map<String, Folder> materialize(
      Notebook notebook,
      List<String> documentPaths,
      NotebookGitProposalImporter.ImportedProposal proposal) {
    Map<String, Folder> folders = ensureAncestry(notebook, documentPaths);
    persistFolderReadmes(folders, documentPaths, proposal);
    entityPersister.flush();
    return folders;
  }

  /**
   * Ensures parent folders for admitted destination paths, creating any ancestry absent from live
   * folders. {@link FolderConstructionService} remains the creation owner.
   */
  Map<String, Folder> ensureAncestry(Notebook notebook, List<String> destinationPaths) {
    Map<String, Folder> folders = foldersByPath(notebook);
    for (String destinationPath : destinationPaths) {
      ensureAncestry(notebook, folders, destinationPath);
    }
    return folders;
  }

  private void ensureAncestry(
      Notebook notebook, Map<String, Folder> folders, String destinationPath) {
    Folder parent = null;
    int componentStart = 0;
    int separator = destinationPath.indexOf('/');
    while (separator >= 0) {
      String folderPath = destinationPath.substring(0, separator);
      Folder folder = folders.get(folderPath);
      if (folder == null) {
        folder =
            folderConstructionService.createFolder(
                notebook,
                validFolderRequest(
                    destinationPath,
                    destinationPath.substring(componentStart, separator),
                    parent == null ? null : parent.getId()));
        folders.put(folderPath, folder);
      }
      parent = folder;
      componentStart = separator + 1;
      separator = destinationPath.indexOf('/', componentStart);
    }
  }

  private void persistFolderReadmes(
      Map<String, Folder> folders,
      List<String> documentPaths,
      NotebookGitProposalImporter.ImportedProposal proposal) {
    for (String documentPath : documentPaths) {
      if (!documentPath.endsWith("/README.md")) {
        continue;
      }
      Folder folder = folders.get(documentPath.substring(0, documentPath.lastIndexOf('/')));
      folder.setReadmeContent(NotebookGitProposalTypedPath.requireReadme(proposal, documentPath));
      entityPersister.save(folder);
    }
  }

  private Map<String, Folder> foldersByPath(Notebook notebook) {
    Map<String, Folder> folders = new LinkedHashMap<>();
    for (Folder folder : folderRepository.findByNotebookIdOrderByIdAsc(notebook.getId())) {
      String folderPath = NotebookGitLivePortablePath.folderPath(folder);
      folders.put(folderPath.substring(0, folderPath.length() - 1), folder);
    }
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
