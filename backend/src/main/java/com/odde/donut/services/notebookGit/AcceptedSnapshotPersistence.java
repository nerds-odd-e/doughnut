package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Appends a complete Portable-tree snapshot as the next accepted Git commit and persists the
 * binding's accepted head and bundle bytes in the caller's transaction.
 */
@Service
public class AcceptedSnapshotPersistence {
  private final EntityPersister entityPersister;

  public AcceptedSnapshotPersistence(EntityPersister entityPersister) {
    this.entityPersister = entityPersister;
  }

  public void persist(
      NotebookGitBundleImporter.ImportedBundle accepted,
      List<PortableTreeEntry> entries,
      NotebookGitBinding binding,
      Timestamp updatedAt,
      String message) {
    NotebookGitBundleBuilder.append(
        accepted.repository(),
        accepted.mainHead(),
        entries,
        NotebookGitCutoverService.SYSTEM_AUTHOR_NAME,
        NotebookGitCutoverService.SYSTEM_AUTHOR_EMAIL,
        message,
        updatedAt.toInstant());
    NotebookGitBundleWriter.BundleWriteResult written =
        NotebookGitBundleWriter.write(accepted.repository());
    binding.setAcceptedGitObjectId(written.headObjectId());
    binding.setBundleBytes(written.bundleBytes());
    binding.setUpdatedAt(updatedAt);
    entityPersister.save(binding);
  }
}
