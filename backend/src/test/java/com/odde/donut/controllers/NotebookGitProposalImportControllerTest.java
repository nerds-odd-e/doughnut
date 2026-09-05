package com.odde.donut.controllers;

import com.odde.donut.entities.Notebook;
import com.odde.donut.services.notebookExport.PortableTreeEntry;
import com.odde.donut.services.notebookGit.NotebookGitBundleBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.BundleWriter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Verifies that notebook Git proposals must be readable bundles with a usable main branch. */
class NotebookGitProposalImportControllerTest extends NotebookGitBundleControllerTestBase {

  @Test
  void rejectsUnreadableBundleBytesWithoutMutatingTheAcceptedBinding() throws Exception {
    assertProposalRejectedWithoutMutatingBinding(
        createGitBackedNotebook(),
        "someExpectedHead",
        "not a git bundle".getBytes(StandardCharsets.UTF_8),
        HttpStatus.BAD_REQUEST);
  }

  @Test
  void rejectsBundleWithoutUsableMainWithoutMutatingTheAcceptedBinding() throws Exception {
    Notebook notebook = createGitBackedNotebook();
    assertProposalRejectedWithoutMutatingBinding(
        notebook, "someExpectedHead", bundleBytesWithoutUsableMain(), HttpStatus.BAD_REQUEST);
  }

  /** A well-formed bundle that advertises {@code refs/heads/other}, not {@code refs/heads/main}. */
  private byte[] bundleBytesWithoutUsableMain() throws IOException {
    List<PortableTreeEntry> entries =
        List.of(new PortableTreeEntry("README.md", "off-main content"));
    try (Repository repository =
        NotebookGitBundleBuilder.build(
            entries, "Proposer", "proposer@example.com", "Off-main commit", Instant.now())) {
      ObjectId commitId = repository.exactRef(Constants.R_HEADS + "main").getObjectId();

      BundleWriter bundleWriter = new BundleWriter(repository);
      bundleWriter.include("refs/heads/other", commitId);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      bundleWriter.writeBundle(NullProgressMonitor.INSTANCE, out);
      return out.toByteArray();
    }
  }
}
