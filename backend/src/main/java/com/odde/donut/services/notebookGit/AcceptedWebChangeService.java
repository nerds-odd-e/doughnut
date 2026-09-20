package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookExport.NotebookLivePortableTree;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
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
  private final NotebookGitProjection projection;
  private final NotebookGitBindingRepository bindingRepository;
  private final NotebookLivePortableTree livePortableTree;
  private final EntityPersister entityPersister;

  public AcceptedWebChangeService(
      NotebookGitProjection projection,
      NotebookGitBindingRepository bindingRepository,
      NotebookLivePortableTree livePortableTree,
      EntityPersister entityPersister) {
    this.projection = projection;
    this.bindingRepository = bindingRepository;
    this.livePortableTree = livePortableTree;
    this.entityPersister = entityPersister;
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
      NotebookGitBundleImporter.ImportedBundle accepted,
      List<PortableTreeEntry> acceptedEntries,
      boolean matchedBefore) {}

  private OpenedNotebook open(NotebookGitBinding binding) {
    ObjectId persistedAcceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    NotebookGitBundleImporter.ImportedBundle accepted =
        NotebookGitBundleImporter.importMainHead(binding.getBundleBytes(), "accepted-bundle");
    if (!accepted.mainHead().equals(persistedAcceptedHead)) {
      accepted.close();
      throw new IllegalStateException("Accepted bundle main does not match its persisted head");
    }
    List<PortableTreeEntry> acceptedEntries =
        NotebookGitAcceptedTree.readEntries(accepted.repository(), accepted.mainHead());
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
    NotebookGitBundleImporter.ImportedBundle accepted = notebook.accepted();
    NotebookGitBundleBuilder.append(
        accepted.repository(),
        accepted.mainHead(),
        notebook.acceptedEntries(),
        entries,
        NotebookGitCutoverService.SYSTEM_AUTHOR_NAME,
        NotebookGitCutoverService.SYSTEM_AUTHOR_EMAIL,
        message,
        updatedAt.toInstant());
    NotebookGitBundleWriter.BundleWriteResult written =
        NotebookGitBundleWriter.write(accepted.repository());
    NotebookGitBinding binding = notebook.binding();
    binding.setAcceptedGitObjectId(written.headObjectId());
    binding.setBundleBytes(written.bundleBytes());
    binding.setUpdatedAt(updatedAt);
    entityPersister.save(binding);
  }

  private List<PortableTreeEntry> snapshot(NotebookGitBinding binding) {
    return livePortableTree.entriesOf(binding.getNotebook());
  }
}
