package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.NoteLeadingFrontmatter;
import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.InspectedRegularFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Distinguishes bounded initial Readme/Note compositions after every supported exact initial shape
 * has already declined them. Recognizes one or two ordinary Notes that share one implied root
 * Folder prefix ({@link NotesInImpliedRootFolder}), one nested Folder Readme ({@link
 * OneNestedFolderReadme}), exactly two sibling root Folder Readmes ({@link
 * TwoSiblingRootFolderReadmes}), and otherwise a {@link ValidUnmatched} marker for added-only
 * Markdown trees with role-correct authored types that include at least one README. {@link
 * NotebookReadmeWithRootNotes} is the notebook README plus one, two, or three root Notes layout.
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
   * Exactly one added nested Folder Readme ({@code Parent/Child/README.md}) on an otherwise empty
   * tree. The parent Folder is implied without a Readme; the child owns the authored Readme.
   */
  record OneNestedFolderReadme(
      String readmePath, String parentFolderName, String childFolderName) {}

  /**
   * Exactly two added root-level Folder Readmes ({@code Folder A/README.md} and {@code Folder
   * B/README.md}) on an otherwise empty tree. No Notes, notebook README, or nesting.
   */
  record TwoSiblingRootFolderReadmes(String firstReadmePath, String secondReadmePath) {}

  /**
   * Root {@code README.md} plus one, two, or three root ordinary Note paths on an otherwise empty
   * tree.
   */
  record NotebookReadmeWithRootNotes(String notebookReadmePath, List<String> notePaths) {
    NotebookReadmeWithRootNotes {
      notePaths = List.copyOf(notePaths);
      if (notePaths.isEmpty() || notePaths.size() > 3) {
        throw new IllegalArgumentException(
            "NotebookReadmeWithRootNotes requires one, two, or three root Note paths.");
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
      String content =
          NotebookGitProposalBlobText.readUtf8(proposal.repository(), proposal.mainHead(), path);
      if (!authoredTypeEquals(content, "Note")) {
        return Optional.empty();
      }
      notePaths.add(path);
    }
    return Optional.of(new NotesInImpliedRootFolder(notePaths, impliedPrefix));
  }

  static Optional<OneNestedFolderReadme> findOneNestedFolderReadme(
      List<InspectedRegularFile> files, NotebookGitProposalImporter.ImportedProposal proposal) {
    if (files.size() != 1) {
      return Optional.empty();
    }
    InspectedRegularFile file = files.getFirst();
    if (!isAddedNestedFolderReadme(file)) {
      return Optional.empty();
    }
    String path = file.path();
    String content =
        NotebookGitProposalBlobText.readUtf8(proposal.repository(), proposal.mainHead(), path);
    if (!authoredTypeEquals(content, "Readme")) {
      return Optional.empty();
    }
    int firstSlash = path.indexOf('/');
    int secondSlash = path.indexOf('/', firstSlash + 1);
    return Optional.of(
        new OneNestedFolderReadme(
            path, path.substring(0, firstSlash), path.substring(firstSlash + 1, secondSlash)));
  }

  static Optional<TwoSiblingRootFolderReadmes> findTwoSiblingRootFolderReadmes(
      List<InspectedRegularFile> files, NotebookGitProposalImporter.ImportedProposal proposal) {
    if (files.size() != 2) {
      return Optional.empty();
    }
    for (InspectedRegularFile file : files) {
      if (!NotebookGitProposalFolderCreationShape.isAddedRootFolderReadme(file)) {
        return Optional.empty();
      }
      String content =
          NotebookGitProposalBlobText.readUtf8(
              proposal.repository(), proposal.mainHead(), file.path());
      if (!authoredTypeEquals(content, "Readme")) {
        return Optional.empty();
      }
    }
    return Optional.of(
        new TwoSiblingRootFolderReadmes(files.getFirst().path(), files.get(1).path()));
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
      String content =
          NotebookGitProposalBlobText.readUtf8(proposal.repository(), proposal.mainHead(), path);
      if (!authoredTypeEquals(content, expectedType)) {
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

  private static boolean authoredTypeEquals(String content, String expectedType) {
    try {
      return NoteLeadingFrontmatter.split(content)
          .flatMap(split -> split.frontmatter().getString("type"))
          .filter(expectedType::equals)
          .isPresent();
    } catch (YAMLException e) {
      return false;
    }
  }

  private static boolean isAddedNestedFolderReadme(InspectedRegularFile file) {
    String path = file.path();
    int firstSlash = path.indexOf('/');
    if (firstSlash <= 0) {
      return false;
    }
    int secondSlash = path.indexOf('/', firstSlash + 1);
    return file.acceptedBlobId() == null
        && file.proposedBlobId() != null
        && secondSlash > firstSlash + 1
        && secondSlash == path.lastIndexOf('/')
        && "README.md".equals(path.substring(secondSlash + 1));
  }

  private static String basename(String path) {
    int lastSlash = path.lastIndexOf('/');
    return lastSlash < 0 ? path : path.substring(lastSlash + 1);
  }
}
