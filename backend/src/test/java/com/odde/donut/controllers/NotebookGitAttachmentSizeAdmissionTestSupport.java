package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

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
    String reason = exception.getReason();
    assertThat(reason, containsString("\"" + path + "\""));
    assertThat(reason, containsString(Long.toString(size)));
    assertThat(reason, containsString(Long.toString(LIMIT)));
    assertThat(reason, containsString("Amend or rebase the unpublished proposal"));
    assertThat(reason, containsString("Do not rewrite already accepted commits"));
    assertThat(reason, containsString("tip deletion alone"));
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
