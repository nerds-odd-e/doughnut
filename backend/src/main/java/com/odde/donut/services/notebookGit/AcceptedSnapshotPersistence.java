package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Service;

/**
 * Persists a complete Portable-tree snapshot as the accepted Git tip and updates the binding's
 * accepted head and bundle bytes in the caller's transaction. Ordinary web content edits may
 * replace an unexposed same-note tip within the amendment window; other writers always append.
 */
@Service
public class AcceptedSnapshotPersistence {
  private static final Duration AMENDMENT_WINDOW = Duration.ofMinutes(10);

  private final EntityPersister entityPersister;

  public AcceptedSnapshotPersistence(EntityPersister entityPersister) {
    this.entityPersister = entityPersister;
  }

  /** Appends a new accepted tip. Does not register amendment eligibility. */
  public void persist(
      NotebookGitBundleImporter.ImportedBundle accepted,
      List<PortableTreeEntry> entries,
      NotebookGitBinding binding,
      Timestamp updatedAt,
      String message) {
    appendAndWrite(accepted, entries, binding, updatedAt, message);
  }

  /**
   * Appends or replaces the unexposed content tip for one ordinary-note edit, then registers the
   * new tip as the amendment candidate.
   */
  public void persistOrdinaryNoteContentEdit(
      NotebookGitBundleImporter.ImportedBundle accepted,
      List<PortableTreeEntry> entries,
      NotebookGitBinding binding,
      Timestamp updatedAt,
      String message,
      Integer noteId) {
    ObjectId currentHead = accepted.mainHead();
    if (eligibleToAmend(binding, currentHead, noteId, updatedAt)) {
      NotebookGitBundleBuilder.replaceTip(
          accepted.repository(), currentHead, entries, message, updatedAt.toInstant());
    } else {
      appendTip(accepted, entries, currentHead, message, updatedAt);
    }
    writeBinding(accepted, binding, updatedAt);
    binding.setAmendmentHead(binding.getAcceptedGitObjectId());
    binding.setAmendmentNoteId(noteId);
    binding.setAmendmentLastChangedAt(updatedAt);
    entityPersister.save(binding);
  }

  private void appendAndWrite(
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

  private static boolean eligibleToAmend(
      NotebookGitBinding binding, ObjectId currentHead, Integer noteId, Timestamp updatedAt) {
    String amendmentHead = binding.getAmendmentHead();
    Integer amendmentNoteId = binding.getAmendmentNoteId();
    Timestamp lastChangedAt = binding.getAmendmentLastChangedAt();
    if (amendmentHead == null || amendmentNoteId == null || lastChangedAt == null) {
      return false;
    }
    if (!amendmentHead.equals(currentHead.getName()) || !amendmentNoteId.equals(noteId)) {
      return false;
    }
    long elapsedMillis = updatedAt.getTime() - lastChangedAt.getTime();
    return elapsedMillis >= 0 && elapsedMillis < AMENDMENT_WINDOW.toMillis();
  }
}
