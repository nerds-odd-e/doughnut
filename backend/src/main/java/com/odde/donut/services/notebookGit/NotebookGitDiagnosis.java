package com.odde.donut.services.notebookGit;

import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.AuthorizationService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** A guard-equivalent, consistent comparison of accepted Git and live Portable content. */
@Service
public class NotebookGitDiagnosis {
  private final NotebookGitStateLoader stateLoader;
  private final NotebookGitAcceptedRepositoryStore repositoryStore;
  private final NotebookGitProjection projection;
  private final AuthorizationService authorizationService;

  public NotebookGitDiagnosis(
      NotebookGitStateLoader stateLoader,
      NotebookGitAcceptedRepositoryStore repositoryStore,
      NotebookGitProjection projection,
      AuthorizationService authorizationService) {
    this.stateLoader = stateLoader;
    this.repositoryStore = repositoryStore;
    this.projection = projection;
    this.authorizationService = authorizationService;
  }

  @Transactional(
      propagation = Propagation.REQUIRES_NEW,
      isolation = Isolation.SERIALIZABLE,
      rollbackFor = Exception.class)
  public Report inspect(Integer notebookId) throws UnexpectedNoAccessRightException {
    var state =
        stateLoader
            .findByNotebookIdForUpdate(notebookId)
            .orElseThrow(NotebookGitBindingMissing::refusal);
    authorizationService.assertAuthorization(state.notebook());
    try (var accepted = repositoryStore.open(state.binding())) {
      var repository = accepted.repository();
      ObjectId head = accepted.head();
      Map<String, ObjectId> acceptedBlobs = NotebookGitAcceptedTree.blobIds(repository, head);
      NotebookGitTreeContent live =
          projection.liveTree(
              state.notebook(), state.folders(), state.storedNotes(), repository, head);
      TreeSet<String> paths = new TreeSet<>(acceptedBlobs.keySet());
      paths.addAll(live.blobIds().keySet());
      List<Difference> differences =
          paths.stream()
              .filter(path -> !Objects.equals(acceptedBlobs.get(path), live.blobIds().get(path)))
              .map(path -> {
                ObjectId before = acceptedBlobs.get(path);
                ObjectId after = live.blobIds().get(path);
                try {
                  return new Difference(
                      path,
                      before == null ? null : before.name(),
                      after == null ? null : after.name(),
                      before == null ? null : repository.open(before).getBytes(),
                      after == null ? null : live.blobs().get(after));
                } catch (IOException e) {
                  throw new UncheckedIOException("Could not read differing accepted content", e);
                }
              })
              .toList();
      return new Report(
          head.name(),
          NotebookGitAcceptedTree.rootTreeId(repository, head).name(),
          live.treeId().name(),
          differences);
    }
  }

  public record Report(
      String acceptedHead, String acceptedTree, String liveTree, List<Difference> differences) {}

  public record Difference(
      String path,
      @Schema(nullable = true) String acceptedBlobId,
      @Schema(nullable = true) String liveBlobId,
      @Schema(nullable = true, description = "Exact Git blob bytes, base64 encoded")
          byte[] acceptedContent,
      @Schema(nullable = true, description = "Exact Git blob bytes, base64 encoded")
          byte[] liveContent) {}
}
