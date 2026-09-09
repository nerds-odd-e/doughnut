package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.InspectedRegularFile;
import java.util.List;
import java.util.Optional;

/**
 * Recognizes exact root Folder-creation proposal shapes from inspected regular-file diffs: one
 * added root-level {@code Folder/README.md}, or that plus one added root {@code README.md}.
 */
final class NotebookGitProposalFolderCreationShape {

  private NotebookGitProposalFolderCreationShape() {}

  record RootFolderCreation(String readmePath) {}

  /**
   * Exactly one added root {@code README.md} plus exactly one added root-level {@code
   * Folder/README.md} on an otherwise empty tree: no accepted blobs on those paths and no other
   * path present (changed or unchanged).
   */
  record InitialNotebookAndRootFolderCreation(String notebookReadmePath, String folderReadmePath) {}

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

  static Optional<InitialNotebookAndRootFolderCreation> findInitialNotebookAndRootFolderCreation(
      List<InspectedRegularFile> files) {
    String notebookReadmePath = null;
    String folderReadmePath = null;
    for (InspectedRegularFile file : files) {
      if (isAddedRootNotebookReadme(file)) {
        if (notebookReadmePath != null) {
          return Optional.empty();
        }
        notebookReadmePath = file.path();
      } else if (isAddedRootFolderReadme(file)) {
        if (folderReadmePath != null) {
          return Optional.empty();
        }
        folderReadmePath = file.path();
      } else {
        return Optional.empty();
      }
    }
    if (notebookReadmePath == null || folderReadmePath == null) {
      return Optional.empty();
    }
    return Optional.of(
        new InitialNotebookAndRootFolderCreation(notebookReadmePath, folderReadmePath));
  }

  private static boolean unchanged(InspectedRegularFile file) {
    return file.acceptedBlobId() != null
        && file.proposedBlobId() != null
        && file.acceptedBlobId().equals(file.proposedBlobId());
  }

  private static boolean isAddedRootNotebookReadme(InspectedRegularFile file) {
    return file.acceptedBlobId() == null
        && file.proposedBlobId() != null
        && "README.md".equals(file.path());
  }

  private static boolean isAddedRootFolderReadme(InspectedRegularFile file) {
    String path = file.path();
    int firstSlash = path.indexOf('/');
    return file.acceptedBlobId() == null
        && file.proposedBlobId() != null
        && firstSlash > 0
        && firstSlash == path.lastIndexOf('/')
        && "README.md".equals(path.substring(firstSlash + 1));
  }
}
