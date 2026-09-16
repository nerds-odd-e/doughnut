package com.odde.donut.services.notebookGit;

import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.ChangeKind;
import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.NoteChange;
import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.NoteOrigin;
import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.unsupportedTreeShape;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * Centralized JGit-backed rename detection for ordinary-note moves. Builds {@link DiffEntry}s from
 * DELETED/ADDED {@link NoteChange}s, runs {@link RenameDetector} at {@link
 * #RENAME_SCORE_THRESHOLD}, and maps detected RENAME entries back to {@link NoteOrigin}s keyed by
 * destination path.
 *
 * <p>Donut admission safeguards are kept here, outside JGit scoring: the ambiguous-blob refusal
 * runs before scoring so non-unique content correspondence is refused rather than silently paired
 * by the library, which would otherwise greedily pick one candidate from a 1:N / N:N same-blob
 * group. Uncertain-identity refusal is delegated to {@link
 * NotebookGitProposalIdentityRefusal#refuseUncertainIdentityCorrespondence(java.util.List)} so the
 * one safeguard message keeps a single home.
 */
final class NotebookGitProposalRenameDetector {

  /**
   * Minimum JGit content similarity for an ordinary-note rename match. 50 enables changed-content
   * rename inference (Git's documented {@code -M} default) while staying below JGit's library
   * default of 60 so the configured policy is distinguishable from the library default.
   */
  static final int RENAME_SCORE_THRESHOLD = 50;

  private NotebookGitProposalRenameDetector() {}

  /**
   * Tip residual / adjacent-step JGit-scored moves without range origin walking. Returns the
   * detected destination-to-origin map; callers pair it with {@link
   * NotebookGitProposalNoteCorrespondence#replaceMatchedAdditionsWithRenames} to rewrite the change
   * list.
   */
  static Map<String, NoteOrigin> detectRenames(Repository repository, List<NoteChange> changes) {
    refuseAmbiguousBlobCorrespondence(changes);
    List<DiffEntry> entries = new ArrayList<>();
    for (NoteChange change : changes) {
      if (change.kind() == ChangeKind.DELETED) {
        entries.add(NoteChangeDiffEntry.deletion(change.path(), change.blobId()));
      } else if (change.kind() == ChangeKind.ADDED) {
        entries.add(NoteChangeDiffEntry.addition(change.path(), change.blobId()));
      }
    }
    if (entries.isEmpty()) {
      return Map.of();
    }
    RenameDetector detector = new RenameDetector(repository);
    detector.addAll(entries);
    detector.setRenameScore(RENAME_SCORE_THRESHOLD);
    List<DiffEntry> detected;
    try {
      detected = detector.compute();
    } catch (IOException e) {
      throw unsupportedTreeShape("identity correspondence could not be scored", e);
    }
    Map<String, NoteOrigin> originsByDestination = new HashMap<>();
    for (DiffEntry entry : detected) {
      if (entry.getChangeType() != DiffEntry.ChangeType.RENAME) {
        continue;
      }
      originsByDestination.put(
          entry.getNewPath(), new NoteOrigin(entry.getOldPath(), entry.getOldId().toObjectId()));
    }
    return originsByDestination;
  }

  /**
   * Preserved ambiguity safeguard: refuse when a blob appears on both sides with anything other
   * than a unique one-to-one correspondence. JGit would otherwise pair one candidate and leave the
   * rest; Donut keeps the existing refusal so uncertain identity is not silently resolved.
   */
  private static void refuseAmbiguousBlobCorrespondence(List<NoteChange> changes) {
    Map<ObjectId, List<NoteChange>> removalsByBlob = changesByBlob(changes, ChangeKind.DELETED);
    Map<ObjectId, List<NoteChange>> additionsByBlob = changesByBlob(changes, ChangeKind.ADDED);
    for (Map.Entry<ObjectId, List<NoteChange>> removalGroup : removalsByBlob.entrySet()) {
      List<NoteChange> additionGroup = additionsByBlob.get(removalGroup.getKey());
      if (additionGroup == null) {
        continue;
      }
      if (removalGroup.getValue().size() != 1 || additionGroup.size() != 1) {
        List<String> affectedPaths = new ArrayList<>();
        removalGroup.getValue().forEach(change -> affectedPaths.add(change.path()));
        additionGroup.forEach(change -> affectedPaths.add(change.path()));
        NotebookGitProposalIdentityRefusal.refuseUncertainIdentityCorrespondence(affectedPaths);
      }
    }
  }

  private static Map<ObjectId, List<NoteChange>> changesByBlob(
      List<NoteChange> changes, ChangeKind kind) {
    Map<ObjectId, List<NoteChange>> changesByBlob = new HashMap<>();
    for (NoteChange change : changes) {
      if (change.kind() == kind) {
        changesByBlob.computeIfAbsent(change.blobId(), ignored -> new ArrayList<>()).add(change);
      }
    }
    return changesByBlob;
  }

  /**
   * Adapter that builds {@link DiffEntry} instances from {@link NoteChange}s so {@link
   * RenameDetector} can score ordinary-note moves. {@link DiffEntry}'s factories are
   * package-private in {@code org.eclipse.jgit.diff}; this subclass reaches the protected
   * constructor and fields to synthesize ADD/DELETE entries with the proposal repository's blob
   * ids.
   */
  private static final class NoteChangeDiffEntry extends DiffEntry {
    static NoteChangeDiffEntry deletion(String path, ObjectId blobId) {
      return new NoteChangeDiffEntry(
          DiffEntry.ChangeType.DELETE, path, DiffEntry.DEV_NULL, blobId, ObjectId.zeroId());
    }

    static NoteChangeDiffEntry addition(String path, ObjectId blobId) {
      return new NoteChangeDiffEntry(
          DiffEntry.ChangeType.ADD, DiffEntry.DEV_NULL, path, ObjectId.zeroId(), blobId);
    }

    private NoteChangeDiffEntry(
        DiffEntry.ChangeType changeType,
        String oldPath,
        String newPath,
        ObjectId oldId,
        ObjectId newId) {
      super();
      this.changeType = changeType;
      this.oldPath = oldPath;
      this.newPath = newPath;
      this.oldId = AbbreviatedObjectId.fromObjectId(oldId);
      this.newId = AbbreviatedObjectId.fromObjectId(newId);
      this.oldMode =
          changeType == DiffEntry.ChangeType.ADD ? FileMode.MISSING : FileMode.REGULAR_FILE;
      this.newMode =
          changeType == DiffEntry.ChangeType.DELETE ? FileMode.MISSING : FileMode.REGULAR_FILE;
    }
  }
}
