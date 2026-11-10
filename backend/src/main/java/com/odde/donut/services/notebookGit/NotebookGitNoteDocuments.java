package com.odde.donut.services.notebookGit;

import com.odde.donut.algorithms.AuthoredNoteDocument;
import com.odde.donut.algorithms.CanonicalDonutOrigin;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.validators.AuthoredNoteContent;
import org.springframework.stereotype.Service;

/** Reads immutable Git note bytes as validated authored documents for live persistence. */
@Service
class NotebookGitNoteDocuments {
  private final CanonicalDonutOrigin canonicalDonutOrigin;

  NotebookGitNoteDocuments(CanonicalDonutOrigin canonicalDonutOrigin) {
    this.canonicalDonutOrigin = canonicalDonutOrigin;
  }

  AuthoredNoteDocument readValidatedDocument(
      NotebookGitProposalImporter.ImportedProposal proposal, String path) {
    String content =
        NotebookGitProposalBlobText.readUtf8(proposal.repository(), proposal.mainHead(), path);
    try {
      AuthoredNoteContent.assertValidForSave(content);
    } catch (ApiException exception) {
      throw exception.withContext("Invalid authored property at path \"" + path + "\"");
    }
    return AuthoredNoteDocument.fromContent(content, canonicalDonutOrigin);
  }
}
