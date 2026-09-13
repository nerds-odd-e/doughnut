package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import java.sql.Timestamp;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Service;

/** Persists a complete Portable-tree snapshot as a new accepted Git tip. */
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
    appendTip(accepted, entries, accepted.mainHead(), message, updatedAt);
    writeBinding(accepted, binding, updatedAt);
    entityPersister.save(binding);
  }

  private static void appendTip(
      NotebookGitBundleImporter.ImportedBundle accepted,
      List<PortableTreeEntry> entries,
      ObjectId parent,
      String message,
      Timestamp updatedAt) {
    NotebookGitBundleBuilder.append(
        accepted.repository(),
        parent,
        entries,
        NotebookGitCutoverService.SYSTEM_AUTHOR_NAME,
        NotebookGitCutoverService.SYSTEM_AUTHOR_EMAIL,
        message,
        updatedAt.toInstant());
  }

  private void writeBinding(
      NotebookGitBundleImporter.ImportedBundle accepted,
      NotebookGitBinding binding,
      Timestamp updatedAt) {
    NotebookGitBundleWriter.BundleWriteResult written =
        NotebookGitBundleWriter.write(accepted.repository());
    binding.setAcceptedGitObjectId(written.headObjectId());
    binding.setBundleBytes(written.bundleBytes());
    binding.setUpdatedAt(updatedAt);
  }
}
