package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.InspectedRegularFile;
import java.util.List;
import java.util.Optional;

/** Recognizes a single root Folder Readme addition, preserving unchanged accepted files. */
final class NotebookGitProposalFolderCreationShape {

  private NotebookGitProposalFolderCreationShape() {}

  record RootFolderCreation(String readmePath) {}

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

  private static boolean unchanged(InspectedRegularFile file) {
    return file.acceptedBlobId() != null
        && file.proposedBlobId() != null
        && file.acceptedBlobId().equals(file.proposedBlobId());
  }

  static boolean isAddedRootFolderReadme(InspectedRegularFile file) {
    return isAddedDirectChildPath(file) && "README.md".equals(directChildBasename(file.path()));
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
