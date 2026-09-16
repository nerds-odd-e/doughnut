package com.odde.donut.services.notebookGit;

import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeInspection.classifyChangedDocuments;
import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeInspection.inspectRegularFiles;
import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.ChangeKind;
import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.ChangedDocument;
import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.DocumentRole;
import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.NoteChange;
import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.NoteOrigin;
import static com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.unsupportedTreeShape;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Resolves ordinary-note identity across an accepted→proposed range: JGit-scored content moves
 * (initially exact-only at {@link NotebookGitProposalRenameDetector#RENAME_SCORE_THRESHOLD},
 * including carried adjacent steps), and confirmed deletion gaps that recreate a path as
 * DELETED+ADDED even when tip bytes match the accepted blob. JGit scoring lives in {@link
 * NotebookGitProposalRenameDetector}; Donut admission safeguards — ambiguous blob correspondence
 * and unresolved removal/addition mixtures — live in {@link NotebookGitProposalIdentityRefusal},
 * outside JGit scoring.
 */
final class NotebookGitProposalNoteCorrespondence {

  private NotebookGitProposalNoteCorrespondence() {}

  static List<NoteChange> admitOrdinaryNoteChanges(
      Repository repository,
      ObjectId acceptedHead,
      ObjectId proposedHead,
      List<NoteChange> changes) {
    Map<String, NoteOrigin> originsAtTip =
        carryExactMoveOrigins(repository, acceptedHead, proposedHead);
    List<NoteChange> resolved =
        changes.isEmpty()
            ? List.of()
            : resolveMoveCorrespondence(repository, changes, originsAtTip);
    return settleDeletionGapReplacements(
        repository, acceptedHead, proposedHead, resolved, originsAtTip);
  }

  /** Tip residual / adjacent-step JGit-scored moves without range origin walking. */
  static List<NoteChange> resolveEqualBlobMoves(Repository repository, List<NoteChange> changes) {
    Map<String, NoteOrigin> originsByDestination =
        NotebookGitProposalRenameDetector.detectRenames(repository, changes);
    return replaceMatchedAdditionsWithRenames(changes, originsByDestination);
  }

  private static List<NoteChange> settleDeletionGapReplacements(
      Repository repository,
      ObjectId acceptedHead,
      ObjectId proposedHead,
      List<NoteChange> resolved,
      Map<String, NoteOrigin> originsAtTip) {
    Set<String> survivingAcceptedPaths = new HashSet<>();
    for (NoteOrigin origin : originsAtTip.values()) {
      survivingAcceptedPaths.add(origin.path());
    }
    Map<String, NoteOrigin> acceptedOrigins = conceptOriginsAt(repository, acceptedHead);
    Map<String, NoteOrigin> tipOrigins = conceptOriginsAt(repository, proposedHead);
    Set<String> replacementPaths = new HashSet<>();
    List<NoteChange> replacements = new ArrayList<>();
    for (Map.Entry<String, NoteOrigin> accepted : acceptedOrigins.entrySet()) {
      String path = accepted.getKey();
      if (survivingAcceptedPaths.contains(path)) {
        continue;
      }
      NoteOrigin tipOrigin = tipOrigins.get(path);
      if (tipOrigin == null) {
        continue;
      }
      replacementPaths.add(path);
      NoteOrigin acceptedOrigin = accepted.getValue();
      replacements.add(new NoteChange(path, ChangeKind.DELETED, acceptedOrigin.blobId(), null));
      replacements.add(new NoteChange(path, ChangeKind.ADDED, tipOrigin.blobId(), null));
    }
    if (replacementPaths.isEmpty()) {
      return resolved;
    }
    List<NoteChange> settled = new ArrayList<>();
    for (NoteChange change : resolved) {
      if (replacementPaths.contains(change.path())) {
        continue;
      }
      settled.add(change);
    }
    settled.addAll(replacements);
    return settled;
  }

  private static Map<String, NoteOrigin> carryExactMoveOrigins(
      Repository repository, ObjectId acceptedHead, ObjectId proposedHead) {
    List<ObjectId> range =
        NotebookGitProposalAncestry.firstParentRange(repository, acceptedHead, proposedHead);
    Map<String, NoteOrigin> origins = conceptOriginsAt(repository, acceptedHead);
    for (int i = 1; i < range.size(); i++) {
      origins =
          advanceOriginsThroughExactMoves(repository, range.get(i - 1), range.get(i), origins);
    }
    return origins;
  }

  private static Map<String, NoteOrigin> conceptOriginsAt(
      Repository repository, ObjectId commitId) {
    try (RevWalk revWalk = new RevWalk(repository);
        TreeWalk walk = new TreeWalk(repository)) {
      RevCommit commit = revWalk.parseCommit(commitId);
      walk.addTree(commit.getTree());
      walk.setRecursive(true);
      Map<String, NoteOrigin> origins = new HashMap<>();
      while (walk.next()) {
        String path = walk.getPathString();
        if (!path.endsWith(".md") || "README.md".equals(basename(path))) {
          continue;
        }
        if (!FileMode.REGULAR_FILE.equals(walk.getFileMode(0))) {
          continue;
        }
        origins.put(path, new NoteOrigin(path, walk.getObjectId(0)));
      }
      return origins;
    } catch (IOException e) {
      throw unsupportedTreeShape("accepted tree could not be read for note origins", e);
    }
  }

  private static Map<String, NoteOrigin> advanceOriginsThroughExactMoves(
      Repository repository,
      ObjectId parentHead,
      ObjectId childHead,
      Map<String, NoteOrigin> origins) {
    List<ChangedDocument> conceptDocuments = new ArrayList<>();
    for (ChangedDocument document :
        classifyChangedDocuments(inspectRegularFiles(repository, parentHead, childHead))) {
      if (document.role() == DocumentRole.CONCEPT) {
        conceptDocuments.add(document);
      }
    }
    List<NoteChange> resolved =
        resolveEqualBlobMoves(
            repository, NotebookGitProposalTreeShape.noteChangesFrom(conceptDocuments));
    NotebookGitProposalIdentityRefusal.refuseResidualRemovalAndAdditionMixture(resolved);
    Map<String, NoteOrigin> next = new HashMap<>(origins);
    for (NoteChange change : resolved) {
      if (change.kind() == ChangeKind.RENAMED) {
        NoteOrigin carried = next.remove(change.origin().path());
        if (carried != null) {
          next.put(change.path(), carried);
        }
      } else if (change.kind() == ChangeKind.DELETED) {
        next.remove(change.path());
      }
    }
    return next;
  }

  private static List<NoteChange> resolveMoveCorrespondence(
      Repository repository, List<NoteChange> changes, Map<String, NoteOrigin> originsAtTip) {
    List<NoteChange> equalBlobResolved = resolveEqualBlobMoves(repository, changes);
    Map<String, NoteChange> deletionsByPath = new HashMap<>();
    for (NoteChange change : equalBlobResolved) {
      if (change.kind() == ChangeKind.DELETED) {
        deletionsByPath.put(change.path(), change);
      }
    }
    Map<String, NoteOrigin> composedOriginsByDestination = new HashMap<>();
    for (NoteChange change : equalBlobResolved) {
      if (change.kind() != ChangeKind.ADDED) {
        continue;
      }
      NoteOrigin carried = originsAtTip.get(change.path());
      if (carried == null) {
        continue;
      }
      if (!deletionsByPath.containsKey(carried.path())) {
        NotebookGitProposalIdentityRefusal.refuseUncertainIdentityCorrespondence(
            List.of(change.path(), carried.path()));
      }
      composedOriginsByDestination.put(change.path(), carried);
    }
    return replaceMatchedAdditionsWithRenames(equalBlobResolved, composedOriginsByDestination);
  }

  private static List<NoteChange> replaceMatchedAdditionsWithRenames(
      List<NoteChange> changes, Map<String, NoteOrigin> originsByDestination) {
    Set<String> matchedSources = new HashSet<>();
    for (NoteOrigin origin : originsByDestination.values()) {
      matchedSources.add(origin.path());
    }
    List<NoteChange> resolved = new ArrayList<>();
    for (NoteChange change : changes) {
      if (change.kind() == ChangeKind.DELETED && matchedSources.contains(change.path())) {
        continue;
      }
      NoteOrigin origin = originsByDestination.get(change.path());
      if (change.kind() == ChangeKind.ADDED && origin != null) {
        resolved.add(new NoteChange(change.path(), ChangeKind.RENAMED, change.blobId(), origin));
      } else {
        resolved.add(change);
      }
    }
    return resolved;
  }

  private static String basename(String path) {
    int lastSlash = path.lastIndexOf('/');
    return lastSlash < 0 ? path : path.substring(lastSlash + 1);
  }
}
