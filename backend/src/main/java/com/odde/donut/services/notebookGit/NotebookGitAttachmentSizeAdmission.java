package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.NotebookGitAttachmentRepresentation;
import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookAttachment.VerifiedNotebookAttachmentBytes;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admits attachment payloads across a proposal's first-parent range against the inclusive size
 * limit. Raw Git measures blob bytes; LFS verifies the tip's referenced objects' actual size,
 * digest, and durability before acceptance (ADR 0002; ADR 0004 Portable attachments; ADR 0006 loud
 * actionable refusal).
 */
final class NotebookGitAttachmentSizeAdmission {

  static final long LIMIT_BYTES = 10_485_760L;

  private NotebookGitAttachmentSizeAdmission() {}

  /**
   * Refuses a newly introduced attachment whose admitted size exceeds {@link #LIMIT_BYTES}. Raw
   * notebooks inspect object length across the contiguous first-parent range from {@code
   * acceptedHead} (exclusive) through {@code proposedHead}, grandfathering object identities
   * already accepted as attachments in this notebook's retained history. LFS notebooks verify each
   * tip attachment pointer against durable content and measure that verified payload size.
   */
  static void admit(
      Repository proposalRepository,
      ObjectId proposedHead,
      Repository acceptedRepository,
      ObjectId acceptedHead,
      NotebookGitAttachmentRepresentation representation,
      Integer notebookId,
      NotebookAttachmentContent content) {
    if (representation == NotebookGitAttachmentRepresentation.LFS) {
      admitLfsTip(proposalRepository, proposedHead, notebookId, content);
      return;
    }
    admitRawRange(proposalRepository, proposedHead, acceptedRepository, acceptedHead);
  }

  private static void admitRawRange(
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

  private static void admitLfsTip(
      Repository proposalRepository,
      ObjectId proposedHead,
      Integer notebookId,
      NotebookAttachmentContent content) {
    for (Map.Entry<String, ObjectId> blob :
        attachmentBlobIds(proposalRepository, proposedHead).entrySet()) {
      byte[] gitBytes = objectBytes(proposalRepository, blob.getValue());
      if (NotebookGitLfsPointer.isEmptyFile(gitBytes)) {
        continue;
      }
      Optional<NotebookGitLfsPointer.Parsed> parsed = NotebookGitLfsPointer.parse(gitBytes);
      if (parsed.isEmpty()) {
        throw rawPayloadRefusal(blob.getKey());
      }
      NotebookGitLfsPointer.Parsed pointer = parsed.get();
      Optional<byte[]> stored = content.get(notebookId, pointer.sha256Hex());
      if (stored.isEmpty()) {
        throw missingObjectRefusal(blob.getKey(), pointer.sha256Hex());
      }
      if (!VerifiedNotebookAttachmentBytes.matchesClaim(
          stored.get(), pointer.sha256Hex(), pointer.size())) {
        throw corruptObjectRefusal(blob.getKey(), pointer.sha256Hex());
      }
      if (pointer.size() > LIMIT_BYTES) {
        throw oversizedRefusal(blob.getKey(), pointer.size());
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

  private static byte[] objectBytes(Repository repository, ObjectId blobId) {
    try {
      return repository.open(blobId).getBytes();
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read attachment object for LFS admission", e);
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
            + "-byte limit. Amend or rebase the unpublished proposal so this oversized payload is"
            + " gone before publishing. Do not rewrite already accepted commits; a later tip"
            + " deletion alone does not clear an oversized payload from unpublished history.");
  }

  private static ResponseStatusException missingObjectRefusal(String path, String sha256Hex) {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Attachment \""
            + path
            + "\" references missing LFS object sha256:"
            + sha256Hex
            + ". Upload the object before publishing.");
  }

  private static ResponseStatusException corruptObjectRefusal(String path, String sha256Hex) {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Attachment \""
            + path
            + "\" references corrupt LFS object sha256:"
            + sha256Hex
            + ". Re-upload matching bytes before publishing.");
  }

  private static ResponseStatusException rawPayloadRefusal(String path) {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Attachment \""
            + path
            + "\" must be a Git LFS pointer or empty file when the notebook uses LFS.");
  }
}
