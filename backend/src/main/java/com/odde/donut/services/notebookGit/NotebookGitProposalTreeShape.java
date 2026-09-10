package com.odde.donut.services.notebookGit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Walks the raw two-tree diff between a proposal's accepted-parent commit and its proposed commit.
 * Changed documents are classified once by operation and container/concept role; unchanged accepted
 * files remain context. Ordinary-note admission permits added and/or modified ordinary Markdown
 * notes at regular file modes, any number of ordinary-note deletions alone or with same-path edits,
 * or exactly one isolated equal-content rename (one removed and one added note sharing a blob).
 * Mixing removals with additions is refused when identity correspondence is uncertain. Unsafe
 * paths, non-regular modes, or a changed folder-reserved {@code README.md} are refused. Callers
 * only invoke this once proposal ancestry is confirmed to be a direct single-parent child of the
 * accepted commit.
 */
public final class NotebookGitProposalTreeShape {

  private NotebookGitProposalTreeShape() {}

  static List<NoteChange> requireAllowedNoteChanges(List<ChangedDocument> documents) {
    return admitOrdinaryNoteChanges(noteChangesFrom(documents));
  }

  static boolean isAdditionOnly(List<ChangedDocument> documents) {
    return !documents.isEmpty()
        && documents.stream().allMatch(document -> document.kind() == ChangeKind.ADDED);
  }

  /**
   * Changed documents only: equal accepted/proposed blobs stay on {@link InspectedRegularFile} as
   * context and are omitted here.
   */
  static List<ChangedDocument> classifyChangedDocuments(List<InspectedRegularFile> files) {
    List<ChangedDocument> documents = new ArrayList<>();
    for (InspectedRegularFile file : files) {
      ChangeKind kind = changeKind(file);
      if (kind == null) {
        continue;
      }
      documents.add(new ChangedDocument(file, kind, documentRole(file.path())));
    }
    return documents;
  }

  /**
   * Walks both trees for every safe regular file, including unchanged paths and folder READMEs.
   * Missing side blobs are {@code null}. Path-safety and regular-file-mode refusals match the note
   * classification path; note-path eligibility is not applied here.
   */
  static List<InspectedRegularFile> inspectRegularFiles(
      Repository repository, ObjectId acceptedHead, ObjectId proposedHead) {
    try (RevWalk revWalk = new RevWalk(repository)) {
      RevCommit acceptedCommit = revWalk.parseCommit(acceptedHead);
      RevCommit proposedCommit = revWalk.parseCommit(proposedHead);
      return walkRegularFiles(repository, acceptedCommit, proposedCommit);
    } catch (IOException e) {
      throw unsupportedTreeShape("proposal tree could not be inspected", e);
    }
  }

  private static List<InspectedRegularFile> walkRegularFiles(
      Repository repository, RevCommit acceptedCommit, RevCommit proposedCommit)
      throws IOException {
    try (TreeWalk walk = new TreeWalk(repository)) {
      walk.addTree(acceptedCommit.getTree());
      walk.addTree(proposedCommit.getTree());
      walk.setRecursive(true);

      List<InspectedRegularFile> files = new ArrayList<>();
      while (walk.next()) {
        String path = walk.getPathString();
        assertPathIsSafe(path);

        FileMode acceptedMode = walk.getFileMode(0);
        FileMode proposedMode = walk.getFileMode(1);
        if (FileMode.MISSING.equals(proposedMode)) {
          requireRegularFileMode(path, acceptedMode);
          files.add(new InspectedRegularFile(path, walk.getObjectId(0), null));
          continue;
        }
        if (FileMode.MISSING.equals(acceptedMode)) {
          requireRegularFileMode(path, proposedMode);
          files.add(new InspectedRegularFile(path, null, walk.getObjectId(1)));
          continue;
        }
        requireRegularFileMode(path, acceptedMode);
        requireRegularFileMode(path, proposedMode);
        files.add(new InspectedRegularFile(path, walk.getObjectId(0), walk.getObjectId(1)));
      }
      return files;
    }
  }

  private static void requireRegularFileMode(String path, FileMode mode) {
    if (!FileMode.REGULAR_FILE.equals(mode)) {
      throw unsupportedTreeShape("path \"" + path + "\" is not a regular file mode");
    }
  }

  private static List<NoteChange> noteChangesFrom(List<ChangedDocument> documents) {
    List<NoteChange> changes = new ArrayList<>();
    for (ChangedDocument document : documents) {
      if (document.role() == DocumentRole.CONTAINER) {
        throw unsupportedTreeShape(
            "path \"" + document.path() + "\" is a folder README, which is reserved");
      }
      if (!document.path().endsWith(".md")) {
        throw unsupportedTreeShape("path \"" + document.path() + "\" is not a Markdown note");
      }
      changes.add(new NoteChange(document.path(), document.kind(), document.blobId(), null));
    }
    return changes;
  }

  /**
   * Accepts added and/or modified ordinary-note changes, any number of deletions alone or with
   * same-path edits, or one isolated equal-content rename. Refuses mixing removals with additions
   * when identity is uncertain (unequal blobs, equal-blob pairs with companion edits, or ambiguous
   * multiple equal-blob candidates).
   */
  private static List<NoteChange> admitOrdinaryNoteChanges(List<NoteChange> changes) {
    if (changes.isEmpty()) {
      throw unsupportedTreeShape("proposal contains no changed file");
    }
    List<NoteChange> renameDetected = detectEqualBlobRename(changes);
    if (renameDetected != null) {
      return renameDetected;
    }
    refuseUncertainRemovalAndAdditionMixtures(changes);
    return changes;
  }

  /**
   * Recognizes the one rename shape this proposal type accepts: a proposal containing exactly one
   * removed and one added ordinary note with identical blob content. Returns {@code null} when the
   * proposal does not match this shape, so callers fall back to deletion/edit admission and
   * removal/addition refusal rules.
   */
  private static List<NoteChange> detectEqualBlobRename(List<NoteChange> changes) {
    if (changes.size() != 2) {
      return null;
    }
    NoteChange deleted =
        changes.stream()
            .filter(change -> change.kind() == ChangeKind.DELETED)
            .findFirst()
            .orElse(null);
    NoteChange added =
        changes.stream()
            .filter(change -> change.kind() == ChangeKind.ADDED)
            .findFirst()
            .orElse(null);
    if (deleted == null || added == null) {
      return null;
    }
    if (!deleted.blobId().equals(added.blobId())) {
      return null;
    }
    return List.of(
        new NoteChange(added.path(), ChangeKind.RENAMED, added.blobId(), deleted.path()));
  }

  private static void refuseUncertainRemovalAndAdditionMixtures(List<NoteChange> changes) {
    boolean hasDeleted = changes.stream().anyMatch(change -> change.kind() == ChangeKind.DELETED);
    boolean hasAdded = changes.stream().anyMatch(change -> change.kind() == ChangeKind.ADDED);
    if (!hasDeleted || !hasAdded) {
      return;
    }
    throw unsupportedTreeShape(
        "separate identity-changing work: publish an equal-content rename alone, or delete and"
            + " create notes in separate commits rather than mixing removals with additions");
  }

  private static ChangeKind changeKind(InspectedRegularFile file) {
    if (file.proposedBlobId() == null) {
      return ChangeKind.DELETED;
    }
    if (file.acceptedBlobId() == null) {
      return ChangeKind.ADDED;
    }
    if (!file.acceptedBlobId().equals(file.proposedBlobId())) {
      return ChangeKind.MODIFIED;
    }
    return null;
  }

  private static DocumentRole documentRole(String path) {
    return "README.md".equals(basename(path)) ? DocumentRole.CONTAINER : DocumentRole.CONCEPT;
  }

  private static void assertPathIsSafe(String path) {
    if (path.isEmpty() || path.startsWith("/")) {
      throw unsupportedTreeShape("path \"" + path + "\" is unsafe");
    }
    for (String segment : path.split("/")) {
      if (segment.equals(".") || segment.equals("..")) {
        throw unsupportedTreeShape("path \"" + path + "\" is unsafe");
      }
    }
  }

  private static String basename(String path) {
    int lastSlash = path.lastIndexOf('/');
    return lastSlash < 0 ? path : path.substring(lastSlash + 1);
  }

  static ResponseStatusException unsupportedTreeShape(String reason) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported tree shape: " + reason);
  }

  private static ResponseStatusException unsupportedTreeShape(String reason, Throwable cause) {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Unsupported tree shape: " + reason, cause);
  }

  /**
   * One regular file present on either side of the two-tree walk. A missing side's blob is {@code
   * null}; equal non-null blobs are an unchanged file.
   */
  record InspectedRegularFile(String path, ObjectId acceptedBlobId, ObjectId proposedBlobId) {}

  /**
   * One changed document from the inspected diff, with Git operation and container/concept role.
   * Unchanged accepted files are not represented here.
   */
  record ChangedDocument(InspectedRegularFile file, ChangeKind kind, DocumentRole role) {
    String path() {
      return file.path();
    }

    ObjectId blobId() {
      return kind == ChangeKind.DELETED ? file.acceptedBlobId() : file.proposedBlobId();
    }
  }

  /**
   * @param path the current (proposed-tree) Portable path; for RENAMED this is the new path
   * @param blobId the raw blob object id relevant to this change: the added blob (proposed tree)
   *     for ADDED, the removed blob (accepted tree) for DELETED, the proposed blob for MODIFIED
   *     (not meaningfully used by callers today), and the shared blob for RENAMED
   * @param fromPath the original (accepted-tree) Portable path being renamed from; present only for
   *     RENAMED, {@code null} otherwise
   */
  record NoteChange(String path, ChangeKind kind, ObjectId blobId, String fromPath) {}

  enum ChangeKind {
    ADDED,
    MODIFIED,
    DELETED,
    RENAMED
  }

  enum DocumentRole {
    CONTAINER,
    CONCEPT
  }
}
