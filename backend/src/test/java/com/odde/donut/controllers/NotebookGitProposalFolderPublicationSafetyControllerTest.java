package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.odde.donut.algorithms.FrontmatterNoteLevel;
import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Verifies atomic refusal and idempotence around folder publication. */
class NotebookGitProposalFolderPublicationSafetyControllerTest
    extends NotebookGitProposalFolderControllerTestBase {

  @Test
  void retriesAnAcceptedFolderAndNotesCommitWithoutDuplicatingIdentities() throws Exception {
    LearnedNotebook fixture = boundNotebookWithLearnedNote();
    String initialHead = fixture.binding().getAcceptedGitObjectId();
    byte[] proposalBytes =
        proposalBundleBytes(
            fixture.binding(),
            withExisting(
                List.of(
                    new NotebookGitProposalFile(FOLDER_README_PATH, FOLDER_README),
                    new NotebookGitProposalFile(NOTE_A_PATH, NOTE_A),
                    new NotebookGitProposalFile(NOTE_B_PATH, NOTE_B))));

    String publishedHead =
        controller.publishNotebookGitProposal(
            fixture.notebook().getId(), initialHead, proposalBytes);
    PublicationFootprint published = committedFootprint(fixture.notebook());
    assertThat(published.folderIds(), hasSize(1));
    assertThat(published.noteIds(), hasSize(3));

    String retriedHead =
        controller.publishNotebookGitProposal(
            fixture.notebook().getId(), initialHead, proposalBytes);

    assertThat(retriedHead, equalTo(publishedHead));
    PublicationFootprint retried = committedFootprint(fixture.notebook());
    assertThat(retried.acceptedHead(), equalTo(published.acceptedHead()));
    assertThat(retried.folderIds(), equalTo(published.folderIds()));
    assertThat(retried.noteIds(), equalTo(published.noteIds()));
    assertThat(retried.trackerId(), equalTo(published.trackerId()));
  }

  @Test
  void rejectsInvalidMemberAfterEligibleFolderAndNotesWithoutPartialPublication() throws Exception {
    LearnedNotebook fixture = boundNotebookWithLearnedNote();
    PublicationFootprint before = committedFootprint(fixture.notebook());
    byte[] proposalBytes =
        proposalBundleBytes(
            fixture.binding(),
            withExisting(
                List.of(
                    new NotebookGitProposalFile(FOLDER_README_PATH, FOLDER_README),
                    new NotebookGitProposalFile(NOTE_A_PATH, NOTE_A),
                    new NotebookGitProposalFile(
                        NOTE_B_PATH, "---\ntype: Note\nnote_level: 7\n---\ninvalid content"))));

    ApiException exception =
        assertProposalRejectedWithoutMutatingBinding(
            fixture.notebook(),
            fixture.binding().getAcceptedGitObjectId(),
            proposalBytes,
            ApiException.class);

    assertThat(exception.getErrorBody().getMessage(), containsString(NOTE_B_PATH));
    assertThat(
        exception.getErrorBody().getMessage(),
        containsString(FrontmatterNoteLevel.AUTHORED_NOTE_LEVEL_MESSAGE));
    assertThat(committedFootprint(fixture.notebook()), equalTo(before));
  }

  @Test
  void rejectsAComposedProposalWhenAnEmptyFolderAppearedOutsideAcceptedHistory() throws Exception {
    LearnedNotebook fixture = boundNotebookWithLearnedNote();
    Folder live = makeMe.aFolder().notebook(fixture.notebook()).name("例文").please();
    byte[] proposalBytes =
        proposalBundleBytes(
            fixture.binding(),
            withExisting(
                List.of(
                    new NotebookGitProposalFile(FOLDER_README_PATH, FOLDER_README),
                    new NotebookGitProposalFile(NOTE_A_PATH, NOTE_A),
                    new NotebookGitProposalFile(NOTE_B_PATH, NOTE_B))));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            fixture.notebook(),
            fixture.binding().getAcceptedGitObjectId(),
            proposalBytes,
            HttpStatus.CONFLICT);

    assertThat(exception.getReason(), containsString("differs from accepted main"));
    assertThat(committedFootprint(fixture.notebook()).folderIds(), equalTo(List.of(live.getId())));
    assertThat(
        committedFootprint(fixture.notebook()).noteIds(),
        equalTo(List.of(fixture.existing().getId())));
  }

  @Test
  void refusesAComposedProposalBasedOnAStaleHeadWithoutWritingFolderOrNotes() throws Exception {
    LearnedNotebook fixture = boundNotebookWithLearnedNote();
    byte[] proposalBytes =
        proposalBundleBytes(
            fixture.binding(),
            withExisting(
                List.of(
                    new NotebookGitProposalFile(FOLDER_README_PATH, FOLDER_README),
                    new NotebookGitProposalFile(NOTE_A_PATH, NOTE_A),
                    new NotebookGitProposalFile(NOTE_B_PATH, NOTE_B))));
    NoteUpdateContentDTO update = new NoteUpdateContentDTO();
    update.setContent("---\ntype: Note\n---\nWeb advanced content.\n");
    textContentController.updateNoteContent(fixture.existing(), update);
    PublicationFootprint afterWeb = committedFootprint(fixture.notebook());

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            fixture.notebook(),
            fixture.binding().getAcceptedGitObjectId(),
            proposalBytes,
            HttpStatus.CONFLICT);

    assertThat(
        exception.getReason(), containsString("The notebook changed since this publish started"));
    assertThat(committedFootprint(fixture.notebook()), equalTo(afterWeb));
  }

  @Test
  void refusesAComposedProposalWhenLiveProjectionHasDrifted() throws Exception {
    LearnedNotebook fixture = boundNotebookWithLearnedNote();
    NoteCreationDTO webCreation = new NoteCreationDTO();
    webCreation.setNewTitle("addition");
    webCreation.setContent(
        "---\ntype: Relationship\nsource: \"[[A]]\"\ntarget: \"[[B]]\"\n---\nweb content");
    Note occupied =
        noteRepository
            .findById(controller.createNoteAtNotebookRoot(fixture.notebook(), webCreation).getId())
            .orElseThrow();
    byte[] proposalBytes =
        proposalBundleBytes(
            fixture.binding(),
            withExisting(
                List.of(
                    new NotebookGitProposalFile(FOLDER_README_PATH, FOLDER_README),
                    new NotebookGitProposalFile(NOTE_A_PATH, NOTE_A),
                    new NotebookGitProposalFile(NOTE_B_PATH, NOTE_B))));

    ResponseStatusException exception =
        assertProposalRejectedWithoutMutatingBinding(
            fixture.notebook(),
            fixture.binding().getAcceptedGitObjectId(),
            proposalBytes,
            HttpStatus.CONFLICT);

    assertThat(exception.getReason(), containsString("refresh the checkout before publishing"));
    assertThat(
        noteRepository.findById(fixture.existing().getId()).orElseThrow().getContent(),
        equalTo(EXISTING_CONTENT));
    assertThat(
        folderRepository.findByNotebookIdOrderByIdAsc(fixture.notebook().getId()), hasSize(0));
    assertThat(
        noteRepository.findById(occupied.getId()).orElseThrow().getTitle(), equalTo("addition"));
    assertThat(
        inCommittedTransaction(
            transactionManager,
            () ->
                memoryTrackerRepository
                    .findById(fixture.tracker().getId())
                    .orElseThrow()
                    .isActive()),
        equalTo(true));
  }
}
