package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Note;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.lib.ObjectId;

/**
 * Applies tip documents under an already-reparented destination after Folder relocation, and splits
 * residual documents into those that must run before versus after reparenting.
 */
final class NotebookGitProposalRelocatedDocuments {

  private NotebookGitProposalRelocatedDocuments() {}

  /**
   * Container or note documents under the relocated destination need the source Folder reparented
   * first; documents that create or fill the destination parent must run before reparenting.
   */
  static void partitionAroundRelocation(
      List<NotebookGitProposalTreeShape.ChangedDocument> documents,
      NotebookGitProposalFolderShape.FolderRelocation relocation,
      List<NotebookGitProposalTreeShape.ChangedDocument> beforeRelocation,
      List<NotebookGitProposalTreeShape.ChangedDocument> afterRelocation) {
    String destPrefix = relocation.destPrefix();
    for (NotebookGitProposalTreeShape.ChangedDocument document : documents) {
      if (document.path().startsWith(destPrefix + "/")) {
        afterRelocation.add(document);
      } else {
        beforeRelocation.add(document);
      }
    }
  }

  /**
   * Tip concept notes under an already-reparented destination resolve against live folders and tip
   * representation; they must not re-materialize the relocated ancestry from the accepted tree.
   */
  static NotebookGitStateLoader.LockedNotebookState applyUnderDestination(
      NotebookGitStateLoader.LockedNotebookState published,
      NotebookGitProposalImporter.ImportedProposal proposal,
      List<NotebookGitProposalTreeShape.ChangedDocument> documents,
      Timestamp publishedAt,
      NotebookGitProposalNoteAddition noteAddition,
      NotebookGitProposalDocumentApplication documentApplication) {
    ObjectId acceptedHead = ObjectId.fromString(published.binding().getAcceptedGitObjectId());
    List<Note> proposedNotes = new ArrayList<>(published.storedNotes());
    List<NotebookGitProposalTreeShape.ChangedDocument> containers = new ArrayList<>();
    for (NotebookGitProposalTreeShape.ChangedDocument document : documents) {
      if (document.role() == NotebookGitProposalTreeShape.DocumentRole.CONTAINER) {
        containers.add(document);
        continue;
      }
      proposedNotes.add(
          noteAddition.applyAtRepresentedPath(
              published.notebook(),
              published.folders(),
              proposal,
              acceptedHead,
              document.path(),
              publishedAt));
    }
    NotebookGitStateLoader.LockedNotebookState withNotes =
        new NotebookGitStateLoader.LockedNotebookState(
            published.binding(), published.notebook(), published.folders(), proposedNotes);
    if (containers.isEmpty()) {
      return withNotes;
    }
    return documentApplication.apply(withNotes, proposal, containers, publishedAt);
  }
}
