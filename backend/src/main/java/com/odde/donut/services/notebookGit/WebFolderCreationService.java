package com.odde.donut.services.notebookGit;

import com.odde.donut.controllers.dto.FolderCreationRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.FolderConstructionService;
import com.odde.donut.testability.TestabilitySettings;
import org.springframework.stereotype.Service;

@Service
public class WebFolderCreationService {
  private final AcceptedWebChangeService acceptedWebChangeService;
  private final NotebookRepository notebookRepository;
  private final FolderConstructionService folderConstructionService;
  private final TestabilitySettings testabilitySettings;

  public WebFolderCreationService(
      AcceptedWebChangeService acceptedWebChangeService,
      NotebookRepository notebookRepository,
      FolderConstructionService folderConstructionService,
      TestabilitySettings testabilitySettings) {
    this.acceptedWebChangeService = acceptedWebChangeService;
    this.notebookRepository = notebookRepository;
    this.folderConstructionService = folderConstructionService;
    this.testabilitySettings = testabilitySettings;
  }

  public Folder createFolder(Notebook notebook, FolderCreationRequest request)
      throws UnexpectedNoAccessRightException {
    return acceptedWebChangeService.apply(
        notebook.getId(),
        () -> {
          Notebook liveNotebook = notebookRepository.findById(notebook.getId()).orElseThrow();
          return folderConstructionService.createFolder(liveNotebook, request);
        },
        folder -> "Add folder: " + folder.getName(),
        testabilitySettings.getCurrentUTCTimestamp());
  }
}
