package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.ChangeKind;
import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.ChangedDocument;
import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.DocumentRole;
import java.util.List;
import java.util.Optional;

/** Recognizes a single root Folder Readme addition, preserving unchanged accepted files. */
final class NotebookGitProposalFolderCreationShape {

  private NotebookGitProposalFolderCreationShape() {}

  record RootFolderCreation(String readmePath) {}

  static Optional<RootFolderCreation> findSingleRootFolderCreation(
      List<ChangedDocument> documents) {
    RootFolderCreation candidate = null;
    for (ChangedDocument document : documents) {
      if (candidate != null || !isAddedRootFolderReadme(document)) {
        return Optional.empty();
      }
      candidate = new RootFolderCreation(document.path());
    }
    return Optional.ofNullable(candidate);
  }

  private static boolean isAddedRootFolderReadme(ChangedDocument document) {
    return document.kind() == ChangeKind.ADDED
        && document.role() == DocumentRole.CONTAINER
        && isDirectChildPath(document.path());
  }

  private static boolean isDirectChildPath(String path) {
    int firstSlash = path.indexOf('/');
    return firstSlash > 0 && firstSlash == path.lastIndexOf('/');
  }
}
