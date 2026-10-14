package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.dto.ConversationCorrectionAcceptance;
import com.odde.donut.controllers.dto.TitleRenameReferenceHandling;
import com.odde.donut.entities.ConversationNoteCorrection;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.services.notebookGit.NotebookGitProposalBlobText;
import com.odde.donut.testability.GitBundleTestReader;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConversationCorrectionTitleAcceptanceControllerTest
    extends ConversationCorrectionAcceptanceControllerTestSupport {
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void publishesReviewedTitleOnTheSameNoteAndRetainsItsReceipt(boolean mcq) throws Exception {
    Review review = fixture(mcq, true);
    accept(review, "  Reviewed title  ", review.base());
    ConversationNoteCorrection stored = reread(review);
    assertThat(stored.getDecision(), is(ConversationNoteCorrection.Decision.ACCEPTED));
    assertThat(stored.getReviewedValue(), is("Reviewed title"));
    assertThat(stored.getNote().getId(), is(review.noteId()));
    assertThat(
        inCommittedTransaction(
            transactionManager,
            () -> noteRepository.findById(review.noteId()).orElseThrow().getTitle()),
        is("Reviewed title"));
    try (InMemoryRepository repository = new InMemoryRepository(new DfsRepositoryDescription())) {
      var tip = GitBundleTestReader.fetchHead(repository, acceptedBundleBytes(review.notebook()));
      assertThat(tip.getName(), is(stored.getAcceptedRevision()));
      assertThat(
          NotebookGitProposalBlobText.readUtf8(repository, tip, "Reviewed title.md"),
          is(ACCEPTED_CONTENT));
    }
    assertThat(
        accept(review, "Retry title", review.base()).getReviewedValue(), is("Reviewed title"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "bad/title", "README"})
  void validatesReviewedTitlesWithTheOrdinaryTitleConstraints(String title) throws Exception {
    Review review = fixture(false, true);
    ApiException refused =
        assertThrows(ApiException.class, () -> accept(review, title, review.base()));
    assertThat(refused.getErrorBody().getErrors().containsKey("newTitle"), is(true));
    assertThat(reread(review).getDecision(), is(ConversationNoteCorrection.Decision.PENDING));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void requiresAndHonorsTheOrdinaryReferenceHandlingChoice(boolean updateText) throws Exception {
    Review review = fixture(false, true);
    Integer carrierId =
        inCommittedTransaction(
            transactionManager,
            () ->
                makeMe
                    .aNote()
                    .underSameNotebookAs(noteRepository.findById(review.noteId()).orElseThrow())
                    .title("Referrer")
                    .please()
                    .getId());
    saveContent(carrierId, content("[[Subject]]"));
    String base = reloadCommittedBinding(review.notebook().getId()).getAcceptedGitObjectId();
    ApiException refused =
        assertThrows(ApiException.class, () -> accept(review, "Reviewed title", base));
    assertThat(refused.getErrorBody().getErrors().get("referenceHandling"), containsString("wiki"));
    conversationController.acceptCorrection(
        review.correctionId(),
        new ConversationCorrectionAcceptance(
            "Reviewed title",
            base,
            updateText
                ? TitleRenameReferenceHandling.UPDATE_VISIBLE_TEXT
                : TitleRenameReferenceHandling.KEEP_VISIBLE_TEXT));
    assertThat(
        inCommittedTransaction(
            transactionManager,
            () -> noteRepository.findById(carrierId).orElseThrow().getContent()),
        containsString(updateText ? "[[Reviewed title]]" : "[[Reviewed title|Subject]]"));
  }
}
