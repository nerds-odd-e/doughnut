package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Notebook;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.server.ResponseStatusException;

/** Shared limit, payload, and refusal helpers for attachment-size admission controller proof. */
abstract class NotebookGitAttachmentSizeAdmissionTestSupport
    extends NotebookGitWebContentControllerTestBase {

  static final long LIMIT = 10_485_760L;
  static final String NOTE_MARKDOWN = "---\ntype: Note\n---\naccepted content";

  static void assertOversizedRefusal(ResponseStatusException exception, String path, long size) {
    assertThat(
        exception.getReason(),
        equalTo(
            "Attachment \""
                + path
                + "\" is "
                + size
                + " bytes, which exceeds the "
                + LIMIT
                + "-byte limit in the proposal's latest commit. Remove or shrink it in the"
                + " unpublished commits (amending or adding a commit both work), then publish"
                + " again. Do not rewrite already accepted commits."));
  }

  List<PortableTreeEntry> committedRootAttachments(Notebook notebook) {
    return NotebookLiveProjectionTestReader.rootAttachments(
        transactionManager, notebookAttachmentRepository, notebook.getId());
  }

  static byte[] filledBytes(long length, byte value) {
    byte[] bytes = new byte[Math.toIntExact(length)];
    Arrays.fill(bytes, value);
    return bytes;
  }
}
