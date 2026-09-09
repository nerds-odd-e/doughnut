package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.InspectedRegularFile;
import java.util.List;
import java.util.Optional;

/**
 * Recognizes exact root Folder-creation proposal shapes from inspected regular-file diffs: one
 * added root-level {@code Folder/README.md}; that plus one ordinary note inside the same Folder;
 * that Folder Readme plus one added root {@code README.md}; or those two Readmes plus one added
 * ordinary note directly inside that same Folder.
 */
final class NotebookGitProposalFolderCreationShape {

  private NotebookGitProposalFolderCreationShape() {}

  record RootFolderCreation(String readmePath) {}

  /**
   * Exactly one added root-level {@code Folder/README.md} plus exactly one added ordinary Note
   * directly inside that same Folder on an otherwise empty tree: no accepted blobs on those paths
   * and no other path present.
   */
  record RootFolderAndContainedNoteCreation(String folderReadmePath, String notePath) {}

  /**
   * Exactly one added root {@code README.md} plus exactly one added root-level {@code
   * Folder/README.md} on an otherwise empty tree: no accepted blobs on those paths and no other
   * path present (changed or unchanged).
   */
  record InitialNotebookAndRootFolderCreation(String notebookReadmePath, String folderReadmePath) {}

  /**
   * Exactly the two-README initial tree plus one added ordinary {@code .md} note whose parent path
   * is that same new root Folder.
   */
  record InitialNotebookRootFolderAndNoteCreation(
      String notebookReadmePath, String folderReadmePath, String notePath) {}

  static Optional<RootFolderCreation> findSingleRootFolderCreation(
      List<InspectedRegularFile> files) {
    RootFolderCreation candidate = null;
    for (InspectedRegularFile file : files) {
      if (unchanged(file)) {
        continue;
      }
      if (candidate != null || !isAddedRootFolderReadme(file)) {
        return Optional.empty();
      }
      candidate = new RootFolderCreation(file.path());
    }
    return Optional.ofNullable(candidate);
  }

  static Optional<RootFolderAndContainedNoteCreation> findRootFolderAndContainedNoteCreation(
      List<InspectedRegularFile> files) {
    return collectInitialAddedPaths(files)
        .filter(paths -> paths.notebookReadmePath() == null && paths.notePath() != null)
        .filter(paths -> paths.notePath().startsWith(folderPrefix(paths.folderReadmePath())))
        .map(
            paths ->
                new RootFolderAndContainedNoteCreation(paths.folderReadmePath(), paths.notePath()));
  }

  static Optional<InitialNotebookAndRootFolderCreation> findInitialNotebookAndRootFolderCreation(
      List<InspectedRegularFile> files) {
    return collectInitialAddedPaths(files)
        .filter(paths -> paths.notebookReadmePath() != null && paths.notePath() == null)
        .map(
            paths ->
                new InitialNotebookAndRootFolderCreation(
                    paths.notebookReadmePath(), paths.folderReadmePath()));
  }

  static Optional<InitialNotebookRootFolderAndNoteCreation>
      findInitialNotebookRootFolderAndNoteCreation(List<InspectedRegularFile> files) {
    return collectInitialAddedPaths(files)
        .filter(paths -> paths.notebookReadmePath() != null && paths.notePath() != null)
        .filter(paths -> paths.notePath().startsWith(folderPrefix(paths.folderReadmePath())))
        .map(
            paths ->
                new InitialNotebookRootFolderAndNoteCreation(
                    paths.notebookReadmePath(), paths.folderReadmePath(), paths.notePath()));
  }

  private record InitialAddedPaths(
      String notebookReadmePath, String folderReadmePath, String notePath) {}

  private static Optional<InitialAddedPaths> collectInitialAddedPaths(
      List<InspectedRegularFile> files) {
    String notebookReadmePath = null;
    String folderReadmePath = null;
    String notePath = null;
    for (InspectedRegularFile file : files) {
      if (NotebookGitProposalInitialNotebookReadmePublication.isAddedRootNotebookReadme(file)) {
        if (notebookReadmePath != null) {
          return Optional.empty();
        }
        notebookReadmePath = file.path();
      } else if (isAddedRootFolderReadme(file)) {
        if (folderReadmePath != null) {
          return Optional.empty();
        }
        folderReadmePath = file.path();
      } else if (isAddedDirectChildOrdinaryNote(file)) {
        if (notePath != null) {
          return Optional.empty();
        }
        notePath = file.path();
      } else {
        return Optional.empty();
      }
    }
    if (folderReadmePath == null) {
      return Optional.empty();
    }
    return Optional.of(new InitialAddedPaths(notebookReadmePath, folderReadmePath, notePath));
  }

  static String folderPrefix(String pathWithRootFolder) {
    return pathWithRootFolder.substring(0, pathWithRootFolder.indexOf('/') + 1);
  }

  private static boolean unchanged(InspectedRegularFile file) {
    return file.acceptedBlobId() != null
        && file.proposedBlobId() != null
        && file.acceptedBlobId().equals(file.proposedBlobId());
  }

  static boolean isAddedRootFolderReadme(InspectedRegularFile file) {
    return isAddedDirectChildPath(file) && "README.md".equals(directChildBasename(file.path()));
  }

  static boolean isAddedDirectChildOrdinaryNote(InspectedRegularFile file) {
    String path = file.path();
    return isAddedDirectChildPath(file)
        && path.endsWith(".md")
        && !"README.md".equals(directChildBasename(path));
  }

  private static boolean isAddedDirectChildPath(InspectedRegularFile file) {
    String path = file.path();
    int firstSlash = path.indexOf('/');
    return file.acceptedBlobId() == null
        && file.proposedBlobId() != null
        && firstSlash > 0
        && firstSlash == path.lastIndexOf('/');
  }

  private static String directChildBasename(String path) {
    return path.substring(path.indexOf('/') + 1);
  }
}
