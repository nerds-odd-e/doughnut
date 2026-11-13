package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies a learned note relocated across parents keeps its private associations, while an
 * untouched identical-content note keeps its own. The canonical private-association shape for
 * same-parent rename is covered in {@link
 * NotebookGitProposalRenamePrivateAssociationControllerTest}. Filename-preserving placement is
 * covered in {@link NotebookGitProposalRelocationControllerTest}.
 */
class NotebookGitProposalRelocationPrivateAssociationControllerTest
    extends NotebookGitMovedNotePrivateAssociationControllerTestBase {

  private static final String TYPED_NOTE_CONTENT = "---\ntype: Note\n---\noriginal content";
  private static final String MATCHING_CONTENT = "---\ntype: Note\n---\nmatching learned content";

  @Test
  void
      preservesPrivateAssociationsWhenRelocatingAcrossParentsAndLeavesTheIdenticalTextNoteUntouched()
          throws Exception {
    Notebook notebook = createGitBackedNotebook();
    Folder destination = makeMe.aFolder().notebook(notebook).name("Dest").please();
    makeMe.aNote().folder(destination).title("keep").content(TYPED_NOTE_CONTENT).please();
    Note relocatedNote =
        makeMe.aNote().notebook(notebook).title("Original").content(MATCHING_CONTENT).please();
    Note untouchedNote =
        makeMe.aNote().notebook(notebook).title("Untouched").content(MATCHING_CONTENT).please();
    RelocatedNoteAssociations associations =
        commitPrivateAssociations(relocatedNote, untouchedNote);
    NotebookGitBinding binding = snapshotCurrentPortableTree(notebook);

    controller.publishNotebookGitProposal(
        notebook.getId(),
        binding.getAcceptedGitObjectId(),
        proposalBundleBytes(
            binding,
            List.of(
                new NotebookGitProposalFile("Dest/keep.md", TYPED_NOTE_CONTENT),
                new NotebookGitProposalFile("Dest/Original.md", MATCHING_CONTENT),
                new NotebookGitProposalFile("Untouched.md", MATCHING_CONTENT))));

    MemoryTracker relocatedTracker =
        memoryTrackerRepository.findById(associations.relocatedTracker().getId()).orElseThrow();
    assertThat(relocatedTracker.getNote().getId(), equalTo(relocatedNote.getId()));
    assertThat(
        relocatedTracker.getRemovedFromTracking(),
        equalTo(associations.relocatedTracker().getRemovedFromTracking()));
    assertThat(
        relocatedTracker.getNextRecallAt(),
        equalTo(associations.relocatedTracker().getNextRecallAt()));
    assertThat(
        memoryTrackerRepository
            .findById(associations.untouchedTracker().getId())
            .orElseThrow()
            .getNote()
            .getId(),
        equalTo(untouchedNote.getId()));
    assertThat(
        mcqRepository.findById(associations.mcqId()).orElseThrow().getNote().getId(),
        equalTo(relocatedNote.getId()));
    assertThat(
        conversationRepository
            .findById(associations.conversationId())
            .orElseThrow()
            .getSubject()
            .getNote()
            .getId(),
        equalTo(relocatedNote.getId()));
  }
}
