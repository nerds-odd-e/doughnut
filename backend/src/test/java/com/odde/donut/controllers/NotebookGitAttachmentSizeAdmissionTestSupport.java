package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import com.odde.donut.entities.Notebook;
import com.odde.donut.services.notebookTree.PortableTreeEntry;
import com.odde.donut.testability.GitBundleTestReader;
import java.util.Arrays;
import java.util.List;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.springframework.web.server.ResponseStatusException;

/**
 * Shared limit, payload, tip-download, and refusal helpers for attachment-size admission controller
 * proof.
 */
abstract class NotebookGitAttachmentSizeAdmissionTestSupport
    extends NotebookGitWebContentControllerTestBase {

  static final long LIMIT = 10_485_760L;
  static final String NOTE_MARKDOWN = "---\ntype: Note\n---\naccepted content";
  static final String SMALL_JSON = "{\"schema\": \"donut\"}\n";

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

  List<PortableTreeEntry> acceptedTipEntries(Notebook notebook) throws Exception {
    byte[] downloaded =
        controller
            .downloadNotebookGitBundle(notebookRepository.findById(notebook.getId()).orElseThrow())
            .getBody();
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription());
        RevWalk revWalk = new RevWalk(repository)) {
      RevCommit commit = revWalk.parseCommit(GitBundleTestReader.fetchHead(repository, downloaded));
      return GitBundleTestReader.readTreeEntriesWithMetadata(repository, commit);
    }
  }

  boolean nativeObjectPresent(Integer bindingId, ObjectId objectId) {
    return inCommittedTransaction(
        transactionManager,
        () ->
            ((Number)
                        entityManager
                            .createNativeQuery(
                                "SELECT COUNT(*) FROM notebook_git_accepted_object WHERE"
                                    + " notebook_git_binding_id = :bindingId AND git_object_id ="
                                    + " :gitObjectId")
                            .setParameter("bindingId", bindingId)
                            .setParameter("gitObjectId", objectId.getName())
                            .getSingleResult())
                    .longValue()
                > 0);
  }

  static ObjectId blobIdOf(byte[] content) {
    return new ObjectInserter.Formatter().idFor(Constants.OBJ_BLOB, content);
  }

  static byte[] filledBytes(long length, byte value) {
    byte[] bytes = new byte[Math.toIntExact(length)];
    Arrays.fill(bytes, value);
    return bytes;
  }
}
