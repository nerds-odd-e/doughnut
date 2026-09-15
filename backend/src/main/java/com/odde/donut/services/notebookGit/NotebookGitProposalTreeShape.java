package com.odde.donut.services.notebookGit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Walks the raw two-tree diff between a proposal's accepted-parent commit and its proposed commit.
 * Changed documents are classified once by operation and container/concept role; unchanged accepted
 * files remain context. Publication admission partitions added container Readmes from ordinary-note
 * changes so folder Readmes can accompany note edits. Ordinary-note admission permits added and/or
 * modified ordinary Markdown notes at regular file modes, any number of ordinary-note deletions
 * alone or with same-path edits, and unambiguous equal-content moves with compatible companions.
 * Exact move correspondence and confirmed deletion-gap recreation (DELETED+ADDED, including
 * identical tip bytes) are settled by {@link NotebookGitProposalNoteCorrespondence}. Within one
 * adjacent transition, residual removals mixed with additions are refused when identity
 * correspondence is uncertain. Net tip deletions may compose with later additions once each
 * adjacent step is admissible. Unsafe paths, non-regular modes, or a changed folder-reserved {@code
 * README.md} are refused. Structural {@code .keep} changes are not note changes. Callers only
 * invoke this once proposal ancestry is confirmed to be a contiguous single-parent range from the
 * accepted commit.
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
    return admitShape(
        documents,
        conceptDocuments ->
            NotebookGitProposalNoteCorrespondence.admitOrdinaryNoteChanges(
                repository, acceptedHead, proposedHead, noteChangesFrom(conceptDocuments)));
  }

  /**
   * Admits residual documents after an exact folder relocation already consumed the relocated
   * subtree. Skips range origin walking so equal-blob notes that moved with the folder are not
   * re-interpreted as ambiguous note correspondence.
   */
  static AdmittedShape requireAdmittedResidualShape(List<ChangedDocument> documents) {
    return admitShape(
        documents,
        conceptDocuments ->
            NotebookGitProposalNoteCorrespondence.resolveEqualBlobMoves(
                noteChangesFrom(conceptDocuments)));
  }

  private static AdmittedShape admitShape(
      List<ChangedDocument> documents,
      Function<List<ChangedDocument>, List<NoteChange>> admitConcepts) {
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
    // Empty tip diffs still run concept admission so deletion-gap replacements that leave endpoint
    // trees equal can surface as DELETED+ADDED.
    List<NoteChange> noteChanges = admitConcepts.apply(conceptDocuments);
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
    Set<String> additionPathsFromDocuments = new HashSet<>();
    for (ChangedDocument document : conceptDocuments) {
      if (document.kind() == ChangeKind.ADDED && addedPaths.contains(document.path())) {
        additions.add(document);
        additionPathsFromDocuments.add(document.path());
      }
    }
    // Deletion-gap recreations can be ADDED in noteChanges while absent from the tip A→T document
    // diff (identical bytes) or while tip shows MODIFIED. Synthesize from settled ADDED.
    for (NoteChange change : noteChanges) {
      if (change.kind() != ChangeKind.ADDED || additionPathsFromDocuments.contains(change.path())) {
        continue;
      }
      additions.add(
          new ChangedDocument(
              new InspectedRegularFile(change.path(), null, change.blobId()),
              ChangeKind.ADDED,
              DocumentRole.CONCEPT));
    }
    return new AdmittedShape(noteChanges, additions);
  }

  private static ResponseStatusException reservedFolderReadme(String path) {
    return unsupportedTreeShape("path \"" + path + "\" is a folder README, which is reserved");
  }

  static List<ChangedDocument> classifyChangedDocuments(List<InspectedRegularFile> files) {
    return NotebookGitProposalTreeInspection.classifyChangedDocuments(files);
  }

  static List<InspectedRegularFile> inspectRegularFiles(
      Repository repository, ObjectId acceptedHead, ObjectId proposedHead) {
    return NotebookGitProposalTreeInspection.inspectRegularFiles(
        repository, acceptedHead, proposedHead);
  }

  static List<NoteChange> noteChangesFrom(List<ChangedDocument> documents) {
    List<NoteChange> changes = new ArrayList<>();
    for (ChangedDocument document : documents) {
      if (document.path().endsWith("/.keep")) {
        continue;
      }
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

  static ResponseStatusException unsupportedTreeShape(String reason) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported tree shape: " + reason);
  }

  static ResponseStatusException unsupportedTreeShape(String reason, Throwable cause) {
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
