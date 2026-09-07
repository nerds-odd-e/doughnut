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
 * while an untouched identical-content note outside the source keeps its own. Accepted tree and
 * folder identities are covered in {@link NotebookGitProposalFolderRelocationControllerTest}. Note
 * relocation associations are covered in {@link
 * NotebookGitProposalRelocationPrivateAssociationControllerTest}.
 */
class NotebookGitProposalFolderRelocationPrivateAssociationControllerTest
    extends NotebookGitMovedNotePrivateAssociationControllerTestBase {

  private static final String README_BODY = "readme";
  private static final String README = ExportReadmeMarkdown.assemble(README_BODY);
  private static final String MATCHING_CONTENT = "---\ntype: Note\n---\nmatching learned content";

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
  }
}
