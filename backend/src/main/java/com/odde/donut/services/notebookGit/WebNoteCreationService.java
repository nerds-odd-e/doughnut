package com.odde.donut.services.notebookGit;

import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.User;
import com.odde.donut.services.NoteConstructionService;
import com.odde.donut.services.notebookExport.NotebookExportRows;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookExport.PortableTreeSnapshot;
import com.odde.donut.services.wikidataApis.WikidataIdWithApi;
import com.odde.donut.testability.TestabilitySettings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Service;

@Service
public class WebNoteCreationService {
  private final NotebookGitStateLoader notebookGitStateLoader;
  private final NoteConstructionService noteConstructionService;
  private final NotebookGitProjection projection;
  private final AcceptedSnapshotPersistence acceptedSnapshotPersistence;
  private final TestabilitySettings testabilitySettings;

  public WebNoteCreationService(
      NotebookGitStateLoader notebookGitStateLoader,
      NoteConstructionService noteConstructionService,
      NotebookGitProjection projection,
      AcceptedSnapshotPersistence acceptedSnapshotPersistence,
      TestabilitySettings testabilitySettings) {
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.noteConstructionService = noteConstructionService;
    this.projection = projection;
    this.acceptedSnapshotPersistence = acceptedSnapshotPersistence;
    this.testabilitySettings = testabilitySettings;
  }

  public NoteRealm createRootNote(
      Notebook notebook,
      NoteCreationDTO noteCreation,
      User user,
      WikidataIdWithApi wikidataIdWithApi)
      throws InterruptedException, IOException {
    if (!isTitleOnlyRoot(noteCreation)) {
      return noteConstructionService.createRootNoteWithWikidataService(
          notebook, noteCreation, user, wikidataIdWithApi);
    }
    var lockedState = notebookGitStateLoader.findByNotebookIdForUpdate(notebook.getId());
    if (lockedState.isEmpty()) {
      return noteConstructionService.createRootNoteWithWikidataService(
          notebook, noteCreation, user, wikidataIdWithApi);
    }

    NotebookGitStateLoader.LockedNotebookState state = lockedState.orElseThrow();
    NotebookGitBinding binding = state.binding();
    ObjectId persistedAcceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    try (NotebookGitBundleImporter.ImportedBundle accepted =
        NotebookGitBundleImporter.importMainHead(binding.getBundleBytes(), "accepted-bundle")) {
      if (!accepted.mainHead().equals(persistedAcceptedHead)) {
        throw new IllegalStateException("Accepted bundle main does not match its persisted head");
      }
      boolean eligible = emptyMatchingAcceptedTree(state, accepted);
      NoteRealm realm =
          noteConstructionService.createRootNoteWithWikidataService(
              state.notebook(), noteCreation, user, wikidataIdWithApi);
      if (!eligible) {
        return realm;
      }

      List<Note> completeLiveNotes = new ArrayList<>(state.liveNotes());
      completeLiveNotes.add(realm.getNote());
      List<PortableTreeEntry> entries =
          PortableTreeSnapshot.build(
              state.notebook().getReadmeContent(),
              state.folders(),
              NotebookExportRows.notes(completeLiveNotes));
      acceptedSnapshotPersistence.persist(
          accepted,
          entries,
          binding,
          testabilitySettings.getCurrentUTCTimestamp(),
          "Add note: " + realm.getNote().getTitle());
      return realm;
    }
  }

  private static boolean isTitleOnlyRoot(NoteCreationDTO noteCreation) {
    return noteCreation.getFolderId() == null && noteCreation.getContent() == null;
  }

  private boolean emptyMatchingAcceptedTree(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitBundleImporter.ImportedBundle accepted) {
    List<PortableTreeEntry> currentEntries =
        PortableTreeSnapshot.build(
            state.notebook().getReadmeContent(),
            state.folders(),
            NotebookExportRows.notes(state.liveNotes()));
    return currentEntries.isEmpty()
        && projection.matchesAcceptedTree(
            state.notebook(),
            state.folders(),
            state.liveNotes(),
            accepted.repository(),
            accepted.mainHead());
  }
}
