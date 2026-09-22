package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookGit.NotebookGitAcceptedRepositoryStore.OpenedAcceptedRepository;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.NotebookProjectionChange;
import com.odde.donut.services.notebookGit.ProjectionChangeCapture.ProjectionChange;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcceptedWebChangeService {
  private final NotebookGitBindingRepository bindingRepository;
  private final EntityPersister entityPersister;
  private final NotebookGitAcceptedRepositoryStore repositoryStore;
  private final ProjectionChangeCapture projectionChangeCapture;
  private final NotebookGitTreeEncoder treeEncoder;

  public AcceptedWebChangeService(
      NotebookGitBindingRepository bindingRepository,
      EntityPersister entityPersister,
      NotebookGitAcceptedRepositoryStore repositoryStore,
      ProjectionChangeCapture projectionChangeCapture,
      NotebookGitTreeEncoder treeEncoder) {
    this.bindingRepository = bindingRepository;
    this.entityPersister = entityPersister;
    this.repositoryStore = repositoryStore;
    this.projectionChangeCapture = projectionChangeCapture;
    this.treeEncoder = treeEncoder;
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
    try (ProjectionChange change = projectionChangeCapture.open()) {
      for (Integer notebookId : new TreeSet<>(notebookIds)) {
        bindingRepository
            .findByNotebookIdForUpdate(notebookId)
            .ifPresent(binding -> opened.add(open(binding)));
      }
      T result = operation.run();
      if (!opened.isEmpty()) {
        entityPersister.flush();
        String message = commitMessage.apply(result);
        opened.forEach(notebook -> commitIfChanged(notebook, change, message, updatedAt));
      }
      return result;
    } finally {
      opened.forEach(notebook -> notebook.accepted().close());
    }
  }

  private record OpenedNotebook(NotebookGitBinding binding, OpenedAcceptedRepository accepted) {}

  private OpenedNotebook open(NotebookGitBinding binding) {
    return new OpenedNotebook(binding, repositoryStore.open(binding));
  }

  private void commitIfChanged(
      OpenedNotebook notebook, ProjectionChange change, String message, Timestamp updatedAt) {
    OpenedAcceptedRepository accepted = notebook.accepted();
    ObjectId acceptedRootTreeId =
        NotebookGitAcceptedTree.rootTreeId(accepted.repository(), accepted.head());
    NotebookProjectionChange notebookChange = change.of(notebook.binding().getNotebook().getId());
    NotebookGitTreeContent tree =
        treeEncoder.derive(notebookChange, accepted.repository(), acceptedRootTreeId);
    if (tree.treeId().equals(acceptedRootTreeId)) {
      return;
    }
    NotebookGitCommitBuilder.append(
        accepted.repository(),
        accepted.head(),
        tree,
        NotebookGitCutoverService.SYSTEM_AUTHOR_NAME,
        NotebookGitCutoverService.SYSTEM_AUTHOR_EMAIL,
        message,
        updatedAt.toInstant());
    repositoryStore.store(notebook.binding(), accepted.repository(), updatedAt);
  }
}
