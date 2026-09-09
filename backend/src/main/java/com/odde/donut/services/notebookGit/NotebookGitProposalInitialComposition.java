package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.NoteLeadingFrontmatter;
import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.InspectedRegularFile;
import java.util.List;
import java.util.Optional;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Distinguishes an added-only, empty-base initial proposal made entirely of safe regular Markdown
 * paths whose basename roles and authored {@code type} values are valid, after every supported
 * exact initial shape has already declined it. Recognition does not throw: invalid Markdown, wrong
 * types, non-Markdown paths, and non-initial diffs simply do not match.
 */
final class NotebookGitProposalInitialComposition {

  private NotebookGitProposalInitialComposition() {}

  /**
   * Marker that every inspected path is an added Markdown file with a role-correct authored type
   * and at least one {@code README.md} basename is included.
   */
  record ValidUnmatched() {}

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

  private static String basename(String path) {
    int lastSlash = path.lastIndexOf('/');
    return lastSlash < 0 ? path : path.substring(lastSlash + 1);
  }
}
