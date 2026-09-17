package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Note;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookExport.ExportFolderRow;
import com.odde.donut.services.notebookExport.NotebookExportRows;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookExport.PortableTreeSnapshot;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcceptedWebChangeService {
  private final NotebookGitStateLoader notebookGitStateLoader;
  private final NotebookGitProjection projection;
  private final AcceptedSnapshotPersistence acceptedSnapshotPersistence;
  private final EntityPersister entityPersister;

  public AcceptedWebChangeService(
      NotebookGitStateLoader notebookGitStateLoader,
      NotebookGitProjection projection,
      AcceptedSnapshotPersistence acceptedSnapshotPersistence,
      EntityPersister entityPersister) {
    this.notebookGitStateLoader = notebookGitStateLoader;
    this.projection = projection;
    this.acceptedSnapshotPersistence = acceptedSnapshotPersistence;
    this.entityPersister = entityPersister;
  }

  @FunctionalInterface
  public interface CompleteOperation<T> {
    T run(LockedNotebooks locked) throws UnexpectedNoAccessRightException;
  }

  @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
  public <T> T apply(
      Integer notebookId,
      CompleteOperation<T> operation,
      Function<T, String> commitMessage,
      Timestamp updatedAt)
      throws UnexpectedNoAccessRightException {
    var lockedState = notebookGitStateLoader.findByNotebookIdForUpdate(notebookId);
    if (lockedState.isEmpty()) {
      return operation.run(new LockedNotebooks(Map.of()));
    }

    NotebookGitStateLoader.LockedNotebookState state = lockedState.orElseThrow();
    NotebookGitBinding binding = state.binding();
    ObjectId persistedAcceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    try (NotebookGitBundleImporter.ImportedBundle accepted =
        NotebookGitBundleImporter.importMainHead(binding.getBundleBytes(), "accepted-bundle")) {
      if (!accepted.mainHead().equals(persistedAcceptedHead)) {
        throw new IllegalStateException("Accepted bundle main does not match its persisted head");
      }
      boolean acceptedTreeMatchedBeforeSave = acceptedTreeMatches(state, accepted);
      T result = operation.run(new LockedNotebooks(Map.of(notebookId, state)));
      if (!acceptedTreeMatchedBeforeSave) {
        return result;
      }
      entityPersister.flush();
      List<ExportFolderRow> currentFolders = notebookGitStateLoader.foldersOf(state.notebook());
      List<Note> currentLiveNotes = notebookGitStateLoader.liveNotesOf(state.notebook());
      if (projection.matchesAcceptedTree(
          state.notebook(),
          currentFolders,
          currentLiveNotes,
          accepted.repository(),
          accepted.mainHead())) {
        return result;
      }

      List<PortableTreeEntry> entries =
          PortableTreeSnapshot.build(
              state.notebook().getReadmeContent(),
              currentFolders,
              NotebookExportRows.notes(currentLiveNotes));
      acceptedSnapshotPersistence.persist(
          accepted, entries, binding, updatedAt, commitMessage.apply(result));
      return result;
    }
  }

  private boolean acceptedTreeMatches(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitBundleImporter.ImportedBundle accepted) {
    return projection.matchesAcceptedTree(
        state.notebook(),
        state.folders(),
        state.liveNotes(),
        accepted.repository(),
        accepted.mainHead());
  }
}
