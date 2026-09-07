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
 * Walks the raw two-tree diff (no rename detection) between a proposal's accepted-parent commit and
 * its proposed commit, and permits one modified note, a set containing added ordinary Markdown
 * notes at regular file modes, or exactly one isolated ordinary-note deletion - never mixed
 * deletions/moves, unsafe paths, non-regular modes, or the folder-reserved {@code README.md}.
 * Callers only invoke this once proposal ancestry is confirmed to be a direct single-parent child
 * of the accepted commit.
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
          changes.add(new NoteChange(path, ChangeKind.DELETED));
          continue;
        }
        if (FileMode.MISSING.equals(acceptedMode)) {
          if (!FileMode.REGULAR_FILE.equals(proposedMode)) {
            throw unsupportedTreeShape("path \"" + path + "\" is not a regular file mode");
          }
          changes.add(new NoteChange(path, ChangeKind.ADDED));
          continue;
        }
        if (!FileMode.REGULAR_FILE.equals(acceptedMode)
            || !FileMode.REGULAR_FILE.equals(proposedMode)) {
          throw unsupportedTreeShape("path \"" + path + "\" is not a regular file mode");
        }

        if (!walk.getObjectId(0).equals(walk.getObjectId(1))) {
          changes.add(new NoteChange(path, ChangeKind.MODIFIED));
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
    if (changes.stream().anyMatch(change -> change.kind() == ChangeKind.DELETED)
        && changes.size() > 1) {
      throw unsupportedTreeShape(
          "publish each removed note in an isolated deletion commit, without other file changes"
              + " (no rename detection is performed)");
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

  record NoteChange(String path, ChangeKind kind) {}

  enum ChangeKind {
    ADDED,
    MODIFIED,
    DELETED
  }
}
