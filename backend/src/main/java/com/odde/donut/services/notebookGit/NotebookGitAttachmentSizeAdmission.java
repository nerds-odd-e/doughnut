package com.odde.donut.services.notebookGit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admits attachment payloads across a proposal's first-parent range against the inclusive raw-Git
 * size limit, grandfathering object identities already accepted as attachments in this notebook's
 * retained history (ADR 0002; ADR 0004 Portable attachments; ADR 0006 loud actionable refusal).
 */
final class NotebookGitAttachmentSizeAdmission {

  static final long LIMIT_BYTES = 10_485_760L;

  private NotebookGitAttachmentSizeAdmission() {}

  /**
   * Refuses a newly introduced attachment whose object length exceeds {@link #LIMIT_BYTES} at any
   * path in the contiguous first-parent range from {@code acceptedHead} (exclusive) through {@code
   * proposedHead}. Payloads already present as attachments anywhere in history rooted at {@code
   * acceptedHead} in {@code acceptedRepository} are reused by content identity.
   */
  static void admit(
      Repository proposalRepository,
      ObjectId proposedHead,
      Repository acceptedRepository,
      ObjectId acceptedHead) {
    Set<ObjectId> grandfathered = attachmentObjectIdsInHistory(acceptedRepository, acceptedHead);
    Set<ObjectId> inspected = new HashSet<>();
    List<ObjectId> range =
        NotebookGitProposalAncestry.firstParentRange(
            proposalRepository, acceptedHead, proposedHead);
    for (ObjectId commitId : range.subList(1, range.size())) {
      for (Map.Entry<String, ObjectId> blob :
          attachmentBlobIds(proposalRepository, commitId).entrySet()) {
        ObjectId blobId = blob.getValue();
        if (grandfathered.contains(blobId) || !inspected.add(blobId)) {
          continue;
        }
        long size = objectLength(proposalRepository, blobId);
        if (size > LIMIT_BYTES) {
          throw oversizedRefusal(blob.getKey(), size);
        }
      }
    }
  }

  private static Set<ObjectId> attachmentObjectIdsInHistory(
      Repository repository, ObjectId acceptedHead) {
    Set<ObjectId> attachmentObjectIds = new HashSet<>();
    try (RevWalk walk = new RevWalk(repository)) {
      walk.markStart(walk.parseCommit(acceptedHead));
      for (RevCommit commit : walk) {
        attachmentObjectIds.addAll(attachmentBlobIds(repository, commit.getId()).values());
      }
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Could not inspect accepted attachment history for size admission", e);
    }
    return attachmentObjectIds;
  }

  private static Map<String, ObjectId> attachmentBlobIds(Repository repository, ObjectId commitId) {
    Map<String, ObjectId> attachments = new LinkedHashMap<>();
    for (Map.Entry<String, ObjectId> blob :
        NotebookGitAcceptedTree.blobIds(repository, commitId).entrySet()) {
      if (NotebookGitProposalTreeShape.isAttachment(blob.getKey())) {
        attachments.put(blob.getKey(), blob.getValue());
      }
    }
    return attachments;
  }

  private static long objectLength(Repository repository, ObjectId blobId) {
    try {
      ObjectLoader loader = repository.open(blobId);
      return loader.getSize();
    } catch (IOException e) {
      throw new UncheckedIOException("Could not inspect attachment object length", e);
    }
  }

  private static ResponseStatusException oversizedRefusal(String path, long size) {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Attachment \""
            + path
            + "\" is "
            + size
            + " bytes, which exceeds the "
            + LIMIT_BYTES
            + "-byte limit. Remove this oversized payload from the unpublished proposal before"
            + " publishing. Do not rewrite already accepted commits; a later tip deletion alone"
            + " does not clear an oversized payload from unpublished history.");
  }
}
