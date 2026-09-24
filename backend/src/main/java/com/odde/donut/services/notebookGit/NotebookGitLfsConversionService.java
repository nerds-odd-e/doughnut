package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.notebookGit.NotebookGitAcceptedRepositoryStore.OpenedAcceptedRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Converts one notebook still on legacy raw attachment storage to LFS with one forward Donut System
 * commit on its accepted head: the accepted tree with the initial {@code .gitattributes}. Earlier
 * commits stay untouched. The binding is locked and re-checked, so a notebook already on LFS is
 * left as it is.
 */
@Service
public class NotebookGitLfsConversionService {

  static final String COMMIT_MESSAGE = "Store notebook files with Git LFS";

  private final NotebookGitBindingRepository bindingRepository;
  private final NotebookGitAcceptedRepositoryStore repositoryStore;

  public NotebookGitLfsConversionService(
      NotebookGitBindingRepository bindingRepository,
      NotebookGitAcceptedRepositoryStore repositoryStore) {
    this.bindingRepository = bindingRepository;
    this.repositoryStore = repositoryStore;
  }

  @Transactional
  public void convert(Integer notebookId, Instant conversionTime) {
    NotebookGitBinding binding =
        bindingRepository.findByNotebookIdForUpdate(notebookId).orElseThrow();
    if (binding.getAttachmentRepresentation() == NotebookGitAttachmentRepresentation.LFS) {
      return;
    }
    try (OpenedAcceptedRepository accepted = repositoryStore.open(binding)) {
      ObjectId head =
          NotebookGitCommitBuilder.append(
              accepted.repository(),
              accepted.head(),
              lfsTree(accepted),
              NotebookGitCutoverService.SYSTEM_AUTHOR_NAME,
              NotebookGitCutoverService.SYSTEM_AUTHOR_EMAIL,
              COMMIT_MESSAGE,
              conversionTime);
      binding.setAttachmentRepresentation(NotebookGitAttachmentRepresentation.LFS);
      repositoryStore.store(binding, accepted.repository(), head, Timestamp.from(conversionTime));
    }
  }

  private static NotebookGitTreeContent lfsTree(OpenedAcceptedRepository accepted) {
    try (ObjectReader objectReader = accepted.repository().newObjectReader()) {
      NotebookGitDirectoryTree tree =
          NotebookGitDirectoryTree.fromAcceptedRoot(
              objectReader,
              NotebookGitAcceptedTree.rootTreeId(accepted.repository(), accepted.head()));
      Map<ObjectId, byte[]> blobs = new HashMap<>();
      NotebookGitTreeContent.putEntry(NotebookGitAttributes.initialEntry(), tree, blobs);
      return NotebookGitTreeContent.fromDirectory(tree, blobs);
    }
  }
}
