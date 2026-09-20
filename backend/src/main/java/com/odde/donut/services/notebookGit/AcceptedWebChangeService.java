package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookExport.NotebookLivePortableTree;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookGit.NotebookGitAcceptedRepositoryStore.OpenedAcceptedRepository;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcceptedWebChangeService {
  private final NotebookGitProjection projection;
  private final NotebookGitBindingRepository bindingRepository;
  private final NotebookLivePortableTree livePortableTree;
  private final EntityPersister entityPersister;
  private final NotebookGitAcceptedRepositoryStore repositoryStore;

  public AcceptedWebChangeService(
      NotebookGitProjection projection,
      NotebookGitBindingRepository bindingRepository,
      NotebookLivePortableTree livePortableTree,
      EntityPersister entityPersister,
      NotebookGitAcceptedRepositoryStore repositoryStore) {
    this.projection = projection;
    this.bindingRepository = bindingRepository;
    this.livePortableTree = livePortableTree;
    this.entityPersister = entityPersister;
    this.repositoryStore = repositoryStore;
  }

  @FunctionalInterface
  public interface CompleteOperation<T> {
    T run() throws UnexpectedNoAccessRightException;
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
    List<OpenedNotebook> opened = new ArrayList<>();
    try {
      for (Integer notebookId : new TreeSet<>(notebookIds)) {
        bindingRepository
            .findByNotebookIdForUpdate(notebookId)
            .ifPresent(binding -> opened.add(open(binding)));
      }
      T result = operation.run();
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
      NotebookGitBinding binding,
      OpenedAcceptedRepository accepted,
      List<PortableTreeEntry> acceptedEntries,
      boolean matchedBefore) {}

  private OpenedNotebook open(NotebookGitBinding binding) {
    OpenedAcceptedRepository accepted = repositoryStore.open(binding);
    List<PortableTreeEntry> acceptedEntries =
        NotebookGitAcceptedTree.readEntries(accepted.repository(), accepted.head());
    return new OpenedNotebook(
        binding,
        accepted,
        acceptedEntries,
        projection.matchesAcceptedTree(snapshot(binding), acceptedEntries));
  }

  private void commitIfChanged(OpenedNotebook notebook, String message, Timestamp updatedAt) {
    List<PortableTreeEntry> entries = snapshot(notebook.binding());
    if (projection.matchesAcceptedTree(entries, notebook.acceptedEntries())) {
      return;
    }
    OpenedAcceptedRepository accepted = notebook.accepted();
    NotebookGitBundleBuilder.append(
        accepted.repository(),
        accepted.head(),
        notebook.acceptedEntries(),
        entries,
        NotebookGitCutoverService.SYSTEM_AUTHOR_NAME,
        NotebookGitCutoverService.SYSTEM_AUTHOR_EMAIL,
        message,
        updatedAt.toInstant());
    repositoryStore.store(notebook.binding(), accepted.repository(), updatedAt);
  }

  private List<PortableTreeEntry> snapshot(NotebookGitBinding binding) {
    return livePortableTree.entriesOf(binding.getNotebook());
  }
}
