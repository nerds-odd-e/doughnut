package com.odde.donut.services.notebookGit;

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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Walks the raw two-tree diff between a proposal's accepted-parent commit and its proposed commit.
 * Changed documents are classified once by operation and container/concept role; unchanged accepted
 * files remain context. Publication admission partitions added container Readmes from ordinary-note
 * changes so folder Readmes can accompany note edits. Ordinary-note admission permits added and/or
 * modified ordinary Markdown notes at regular file modes, any number of ordinary-note deletions
 * alone or with same-path edits, and unambiguous equal-content moves with compatible companions.
 * Exact move correspondence is resolved on the tip diff and also carried through adjacent
 * first-parent steps so an unchanged rename followed by a later edit retains accepted origin when
 * tip bytes differ. Mixing unmatched removals with additions is refused when identity
 * correspondence is uncertain. Unsafe paths, non-regular modes, or a changed folder-reserved {@code
 * README.md} are refused. Callers only invoke this once proposal ancestry is confirmed to be a
 * contiguous single-parent range from the accepted commit.
 */
public final class NotebookGitProposalTreeShape {

  private NotebookGitProposalTreeShape() {}

  /**
   * Admits container README additions alongside ordinary-note changes. Non-added container changes
   * stay reserved. Container additions mixed with concept removals stay reserved until that
   * composition is supported.
   */
  static AdmittedShape requireAdmittedShape(
      Repository repository,
      ObjectId acceptedHead,
      ObjectId proposedHead,
      List<ChangedDocument> documents) {
    List<ChangedDocument> containerAdditions = new ArrayList<>();
    List<ChangedDocument> conceptDocuments = new ArrayList<>();
    for (ChangedDocument document : documents) {
      if (document.role() == DocumentRole.CONTAINER) {
        if (document.kind() != ChangeKind.ADDED) {
          throw reservedFolderReadme(document.path());
        }
        containerAdditions.add(document);
      } else {
        conceptDocuments.add(document);
      }
    }
    List<NoteChange> noteChanges = List.of();
    if (!conceptDocuments.isEmpty()) {
      noteChanges =
          admitOrdinaryNoteChanges(
              repository, acceptedHead, proposedHead, noteChangesFrom(conceptDocuments));
    }
    if (!containerAdditions.isEmpty()
        && noteChanges.stream().anyMatch(change -> change.kind() == ChangeKind.DELETED)) {
      throw reservedFolderReadme(containerAdditions.getFirst().path());
    }
    Set<String> addedPaths = new HashSet<>();
    for (NoteChange change : noteChanges) {
      if (change.kind() == ChangeKind.ADDED) {
        addedPaths.add(change.path());
      }
    }
    List<ChangedDocument> additions = new ArrayList<>(containerAdditions);
    for (ChangedDocument document : conceptDocuments) {
      if (addedPaths.contains(document.path())) {
        additions.add(document);
      }
    }
    return new AdmittedShape(noteChanges, additions);
  }

  private static ResponseStatusException reservedFolderReadme(String path) {
    return unsupportedTreeShape("path \"" + path + "\" is a folder README, which is reserved");
  }

  /**
   * Changed documents only: equal accepted/proposed blobs stay on {@link InspectedRegularFile} as
   * context and are omitted here.
   */
  static List<ChangedDocument> classifyChangedDocuments(List<InspectedRegularFile> files) {
    List<ChangedDocument> documents = new ArrayList<>();
    for (InspectedRegularFile file : files) {
      ChangeKind kind = changeKind(file);
      if (kind == null) {
        continue;
      }
      documents.add(new ChangedDocument(file, kind, documentRole(file.path())));
    }
    return documents;
  }

  /**
   * Walks both trees for every safe regular file, including unchanged paths and folder READMEs.
   * Missing side blobs are {@code null}. Path-safety and regular-file-mode refusals match the note
   * classification path; note-path eligibility is not applied here.
   */
  static List<InspectedRegularFile> inspectRegularFiles(
      Repository repository, ObjectId acceptedHead, ObjectId proposedHead) {
    try (RevWalk revWalk = new RevWalk(repository)) {
      RevCommit acceptedCommit = revWalk.parseCommit(acceptedHead);
      RevCommit proposedCommit = revWalk.parseCommit(proposedHead);
      return walkRegularFiles(repository, acceptedCommit, proposedCommit);
    } catch (IOException e) {
      throw unsupportedTreeShape("proposal tree could not be inspected", e);
    }
  }

  private static List<InspectedRegularFile> walkRegularFiles(
      Repository repository, RevCommit acceptedCommit, RevCommit proposedCommit)
      throws IOException {
    try (TreeWalk walk = new TreeWalk(repository)) {
      walk.addTree(acceptedCommit.getTree());
      walk.addTree(proposedCommit.getTree());
      walk.setRecursive(true);

      List<InspectedRegularFile> files = new ArrayList<>();
      while (walk.next()) {
        String path = walk.getPathString();
        assertPathIsSafe(path);

        FileMode acceptedMode = walk.getFileMode(0);
        FileMode proposedMode = walk.getFileMode(1);
        if (FileMode.MISSING.equals(proposedMode)) {
          requireRegularFileMode(path, acceptedMode);
          files.add(new InspectedRegularFile(path, walk.getObjectId(0), null));
          continue;
        }
        if (FileMode.MISSING.equals(acceptedMode)) {
          requireRegularFileMode(path, proposedMode);
          files.add(new InspectedRegularFile(path, null, walk.getObjectId(1)));
          continue;
        }
        requireRegularFileMode(path, acceptedMode);
        requireRegularFileMode(path, proposedMode);
        files.add(new InspectedRegularFile(path, walk.getObjectId(0), walk.getObjectId(1)));
      }
      return files;
    }
  }

  private static void requireRegularFileMode(String path, FileMode mode) {
    if (!FileMode.REGULAR_FILE.equals(mode)) {
      throw unsupportedTreeShape("path \"" + path + "\" is not a regular file mode");
    }
  }

  private static List<NoteChange> noteChangesFrom(List<ChangedDocument> documents) {
    List<NoteChange> changes = new ArrayList<>();
    for (ChangedDocument document : documents) {
      if (document.role() == DocumentRole.CONTAINER) {
        throw reservedFolderReadme(document.path());
      }
      if (!document.path().endsWith(".md")) {
        throw unsupportedTreeShape("path \"" + document.path() + "\" is not a Markdown note");
      }
      changes.add(new NoteChange(document.path(), document.kind(), document.blobId(), null));
    }
    return changes;
  }

  /**
   * Accepts added and/or modified ordinary-note changes, any number of deletions alone or with
   * same-path edits, and unambiguous equal-content moves with compatible companions. Refuses
   * unmatched removal/addition mixtures and ambiguous equal-blob correspondence.
   */
  private static List<NoteChange> admitOrdinaryNoteChanges(
      Repository repository,
      ObjectId acceptedHead,
      ObjectId proposedHead,
      List<NoteChange> changes) {
    if (changes.isEmpty()) {
      throw unsupportedTreeShape("proposal contains no changed file");
    }
    Map<String, NoteOrigin> originsAtTip =
        carryExactMoveOrigins(repository, acceptedHead, proposedHead);
    List<NoteChange> resolvedChanges = resolveMoveCorrespondence(changes, originsAtTip);
    refuseResidualRemovalAndAdditionMixture(resolvedChanges);
    return resolvedChanges;
  }

  /**
   * Carries each accepted concept-note origin through adjacent first-parent steps using only exact
   * equal-content moves. Tip paths that retain continuity map to their accepted {@link NoteOrigin}.
   */
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
    List<NoteChange> resolved = resolveEqualBlobMoves(noteChangesFrom(conceptDocuments));
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

  /**
   * Resolves every unambiguous removed/added pair with identical blob content while retaining all
   * companion changes, then applies origins carried through exact adjacent moves when tip bytes
   * differ. A blob represented by multiple sources or destinations cannot establish identity
   * correspondence, so the complete proposal is refused.
   */
  private static List<NoteChange> resolveMoveCorrespondence(
      List<NoteChange> changes, Map<String, NoteOrigin> originsAtTip) {
    List<NoteChange> equalBlobResolved = resolveEqualBlobMoves(changes);
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
        refuseUncertainIdentityCorrespondence();
      }
      composedOriginsByDestination.put(change.path(), carried);
    }
    return replaceMatchedAdditionsWithRenames(equalBlobResolved, composedOriginsByDestination);
  }

  private static List<NoteChange> resolveEqualBlobMoves(List<NoteChange> changes) {
    Map<ObjectId, List<NoteChange>> removalsByBlob = changesByBlob(changes, ChangeKind.DELETED);
    Map<ObjectId, List<NoteChange>> additionsByBlob = changesByBlob(changes, ChangeKind.ADDED);
    Map<String, NoteOrigin> originsByDestination = new HashMap<>();
    for (Map.Entry<ObjectId, List<NoteChange>> removalGroup : removalsByBlob.entrySet()) {
      List<NoteChange> additionGroup = additionsByBlob.get(removalGroup.getKey());
      if (additionGroup == null) {
        continue;
      }
      if (removalGroup.getValue().size() != 1 || additionGroup.size() != 1) {
        refuseUncertainIdentityCorrespondence();
      }
      NoteChange source = removalGroup.getValue().getFirst();
      NoteChange destination = additionGroup.getFirst();
      originsByDestination.put(destination.path(), new NoteOrigin(source.path(), source.blobId()));
    }
    return replaceMatchedAdditionsWithRenames(changes, originsByDestination);
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

  private static Map<ObjectId, List<NoteChange>> changesByBlob(
      List<NoteChange> changes, ChangeKind kind) {
    Map<ObjectId, List<NoteChange>> changesByBlob = new HashMap<>();
    for (NoteChange change : changes) {
      if (change.kind() == kind) {
        changesByBlob.computeIfAbsent(change.blobId(), ignored -> new ArrayList<>()).add(change);
      }
    }
    return changesByBlob;
  }

  private static void refuseResidualRemovalAndAdditionMixture(List<NoteChange> changes) {
    boolean hasDeleted = changes.stream().anyMatch(change -> change.kind() == ChangeKind.DELETED);
    boolean hasAdded = changes.stream().anyMatch(change -> change.kind() == ChangeKind.ADDED);
    if (!hasDeleted || !hasAdded) {
      return;
    }
    refuseUncertainIdentityCorrespondence();
  }

  private static void refuseUncertainIdentityCorrespondence() {
    throw unsupportedTreeShape(
        "identity correspondence is uncertain: unchanged-content moves may have compatible"
            + " companions, but changed-content moves and unmatched removals mixed with additions"
            + " are not supported");
  }

  private static ChangeKind changeKind(InspectedRegularFile file) {
    if (file.proposedBlobId() == null) {
      return ChangeKind.DELETED;
    }
    if (file.acceptedBlobId() == null) {
      return ChangeKind.ADDED;
    }
    if (!file.acceptedBlobId().equals(file.proposedBlobId())) {
      return ChangeKind.MODIFIED;
    }
    return null;
  }

  private static DocumentRole documentRole(String path) {
    return "README.md".equals(basename(path)) ? DocumentRole.CONTAINER : DocumentRole.CONCEPT;
  }

  private static void assertPathIsSafe(String path) {
    if (path.isEmpty() || path.startsWith("/")) {
      throw unsupportedTreeShape("path \"" + path + "\" is unsafe");
    }
    for (String segment : path.split("/")) {
      if (segment.equals(".") || segment.equals("..")) {
        throw unsupportedTreeShape("path \"" + path + "\" is unsafe");
      }
    }
  }

  private static String basename(String path) {
    int lastSlash = path.lastIndexOf('/');
    return lastSlash < 0 ? path : path.substring(lastSlash + 1);
  }

  static ResponseStatusException unsupportedTreeShape(String reason) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported tree shape: " + reason);
  }

  private static ResponseStatusException unsupportedTreeShape(String reason, Throwable cause) {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Unsupported tree shape: " + reason, cause);
  }

  /**
   * One regular file present on either side of the two-tree walk. A missing side's blob is {@code
   * null}; equal non-null blobs are an unchanged file.
   */
  record InspectedRegularFile(String path, ObjectId acceptedBlobId, ObjectId proposedBlobId) {}

  /**
   * One changed document from the inspected diff, with Git operation and container/concept role.
   * Unchanged accepted files are not represented here.
   */
  record ChangedDocument(InspectedRegularFile file, ChangeKind kind, DocumentRole role) {
    String path() {
      return file.path();
    }

    ObjectId blobId() {
      return kind == ChangeKind.DELETED ? file.acceptedBlobId() : file.proposedBlobId();
    }
  }

  /**
   * Ordinary-note changes plus the addition documents (container Readmes and residual concept
   * additions) ready for document application.
   */
  record AdmittedShape(List<NoteChange> noteChanges, List<ChangedDocument> additions) {}

  /**
   * Accepted-tree path and blob that established note identity. Carried separately from the
   * proposed-tree final {@link NoteChange#path()} and {@link NoteChange#blobId()} so later
   * composition can retain correspondence when final placement or content differs.
   */
  record NoteOrigin(String path, ObjectId blobId) {}

  /**
   * @param path the current (proposed-tree) Portable path; for RENAMED this is the new path
   * @param blobId the raw blob object id relevant to this change: the added blob (proposed tree)
   *     for ADDED, the removed blob (accepted tree) for DELETED, the proposed blob for MODIFIED
   *     (not meaningfully used by callers today), and the proposed tip blob for RENAMED (may differ
   *     from {@link NoteOrigin#blobId()} when an exact move was followed by an edit)
   * @param origin accepted-tree correspondence establishing identity; present for RENAMED, {@code
   *     null} otherwise
   */
  record NoteChange(String path, ChangeKind kind, ObjectId blobId, NoteOrigin origin) {}

  enum ChangeKind {
    ADDED,
    MODIFIED,
    DELETED,
    RENAMED
  }

  enum DocumentRole {
    CONTAINER,
    CONCEPT
  }
}
