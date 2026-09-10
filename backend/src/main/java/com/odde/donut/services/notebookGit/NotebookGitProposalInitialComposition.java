package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.InspectedRegularFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Distinguishes bounded initial Readme/Note compositions after every supported exact initial shape
 * has already declined them. Recognizes one or two ordinary Notes that share one implied root
 * Folder prefix ({@link NotesInImpliedRootFolder}), and otherwise a {@link ValidUnmatched} marker
 * for added-only Markdown trees with role-correct authored types that include at least one README.
 * Recognition does not throw: invalid Markdown, wrong types, non-Markdown paths, and non-initial
 * diffs simply do not match. {@link NotebookGitProposalInitialCompositionPublication} accepts
 * layouts that are wired for publication.
 */
final class NotebookGitProposalInitialComposition {

  private NotebookGitProposalInitialComposition() {}

  /**
   * One or two added ordinary Notes at a single shared root-Folder depth ({@code Folder/A.md},
   * optionally {@code Folder/B.md}) on an otherwise empty tree. The Note paths imply that root
   * Folder prefix even when no Folder README exists.
   */
  record NotesInImpliedRootFolder(List<String> notePaths, String impliedRootFolderPrefix) {
    NotesInImpliedRootFolder {
      notePaths = List.copyOf(notePaths);
      if (notePaths.isEmpty() || notePaths.size() > 2) {
        throw new IllegalArgumentException(
            "NotesInImpliedRootFolder requires one or two ordinary Note paths.");
      }
    }
  }

  /**
   * Marker that every inspected path is an added Markdown file with a role-correct authored type
   * and at least one {@code README.md} basename is included.
   */
  record ValidUnmatched() {}

  static Optional<NotesInImpliedRootFolder> findNotesInImpliedRootFolder(
      List<InspectedRegularFile> files, NotebookGitProposalImporter.ImportedProposal proposal) {
    if (files.isEmpty() || files.size() > 2) {
      return Optional.empty();
    }
    String impliedPrefix = null;
    List<String> notePaths = new ArrayList<>(files.size());
    for (InspectedRegularFile file : files) {
      if (!NotebookGitProposalFolderCreationShape.isAddedDirectChildOrdinaryNote(file)) {
        return Optional.empty();
      }
      String path = file.path();
      String prefix = NotebookGitProposalFolderCreationShape.folderPrefix(path);
      if (impliedPrefix == null) {
        impliedPrefix = prefix;
      } else if (!impliedPrefix.equals(prefix)) {
        return Optional.empty();
      }
      if (!NotebookGitProposalTypedPath.authoredTypeEquals(proposal, path, "Note")) {
        return Optional.empty();
      }
      notePaths.add(path);
    }
    return Optional.of(new NotesInImpliedRootFolder(notePaths, impliedPrefix));
  }

  static Optional<ValidUnmatched> findValidUnmatched(
      List<InspectedRegularFile> files, NotebookGitProposalImporter.ImportedProposal proposal) {
    if (files.isEmpty()) {
      return Optional.empty();
    }
    boolean hasReadme = false;
    for (InspectedRegularFile file : files) {
      if (file.acceptedBlobId() != null || file.proposedBlobId() == null) {
        return Optional.empty();
      }
      String path = file.path();
      if (!path.endsWith(".md")) {
        return Optional.empty();
      }
      String expectedType = expectedTypeForBasename(basename(path));
      if (!NotebookGitProposalTypedPath.authoredTypeEquals(proposal, path, expectedType)) {
        return Optional.empty();
      }
      if ("Readme".equals(expectedType)) {
        hasReadme = true;
      }
    }
    if (!hasReadme) {
      return Optional.empty();
    }
    return Optional.of(new ValidUnmatched());
  }

  private static String expectedTypeForBasename(String basename) {
    return "README.md".equals(basename) ? "Readme" : "Note";
  }

  private static String basename(String path) {
    int lastSlash = path.lastIndexOf('/');
    return lastSlash < 0 ? path : path.substring(lastSlash + 1);
  }
}
