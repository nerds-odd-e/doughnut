package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookAttachment.NotebookAttachmentContent;
import com.odde.donut.services.notebookAttachment.VerifiedNotebookAttachmentBytes;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admits the attachments each first-parent commit of a proposal changes. Each must be an LFS
 * pointer (or empty file) whose content is stored and matches its digest and size; one over the
 * inclusive size limit is refused unless the accepted head already holds it (ADR 0002; ADR 0004;
 * ADR 0006).
 */
final class NotebookGitAttachmentSizeAdmission {

  static final long LIMIT_BYTES = 10_485_760L;

  private NotebookGitAttachmentSizeAdmission() {}

  /**
   * Checks the attachments changed in the first-parent range from {@code acceptedHead} (exclusive)
   * through {@code proposedHead}.
   */
  static void admit(
      Repository proposalRepository,
      ObjectId proposedHead,
      Repository acceptedRepository,
      ObjectId acceptedHead,
      Integer notebookId,
      NotebookAttachmentContent content) {
    Map<String, Long> inspectedSizes = new HashMap<>();
    Set<String> acceptedHeadDigests = null;
    List<ObjectId> range =
        NotebookGitProposalAncestry.firstParentRange(
            proposalRepository, acceptedHead, proposedHead);
    for (int i = 1; i < range.size(); i++) {
      for (Map.Entry<String, ObjectId> blob :
          changedAttachmentBlobIds(proposalRepository, range.get(i - 1), range.get(i)).entrySet()) {
        byte[] gitBytes = objectBytes(proposalRepository, blob.getValue());
        if (NotebookGitLfsPointer.isEmptyFile(gitBytes)) {
          continue;
        }
        NotebookGitLfsPointer.Parsed pointer =
            NotebookGitLfsPointer.parse(gitBytes)
                .orElseThrow(() -> rawPayloadRefusal(blob.getKey()));
        String digest = pointer.sha256Hex();
        Long seenSize = inspectedSizes.putIfAbsent(digest, pointer.size());
        if (seenSize != null) {
          if (!seenSize.equals(pointer.size())) {
            throw corruptObjectRefusal(blob.getKey(), digest);
          }
          continue;
        }
        if (pointer.size() > LIMIT_BYTES) {
          if (acceptedHeadDigests == null) {
            acceptedHeadDigests = attachmentPayloadDigestsAt(acceptedRepository, acceptedHead);
          }
          if (!acceptedHeadDigests.contains(digest)) {
            throw oversizedRefusal(blob.getKey(), pointer.size());
          }
        }
        byte[] stored =
            content
                .get(notebookId, digest)
                .orElseThrow(() -> missingObjectRefusal(blob.getKey(), digest));
        if (!VerifiedNotebookAttachmentBytes.matchesClaim(stored, digest, pointer.size())) {
          throw corruptObjectRefusal(blob.getKey(), digest);
        }
      }
    }
  }

  private static Set<String> attachmentPayloadDigestsAt(Repository repository, ObjectId commitId) {
    Set<String> digests = new HashSet<>();
    for (Map.Entry<String, ObjectId> blob :
        NotebookGitAcceptedTree.blobIds(repository, commitId).entrySet()) {
      if (NotebookGitProposalTreeShape.isAttachment(blob.getKey())) {
        NotebookGitLfsPointer.parse(objectBytes(repository, blob.getValue()))
            .ifPresent(pointer -> digests.add(pointer.sha256Hex()));
      }
    }
    return digests;
  }

  /** Attachment paths whose blob differs from {@code parent}'s, read from tree objects alone. */
  private static Map<String, ObjectId> changedAttachmentBlobIds(
      Repository repository, ObjectId parent, ObjectId commit) {
    try (TreeWalk treeWalk = new TreeWalk(repository)) {
      treeWalk.addTree(NotebookGitAcceptedTree.rootTreeId(repository, parent));
      treeWalk.addTree(NotebookGitAcceptedTree.rootTreeId(repository, commit));
      treeWalk.setRecursive(true);
      treeWalk.setFilter(TreeFilter.ANY_DIFF);
      Map<String, ObjectId> changed = new LinkedHashMap<>();
      while (treeWalk.next()) {
        if (treeWalk.getFileMode(1) != FileMode.MISSING
            && NotebookGitProposalTreeShape.isAttachment(treeWalk.getPathString())) {
          changed.put(treeWalk.getPathString(), treeWalk.getObjectId(1));
        }
      }
      return changed;
    } catch (IOException e) {
      throw new UncheckedIOException("Could not inspect changed attachments for LFS admission", e);
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
            + "-byte limit in the proposal's latest commit. Remove or shrink it in the unpublished"
            + " commits (amending or adding a commit both work), then publish again. Do not"
            + " rewrite already accepted commits.");
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
        "Attachment \"" + path + "\" must be a Git LFS pointer or empty file.");
  }
}
