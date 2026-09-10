package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookGit.NotebookGitProposalTreeShape.InspectedRegularFile;
import java.util.List;
import java.util.Optional;
import org.eclipse.jgit.lib.ObjectId;
import org.springframework.stereotype.Service;

/** Admits supported implied-Folder compositions to the initial tree publisher. */
@Service
class NotebookGitProposalInitialCompositionPublication {
  private final NotebookGitProposalInitialTreePublication initialTreePublication;

  NotebookGitProposalInitialCompositionPublication(
      NotebookGitProposalInitialTreePublication initialTreePublication) {
    this.initialTreePublication = initialTreePublication;
  }

  /**
   * When the notebook is empty and the inspected tree matches a wired initial composition, accepts
   * it and returns the new head; otherwise empty so the publisher can continue.
   */
  Optional<String> tryAccept(
      NotebookGitStateLoader.LockedNotebookState state,
      NotebookGitProposalImporter.ImportedProposal proposal,
      ObjectId acceptedHead,
      List<InspectedRegularFile> files) {
    if (!state.folders().isEmpty() || !state.liveNotes().isEmpty()) {
      return Optional.empty();
    }
    Optional<NotebookGitProposalInitialComposition.NotesInImpliedRootFolder>
        notesInImpliedRootFolder =
            NotebookGitProposalInitialComposition.findNotesInImpliedRootFolder(files, proposal);
    if (notesInImpliedRootFolder.isPresent()) {
      return Optional.of(
          initialTreePublication.acceptNotesTree(
              state,
              proposal,
              acceptedHead,
              null,
              List.of(),
              notesInImpliedRootFolder.get().notePaths(),
              "Initial Notes in an implied root Folder require an empty notebook."));
    }
    return Optional.empty();
  }
}
