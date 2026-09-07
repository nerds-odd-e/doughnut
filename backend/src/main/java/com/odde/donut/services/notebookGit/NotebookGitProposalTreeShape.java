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
 * Walks the raw two-tree diff between a proposal's accepted-parent commit and its proposed commit,
 * and permits one modified note, a set containing added ordinary Markdown notes at regular file
 * modes, exactly one isolated ordinary-note deletion, or exactly one same-parent equal-content
 * rename (one removed and one added note sharing a blob in the same directory) - never mixed,
 * multiple, or cross-parent removed/added pairs, unsafe paths, non-regular modes, or the
 * folder-reserved {@code README.md}. Callers only invoke this once proposal ancestry is confirmed
 * to be a direct single-parent child of the accepted commit.
 */
public final class NotebookGitProposalTreeShape {

  private NotebookGitProposalTreeShape() {}

  /**
   * @return the note changes, once every constraint holds
   * @throws ResponseStatusException 400 BAD_REQUEST naming the offending path/reason when the tree
   *     shape is unsupported, or when either commit cannot be inspected
   */
  public static List<NoteChange> requireRegularNoteChanges(
      Repository repository, ObjectId acceptedHead, ObjectId proposedHead) {
    try (RevWalk revWalk = new RevWalk(repository)) {
      RevCommit acceptedCommit = revWalk.parseCommit(acceptedHead);
      RevCommit proposedCommit = revWalk.parseCommit(proposedHead);
      return walkTreeShape(repository, acceptedCommit, proposedCommit);
    } catch (IOException e) {
      throw unsupportedTreeShape("proposal tree could not be inspected", e);
    }
  }

  private static List<NoteChange> walkTreeShape(
      Repository repository, RevCommit acceptedCommit, RevCommit proposedCommit)
      throws IOException {
    try (TreeWalk walk = new TreeWalk(repository)) {
      walk.addTree(acceptedCommit.getTree());
      walk.addTree(proposedCommit.getTree());
      walk.setRecursive(true);

      List<NoteChange> changes = new ArrayList<>();
      while (walk.next()) {
        String path = walk.getPathString();
        assertPathIsSafe(path);

        FileMode acceptedMode = walk.getFileMode(0);
        FileMode proposedMode = walk.getFileMode(1);
        if (FileMode.MISSING.equals(proposedMode)) {
          if (!FileMode.REGULAR_FILE.equals(acceptedMode)) {
            throw unsupportedTreeShape("path \"" + path + "\" is not a regular file mode");
          }
          changes.add(new NoteChange(path, ChangeKind.DELETED, walk.getObjectId(0), null));
          continue;
        }
        if (FileMode.MISSING.equals(acceptedMode)) {
          if (!FileMode.REGULAR_FILE.equals(proposedMode)) {
            throw unsupportedTreeShape("path \"" + path + "\" is not a regular file mode");
          }
          changes.add(new NoteChange(path, ChangeKind.ADDED, walk.getObjectId(1), null));
          continue;
        }
        if (!FileMode.REGULAR_FILE.equals(acceptedMode)
            || !FileMode.REGULAR_FILE.equals(proposedMode)) {
          throw unsupportedTreeShape("path \"" + path + "\" is not a regular file mode");
        }

        if (!walk.getObjectId(0).equals(walk.getObjectId(1))) {
          changes.add(new NoteChange(path, ChangeKind.MODIFIED, walk.getObjectId(1), null));
        }
      }

      return requireAllowedNoteChanges(changes);
    }
  }

  private static List<NoteChange> requireAllowedNoteChanges(List<NoteChange> changes) {
    for (NoteChange change : changes) {
      assertRegularNotePath(change.path());
    }
    if (changes.isEmpty()) {
      throw unsupportedTreeShape("proposal contains no changed file");
    }
    List<NoteChange> renameDetected = detectSameParentRename(changes);
    if (renameDetected != null) {
      return renameDetected;
    }
    if (changes.stream().anyMatch(change -> change.kind() == ChangeKind.DELETED)
        && changes.size() > 1) {
      throw unsupportedTreeShape(
          "publish each removed note in an isolated deletion commit, or an isolated same-parent"
              + " rename with unchanged content, without other file changes");
    }
    if (changes.size() > 1
        && changes.stream().noneMatch(change -> change.kind() == ChangeKind.ADDED)) {
      throw unsupportedTreeShape(
          "multiple changed files: "
              + String.join(", ", changes.stream().limit(2).map(NoteChange::path).toList())
              + ". Edits-only proposals require separate commits for each note");
    }

    return changes;
  }

  /**
   * Recognizes the one rename shape this proposal type accepts: a proposal containing exactly one
   * removed and one added ordinary note, with identical blob content, in the same parent directory.
   * Returns {@code null} when the proposal does not match this shape, so callers fall back to the
   * existing removal/addition eligibility rules.
   */
  private static List<NoteChange> detectSameParentRename(List<NoteChange> changes) {
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
    if (!parentDirectory(deleted.path()).equals(parentDirectory(added.path()))) {
      return null;
    }
    return List.of(
        new NoteChange(added.path(), ChangeKind.RENAMED, added.blobId(), deleted.path()));
  }

  private static String parentDirectory(String path) {
    int lastSlash = path.lastIndexOf('/');
    return lastSlash < 0 ? "" : path.substring(0, lastSlash + 1);
  }

  private static void assertRegularNotePath(String changedPath) {
    if (!changedPath.endsWith(".md")) {
      throw unsupportedTreeShape("path \"" + changedPath + "\" is not a Markdown note");
    }
    if ("README.md".equals(basename(changedPath))) {
      throw unsupportedTreeShape(
          "path \"" + changedPath + "\" is a folder README, which is reserved");
    }
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

  private static ResponseStatusException unsupportedTreeShape(String reason) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported tree shape: " + reason);
  }

  private static ResponseStatusException unsupportedTreeShape(String reason, Throwable cause) {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Unsupported tree shape: " + reason, cause);
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
}
