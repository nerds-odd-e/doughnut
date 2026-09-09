package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.NoteLeadingFrontmatter;
import com.odde.donut.validators.AuthoredNoteContent;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Requires expected frontmatter {@code type} on proposal Markdown paths during Folder acceptance.
 */
final class NotebookGitProposalTypedPath {

  private NotebookGitProposalTypedPath() {}

  static String requireReadme(
      NotebookGitProposalImporter.ImportedProposal proposal, String readmePath) {
    String readme =
        NotebookGitProposalBlobText.readUtf8(
            proposal.repository(), proposal.mainHead(), readmePath);
    AuthoredNoteContent.assertValidForSave(readme);
    if (!"Readme".equals(frontmatterType(readme))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Invalid Markdown: path \"" + readmePath + "\" must have type: Readme");
    }
    return readme;
  }

  static void requireOrdinaryNote(
      NotebookGitProposalImporter.ImportedProposal proposal, String notePath) {
    String content =
        NotebookGitProposalBlobText.readUtf8(proposal.repository(), proposal.mainHead(), notePath);
    String type = frontmatterType(content);
    if (!"Note".equals(type)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Invalid Markdown: path \""
              + notePath
              + "\" has type: "
              + type
              + "; must have type: Note");
    }
  }

  private static String frontmatterType(String content) {
    return NoteLeadingFrontmatter.split(content)
        .flatMap(split -> split.frontmatter().getString("type"))
        .orElseThrow();
  }
}
