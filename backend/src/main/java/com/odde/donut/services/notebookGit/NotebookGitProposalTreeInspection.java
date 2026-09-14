package com.odde.donut.services.notebookGit;

import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.ChangeKind;
import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.ChangedDocument;
import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.DocumentRole;
import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.InspectedRegularFile;
import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.unsupportedTreeShape;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Two-tree walk of safe regular files between commits, plus classification of changed documents by
 * Git operation and container/concept role.
 */
final class NotebookGitProposalTreeInspection {

  private NotebookGitProposalTreeInspection() {}

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
}
