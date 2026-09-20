package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookGit.NotebookGitBundleWriter.BundleWriteResult;
import java.sql.Timestamp;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Service;

/**
 * Owns the accepted Git repository's bundle-backed persistence mechanics on behalf of every caller
 * that opens or stores a {@link NotebookGitBinding}'s accepted history: ordinary web changes,
 * proposal publication, creation/cutover/reset and bundle download. Bundle bytes remain the sole
 * durable representation; this class only centralizes the open/store mechanics so a later storage
 * change can happen once, beneath every caller.
 */
@Service
class NotebookGitAcceptedRepositoryStore {

  private final EntityPersister entityPersister;

  NotebookGitAcceptedRepositoryStore(EntityPersister entityPersister) {
    this.entityPersister = entityPersister;
  }

  /** An accepted repository imported from stored bundle bytes, open for the caller's use. */
  record OpenedAcceptedRepository(Repository repository, ObjectId head) implements AutoCloseable {
    @Override
    public void close() {
      repository.close();
    }
  }

  /**
   * Imports {@code binding}'s stored bundle bytes into an openable JGit repository, verifying the
   * imported head matches the binding's persisted accepted Git object ID.
   */
  OpenedAcceptedRepository open(NotebookGitBinding binding) {
    ObjectId persistedAcceptedHead = ObjectId.fromString(binding.getAcceptedGitObjectId());
    NotebookGitBundleImporter.ImportedBundle accepted =
        NotebookGitBundleImporter.importMainHead(binding.getBundleBytes(), "accepted-bundle");
    if (!accepted.mainHead().equals(persistedAcceptedHead)) {
      accepted.close();
      throw new IllegalStateException("Accepted bundle main does not match its persisted head");
    }
    return new OpenedAcceptedRepository(accepted.repository(), accepted.mainHead());
  }

  /**
   * Writes {@code repository}'s {@code main} head into bundle bytes, sets the accepted Git object
   * ID/bundle bytes/updated-at onto {@code binding}, and persists the binding. Returns the new
   * accepted head's Git object ID.
   */
  String store(NotebookGitBinding binding, Repository repository, Timestamp updatedAt) {
    String headObjectId = apply(binding, NotebookGitBundleWriter.write(repository), updatedAt);
    entityPersister.save(binding);
    return headObjectId;
  }

  /**
   * Sets the accepted Git object ID/bundle bytes/updated-at from an already-written bundle onto
   * {@code binding}, without persisting it. For callers that own their own persistence call (for
   * example creation/cutover/reset, which save through {@code NotebookGitBindingRepository}
   * alongside other binding fields).
   */
  String apply(NotebookGitBinding binding, BundleWriteResult written, Timestamp updatedAt) {
    binding.setAcceptedGitObjectId(written.headObjectId());
    binding.setBundleBytes(written.bundleBytes());
    binding.setUpdatedAt(updatedAt);
    return written.headObjectId();
  }

  /**
   * Opens {@code binding}'s accepted repository and re-serializes its {@code main} head into a
   * fresh, complete, cloneable bundle for transport download. The download's transport contract is
   * a reachable, cloneable bundle, not byte-for-byte equality with the stored representation.
   */
  byte[] downloadableBundle(NotebookGitBinding binding) {
    try (OpenedAcceptedRepository accepted = open(binding)) {
      return NotebookGitBundleWriter.write(accepted.repository()).bundleBytes();
    }
  }
}
