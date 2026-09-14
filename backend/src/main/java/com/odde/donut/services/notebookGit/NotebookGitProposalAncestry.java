package com.odde.donut.services.notebookGit;

import java.io.IOException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Checks that a proposal's {@code main} head is either the notebook's current accepted head
 * unchanged, or reachable from it by a contiguous single-parent commit range - never a caller's
 * claim, never mere similarity. Reused verbatim wherever a proposal's ancestry must be re-verified
 * against the accepted head it targets, including inside the locked publish transaction.
 */
public final class NotebookGitProposalAncestry {

  private NotebookGitProposalAncestry() {}

  /**
   * @param repository the repository (already holding {@code proposedHead}'s history) to inspect
   * @param proposedHead the proposal's {@code main} head
   * @param acceptedHead the notebook's current accepted head
   * @throws ResponseStatusException 409 CONFLICT when {@code proposedHead} is neither identical to
   *     {@code acceptedHead} nor a contiguous single-parent descendant of it
   */
  public static void assertFollowsAcceptedHead(
      Repository repository, ObjectId proposedHead, ObjectId acceptedHead) {
    if (proposedHead.equals(acceptedHead)) {
      return;
    }
    try (RevWalk walk = new RevWalk(repository)) {
      ObjectId current = proposedHead;
      while (!current.equals(acceptedHead)) {
        RevCommit commit = walk.parseCommit(current);
        if (commit.getParentCount() != 1) {
          throw ancestryConflict();
        }
        current = commit.getParent(0);
      }
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Proposal's main head could not be inspected for ancestry.", e);
    }
  }

  private static ResponseStatusException ancestryConflict() {
    return new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Proposal's main head is not a contiguous single-parent descendant of the notebook's"
            + " accepted head.");
  }
}
