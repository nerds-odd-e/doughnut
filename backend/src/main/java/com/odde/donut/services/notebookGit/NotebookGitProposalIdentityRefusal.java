package com.odde.donut.services.notebookGit;

import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.ChangeKind;
import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.NoteChange;
import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.unsupportedTreeShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Donut admission safeguards for uncertain ordinary-note identity, kept outside JGit scoring. The
 * single failure outcome is preserved; affected paths are appended so the owner can see which
 * unresolved removals/additions or ambiguous pairings blocked publication. Shared by JGit-scored
 * rename detection ({@link NotebookGitProposalRenameDetector}) and composed-move admission ({@link
 * NotebookGitProposalNoteCorrespondence}) so the one refusal message keeps a single home.
 */
final class NotebookGitProposalIdentityRefusal {

  private NotebookGitProposalIdentityRefusal() {}

  /** Refuse when a step leaves both DELETED and ADDED notes unresolved (no rename matched). */
  static void refuseResidualRemovalAndAdditionMixture(List<NoteChange> changes) {
    List<String> affectedPaths = new ArrayList<>();
    boolean hasDeleted = false;
    boolean hasAdded = false;
    for (NoteChange change : changes) {
      if (change.kind() == ChangeKind.DELETED) {
        hasDeleted = true;
        affectedPaths.add(change.path());
      } else if (change.kind() == ChangeKind.ADDED) {
        hasAdded = true;
        affectedPaths.add(change.path());
      }
    }
    if (!hasDeleted || !hasAdded) {
      return;
    }
    refuseUncertainIdentityCorrespondence(affectedPaths);
  }

  /**
   * Shared uncertain-identity refusal used by both JGit scoring and composed-move admission. The
   * single failure outcome is preserved; affected paths are appended so the owner can see which
   * unresolved removals/additions or ambiguous pairings blocked publication.
   */
  static void refuseUncertainIdentityCorrespondence(List<String> affectedPaths) {
    List<String> sorted = new ArrayList<>(affectedPaths);
    Collections.sort(sorted);
    throw unsupportedTreeShape(
        "identity correspondence is uncertain: unchanged-content moves may have compatible"
            + " companions, but changed-content moves and unmatched removals mixed with additions"
            + " are not supported; affected paths: "
            + String.join(", ", sorted));
  }
}
