package com.odde.donut.services.notebookGit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NotebookGitAttachmentClassificationTest {

  @Test
  void reservesGitAttributesAsMetadataNotAttachments() {
    assertTrue(NotebookGitAttributes.isMetadataPath(".gitattributes"));
    assertTrue(NotebookGitAttributes.isMetadataPath("nested/.gitattributes"));
    assertFalse(NotebookGitProposalTreeShape.isAttachment(".gitattributes"));
    assertTrue(NotebookGitProposalTreeShape.isAttachment("diagram.png"));
    assertFalse(NotebookGitProposalTreeShape.isAttachment("Note.md"));
    assertFalse(NotebookGitProposalTreeShape.isAttachment("Folder/.keep"));
    assertThat(NotebookGitProposalTreeShape.carriesPortableContent(".gitattributes"), is(false));
  }
}
