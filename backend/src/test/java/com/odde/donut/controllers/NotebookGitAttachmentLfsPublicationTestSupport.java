package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookGit.NotebookGitLfsPointer;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Autowired;

/** Shared LFS-binding and pointer fixtures for attachment publication controller proof. */
abstract class NotebookGitAttachmentLfsPublicationTestSupport
    extends NotebookGitAttachmentSizeAdmissionTestSupport {

  @Autowired FolderRepository folderRepository;
  @Autowired NotebookAttachmentContent notebookAttachmentContent;

  Notebook enableLfs(Notebook notebook) {
    NotebookGitBinding binding = reloadCommittedBinding(notebook.getId());
    binding.setAttachmentRepresentation(NotebookGitAttachmentRepresentation.LFS);
    notebookGitBindingRepository.save(binding);
    return notebook;
  }

  byte[] pointerFor(Notebook notebook, byte[] payload) throws Exception {
    String oid = sha256Hex(payload);
    assertThat(
        notebookAttachmentContent.store(
            notebook.getId(), oid, payload.length, new ByteArrayInputStream(payload)),
        is(true));
    return NotebookGitLfsPointer.format(oid, payload.length);
  }

  static String sha256Hex(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }
}
