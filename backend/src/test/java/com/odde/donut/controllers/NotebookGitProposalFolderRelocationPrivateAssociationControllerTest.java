package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGitBinding;
import com.odde.donut.services.notebookExport.ExportReadmeMarkdown;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies a learned note inside a published folder relocation keeps its private associations,
 * while an untouched identical-content note outside the source keeps its own. A later content
 * publication at the moved path updates the same learned note and schedule. Accepted tree and
 * folder identities are covered in {@link NotebookGitProposalFolderRelocationControllerTest}. Note
 * relocation associations are covered in {@link
 * NotebookGitProposalRelocationPrivateAssociationControllerTest}.
 */
class NotebookGitProposalFolderRelocationPrivateAssociationControllerTest
    extends NotebookGitMovedNotePrivateAssociationControllerTestBase {

  private static final String README_BODY = "readme";
  private static final String README = ExportReadmeMarkdown.assemble(README_BODY);
  private static final String MATCHING_CONTENT = "---\ntype: Note\n---\nmatching learned content";
  private static final String EDITED_CONTENT = "---\ntype: Note\n---\nedited after folder move";

  @Test
  void preservesPrivateAssociationsOnMovedFolderNotesAndLeavesTheIdenticalOutsideNoteUntouched()
      throws Exception {
    Notebook notebook = createGitBackedNotebook();
    makeMe.aFolder().notebook(notebook).name("Archive").readmeContent(README_BODY).please();
    Folder topics =
        makeMe.aFolder().notebook(notebook).name("Topics").readmeContent(README_BODY).please();
    Note relocatedNote =
        makeMe.aNote().folder(topics).title("Original").content(MATCHING_CONTENT).please();
    Note deletedNote =
        makeMe
            .aNote()
            .folder(topics)
            .title("Gone")
            .content(MATCHING_CONTENT)
            .softDeleted()
            .please();
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
                new NotebookGitProposalFile("Archive/README.md", README),
                new NotebookGitProposalFile("Archive/Topics/README.md", README),
                new NotebookGitProposalFile("Archive/Topics/Original.md", MATCHING_CONTENT),
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
    assertThat(
        noteRepository.findById(deletedNote.getId()).orElseThrow().getDeletedAt(),
        not(nullValue()));

    NotebookGitBinding afterMove =
        notebookGitBindingRepository.findByNotebook_Id(notebook.getId()).orElseThrow();
    controller.publishNotebookGitProposal(
        notebook.getId(),
        afterMove.getAcceptedGitObjectId(),
        proposalBundleBytes(
            afterMove,
            List.of(
                new NotebookGitProposalFile("Archive/README.md", README),
                new NotebookGitProposalFile("Archive/Topics/README.md", README),
                new NotebookGitProposalFile("Archive/Topics/Original.md", EDITED_CONTENT),
                new NotebookGitProposalFile("Untouched.md", MATCHING_CONTENT))));

    Note reloadedRelocated = noteRepository.findById(relocatedNote.getId()).orElseThrow();
    assertThat(reloadedRelocated.getContent(), equalTo(EDITED_CONTENT));
    MemoryTracker trackerAfterEdit =
        memoryTrackerRepository.findById(associations.relocatedTracker().getId()).orElseThrow();
    assertThat(trackerAfterEdit.getNote().getId(), equalTo(relocatedNote.getId()));
    assertThat(
        trackerAfterEdit.getRemovedFromTracking(),
        equalTo(associations.relocatedTracker().getRemovedFromTracking()));
    assertThat(
        trackerAfterEdit.getNextRecallAt(),
        equalTo(associations.relocatedTracker().getNextRecallAt()));
    assertThat(
        noteRepository.findById(untouchedNote.getId()).orElseThrow().getContent(),
        equalTo(MATCHING_CONTENT));
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
