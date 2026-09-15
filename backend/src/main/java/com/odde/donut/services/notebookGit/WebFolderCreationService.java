package com.odde.donut.services.notebookGit;

import com.odde.donut.controllers.dto.FolderCreationRequest;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.FolderConstructionService;
import com.odde.donut.services.notebookExport.NotebookExportRows;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookExport.PortableTreeSnapshot;
import com.odde.donut.testability.TestabilitySettings;
import java.io.IOException;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Service;

@Service
public class WebFolderCreationService {
  private final NotebookGitStateLoader notebookGitStateLoader;
  private final FolderConstructionService folderConstructionService;
  private final NotebookGitProjection projection;
  private final AcceptedSnapshotPersistence acceptedSnapshotPersistence;
  private final TestabilitySettings testabilitySettings;

  public WebFolderCreationService(
      NotebookGitStateLoader notebookGitStateLoader,
      FolderConstructionService folderConstructionService,
      NotebookGitProjection projection,
      AcceptedSnapshotPersistence acceptedSnapshotPersistence,
      TestabilitySettings testabilitySettings) {
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.folderConstructionService = folderConstructionService;
    this.projection = projection;
    this.acceptedSnapshotPersistence = acceptedSnapshotPersistence;
    this.testabilitySettings = testabilitySettings;
  }

  public Folder createFolder(Notebook notebook, FolderCreationRequest request) throws IOException {
    var lockedState = notebookGitStateLoader.findByNotebookIdForUpdate(notebook.getId());
    if (lockedState.isEmpty()) {
      return folderConstructionService.createFolder(notebook, request);
    }

    NotebookGitStateLoader.LockedNotebookState state = lockedState.orElseThrow();
    NotebookGitBinding binding = state.binding();
    ObjectId persistedAcceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    try (NotebookGitBundleImporter.ImportedBundle accepted =
        NotebookGitBundleImporter.importMainHead(binding.getBundleBytes(), "accepted-bundle")) {
      if (!accepted.mainHead().equals(persistedAcceptedHead)) {
        throw new IllegalStateException("Accepted bundle main does not match its persisted head");
      }
      boolean eligible =
          projection.matchesAcceptedTree(
              state.notebook(),
              state.folders(),
              state.liveNotes(),
              accepted.repository(),
              accepted.mainHead());
      Folder folder = folderConstructionService.createFolder(state.notebook(), request);
      if (!eligible) {
        return folder;
      }

      List<PortableTreeEntry> entries =
          PortableTreeSnapshot.build(
              state.notebook().getReadmeContent(),
              notebookGitStateLoader.foldersOf(state.notebook()),
              NotebookExportRows.notes(state.liveNotes()));
      acceptedSnapshotPersistence.persist(
          accepted,
          entries,
          binding,
          testabilitySettings.getCurrentUTCTimestamp(),
          "Add folder: " + folder.getName());
      return folder;
    }
  }
}
