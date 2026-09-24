package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookAttachment;
import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.NotebookAttachmentRepository;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookAttachment.VerifiedNotebookAttachmentBytes;
import com.odde.donut.services.notebookGit.NotebookGitAcceptedRepositoryStore.OpenedAcceptedRepository;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Converts one notebook still on legacy raw attachment storage to LFS with one forward Donut System
 * commit on its accepted head: the accepted tree with the initial {@code .gitattributes} and each
 * current non-empty file replaced by its LFS pointer, after its bytes are in the notebook's content
 * store. The attachment rows then hold the pointers. Empty files keep the standard empty
 * representation, and earlier commits stay untouched. The binding is locked and re-checked, so a
 * notebook already on LFS is left as it is.
 */
@Service
public class NotebookGitLfsConversionService {

  static final String COMMIT_MESSAGE = "Store notebook files with Git LFS";

  private final NotebookGitBindingRepository bindingRepository;
  private final NotebookGitAcceptedRepositoryStore repositoryStore;
  private final NotebookAttachmentRepository attachmentRepository;
  private final NotebookAttachmentContent attachmentContent;

  public NotebookGitLfsConversionService(
      NotebookGitBindingRepository bindingRepository,
      NotebookGitAcceptedRepositoryStore repositoryStore,
      NotebookAttachmentRepository attachmentRepository,
      NotebookAttachmentContent attachmentContent) {
    this.bindingRepository = bindingRepository;
    this.repositoryStore = repositoryStore;
    this.attachmentRepository = attachmentRepository;
    this.attachmentContent = attachmentContent;
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
              lfsTree(accepted, storeAsPointers(notebookId)),
              NotebookGitCutoverService.SYSTEM_AUTHOR_NAME,
              NotebookGitCutoverService.SYSTEM_AUTHOR_EMAIL,
              COMMIT_MESSAGE,
              conversionTime);
      binding.setAttachmentRepresentation(NotebookGitAttachmentRepresentation.LFS);
      repositoryStore.store(binding, accepted.repository(), head, Timestamp.from(conversionTime));
    }
  }

  /** Stores each non-empty file's bytes and turns its row into the pointer; returns the entries. */
  private List<PortableTreeEntry> storeAsPointers(Integer notebookId) {
    return attachmentRepository.findByNotebook_Id(notebookId).stream()
        .filter(attachment -> attachment.getAcceptedGitContent().length > 0)
        .map(attachment -> storeAsPointer(notebookId, attachment))
        .toList();
  }

  private PortableTreeEntry storeAsPointer(Integer notebookId, NotebookAttachment attachment) {
    byte[] bytes = attachment.getAcceptedGitContent();
    String digest = VerifiedNotebookAttachmentBytes.sha256Hex(bytes);
    try {
      attachmentContent.store(notebookId, digest, bytes.length, new ByteArrayInputStream(bytes));
    } catch (IOException e) {
      throw new UncheckedIOException("Could not store attachment content for LFS conversion", e);
    }
    byte[] pointer = NotebookGitLfsPointer.format(digest, bytes.length);
    attachment.setAcceptedGitContent(pointer);
    return new PortableTreeEntry(NotebookGitPortablePath.ofAttachment(attachment), pointer);
  }

  private static NotebookGitTreeContent lfsTree(
      OpenedAcceptedRepository accepted, List<PortableTreeEntry> pointers) {
    try (ObjectReader objectReader = accepted.repository().newObjectReader()) {
      NotebookGitDirectoryTree tree =
          NotebookGitDirectoryTree.fromAcceptedRoot(
              objectReader,
              NotebookGitAcceptedTree.rootTreeId(accepted.repository(), accepted.head()));
      Map<ObjectId, byte[]> blobs = new HashMap<>();
      NotebookGitTreeContent.putEntry(NotebookGitAttributes.initialEntry(), tree, blobs);
      pointers.forEach(pointer -> NotebookGitTreeContent.putEntry(pointer, tree, blobs));
      return NotebookGitTreeContent.fromDirectory(tree, blobs);
    }
  }
}
