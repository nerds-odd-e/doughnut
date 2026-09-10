package com.odde.donut.services.notebookGit;

import com.odde.donut.controllers.dto.FolderCreationRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.FolderConstructionService;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Materializes proposal folder paths from accepted represented destinations and missing ancestry.
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

  void materialize(
      Notebook notebook,
      List<ExportFolderRow> liveFolders,
      Repository repository,
      ObjectId acceptedHead,
      List<String> documentPaths,
      NotebookGitProposalImporter.ImportedProposal proposal) {
    Map<String, Folder> folders = representedFolders(liveFolders, repository, acceptedHead);
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
      if (documentPath.endsWith("/README.md")) {
        Folder folder = folders.get(documentPath.substring(0, documentPath.lastIndexOf('/')));
        folder.setReadmeContent(NotebookGitProposalTypedPath.requireReadme(proposal, documentPath));
        entityPersister.save(folder);
      }
    }
    entityPersister.flush();
  }

  private Map<String, Folder> representedFolders(
      List<ExportFolderRow> liveFolders, Repository repository, ObjectId acceptedHead) {
    Map<Integer, ExportFolderRow> folderById =
        NotebookGitAcceptedTree.indexFoldersById(liveFolders);
    List<PortableTreeEntry> accepted =
        NotebookGitAcceptedTree.readEntries(repository, acceptedHead);
    Map<String, Folder> folders = new LinkedHashMap<>();
    for (ExportFolderRow row : liveFolders) {
      String folderPath = NotebookGitAcceptedTree.folderPath(row, folderById);
      if (NotebookGitAcceptedTree.representedInAccepted(folderPath, accepted)) {
        folders.put(
            folderPath.substring(0, folderPath.length() - 1),
            entityPersister.find(Folder.class, row.id()));
      }
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
