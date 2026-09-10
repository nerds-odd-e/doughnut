package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.InspectedRegularFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Recognizes exact root Folder-creation proposal shapes from inspected regular-file diffs: one
 * added root-level {@code Folder/README.md}; that plus one or two ordinary notes inside the same
 * Folder; that Folder Readme plus one added root {@code README.md}; those two Readmes plus one
 * added ordinary note directly inside that same Folder; or those two Readmes plus one added
 * ordinary note directly at the notebook root, a sibling of the Folder rather than inside it.
 */
final class NotebookGitProposalFolderCreationShape {

  private NotebookGitProposalFolderCreationShape() {}

  record RootFolderCreation(String readmePath) {}

  /**
   * Exactly one added root-level {@code Folder/README.md} plus one or two added ordinary Notes
   * directly inside that same Folder on an otherwise empty tree: no accepted blobs on those paths
   * and no other path present.
   */
  record RootFolderAndContainedNoteCreation(String folderReadmePath, List<String> notePaths) {
    RootFolderAndContainedNoteCreation {
      notePaths = List.copyOf(notePaths);
      if (notePaths.isEmpty() || notePaths.size() > 2) {
        throw new IllegalArgumentException(
            "RootFolderAndContainedNoteCreation requires one or two Note paths.");
      }
    }
  }

  /**
   * Exactly the two-README initial tree plus one added ordinary {@code .md} note whose parent path
   * is that same new root Folder.
   */
  record InitialNotebookRootFolderAndNoteCreation(
      String notebookReadmePath, String folderReadmePath, String notePath) {}

  /**
   * Exactly the two-README initial tree plus one added ordinary {@code .md} note directly at the
   * notebook root: a sibling of the new root Folder, not inside it.
   */
  record InitialNotebookRootFolderAndRootNoteCreation(
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
        .filter(
            paths ->
                paths.notebookReadmePath() == null
                    && !paths.notePaths().isEmpty()
                    && paths.notePaths().size() <= 2)
        .filter(NotebookGitProposalFolderCreationShape::allStartWithFolderPrefix)
        .map(
            paths ->
                new RootFolderAndContainedNoteCreation(
                    paths.folderReadmePath(), paths.notePaths()));
  }

  static Optional<InitialNotebookRootFolderAndNoteCreation>
      findInitialNotebookRootFolderAndNoteCreation(List<InspectedRegularFile> files) {
    return collectInitialAddedPaths(files)
        .filter(paths -> paths.notebookReadmePath() != null && paths.notePaths().size() == 1)
        .filter(NotebookGitProposalFolderCreationShape::allStartWithFolderPrefix)
        .map(
            paths ->
                new InitialNotebookRootFolderAndNoteCreation(
                    paths.notebookReadmePath(),
                    paths.folderReadmePath(),
                    paths.notePaths().getFirst()));
  }

  static Optional<InitialNotebookRootFolderAndRootNoteCreation>
      findInitialNotebookRootFolderAndRootNoteCreation(List<InspectedRegularFile> files) {
    return collectInitialAddedPaths(files)
        .filter(paths -> paths.notebookReadmePath() != null && paths.notePaths().size() == 1)
        .filter(paths -> paths.notePaths().getFirst().indexOf('/') < 0)
        .map(
            paths ->
                new InitialNotebookRootFolderAndRootNoteCreation(
                    paths.notebookReadmePath(),
                    paths.folderReadmePath(),
                    paths.notePaths().getFirst()));
  }

  private static boolean allStartWithFolderPrefix(InitialAddedPaths paths) {
    String prefix = folderPrefix(paths.folderReadmePath());
    return paths.notePaths().stream().allMatch(notePath -> notePath.startsWith(prefix));
  }

  private record InitialAddedPaths(
      String notebookReadmePath, String folderReadmePath, List<String> notePaths) {}

  private static Optional<InitialAddedPaths> collectInitialAddedPaths(
      List<InspectedRegularFile> files) {
    String notebookReadmePath = null;
    String folderReadmePath = null;
    List<String> notePaths = new ArrayList<>();
    for (InspectedRegularFile file : files) {
      if (NotebookGitProposalInitialTreePublication.isAddedRootNotebookReadme(file)) {
        if (notebookReadmePath != null) {
          return Optional.empty();
        }
        notebookReadmePath = file.path();
      } else if (isAddedRootFolderReadme(file)) {
        if (folderReadmePath != null) {
          return Optional.empty();
        }
        folderReadmePath = file.path();
      } else if (isAddedDirectChildOrdinaryNote(file)
          || NotebookGitProposalInitialTreePublication.isAddedRootMarkdownFile(file)) {
        notePaths.add(file.path());
      } else {
        return Optional.empty();
      }
    }
    if (folderReadmePath == null) {
      return Optional.empty();
    }
    return Optional.of(new InitialAddedPaths(notebookReadmePath, folderReadmePath, notePaths));
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
