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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
    return apply(Set.of(notebookId), operation, commitMessage, updatedAt);
  }

  @Transactional(isolation = Isolation.SERIALIZABLE, rollbackFor = Exception.class)
  public <T> T apply(
      Set<Integer> notebookIds,
      CompleteOperation<T> operation,
      Function<T, String> commitMessage,
      Timestamp updatedAt)
      throws UnexpectedNoAccessRightException {
    Map<Integer, NotebookGitStateLoader.LockedNotebookState> states = new LinkedHashMap<>();
    List<OpenedNotebook> opened = new ArrayList<>();
    try {
      for (Integer notebookId : new TreeSet<>(notebookIds)) {
        notebookGitStateLoader
            .findByNotebookIdForUpdate(notebookId)
            .ifPresent(
                state -> {
                  states.put(notebookId, state);
                  opened.add(open(state));
                });
      }
      T result = operation.run(new LockedNotebooks(states));
      List<OpenedNotebook> matchedBefore =
          opened.stream().filter(OpenedNotebook::matchedBefore).toList();
      if (!matchedBefore.isEmpty()) {
        entityPersister.flush();
        String message = commitMessage.apply(result);
        matchedBefore.forEach(notebook -> commitIfChanged(notebook, message, updatedAt));
      }
      return result;
    } finally {
      opened.forEach(notebook -> notebook.accepted().close());
    }
  }

  private record OpenedNotebook(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitBundleImporter.ImportedBundle accepted,
      boolean matchedBefore) {}

  private OpenedNotebook open(NotebookGitStateLoader.LockedNotebookState state) {
    NotebookGitBinding binding = state.binding();
    ObjectId persistedAcceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    NotebookGitBundleImporter.ImportedBundle accepted =
        NotebookGitBundleImporter.importMainHead(binding.getBundleBytes(), "accepted-bundle");
    if (!accepted.mainHead().equals(persistedAcceptedHead)) {
      accepted.close();
      throw new IllegalStateException("Accepted bundle main does not match its persisted head");
    }
    return new OpenedNotebook(state, accepted, acceptedTreeMatches(state, accepted));
  }

  private void commitIfChanged(OpenedNotebook notebook, String message, Timestamp updatedAt) {
    NotebookGitStateLoader.LockedNotebookState state = notebook.state();
    NotebookGitBundleImporter.ImportedBundle accepted = notebook.accepted();
    List<ExportFolderRow> currentFolders = notebookGitStateLoader.foldersOf(state.notebook());
    List<Note> currentStoredNotes = notebookGitStateLoader.storedNotesOf(state.notebook());
    if (projection.matchesAcceptedTree(
        state.notebook(),
        currentFolders,
        currentStoredNotes,
        accepted.repository(),
        accepted.mainHead())) {
      return;
    }
    List<PortableTreeEntry> entries =
        PortableTreeSnapshot.build(
            state.notebook().getReadmeContent(),
            currentFolders,
            NotebookExportRows.notes(currentStoredNotes));
    acceptedSnapshotPersistence.persist(accepted, entries, state.binding(), updatedAt, message);
  }

  private boolean acceptedTreeMatches(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitBundleImporter.ImportedBundle accepted) {
    return projection.matchesAcceptedTree(
        state.notebook(),
        state.folders(),
        state.storedNotes(),
        accepted.repository(),
        accepted.mainHead());
  }
}
