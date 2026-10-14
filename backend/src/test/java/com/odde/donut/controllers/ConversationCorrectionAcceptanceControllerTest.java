package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.ConversationNoteCorrection;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

@ActiveProfiles({"test", "notebook-git-publication-atomic-test"})
@Import(NotebookGitPublicationAtomicTestSupport.FailingBindingSaveConfig.class)
class ConversationCorrectionAcceptanceControllerTest
    extends ConversationCorrectionAcceptanceControllerTestSupport {

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void publishesReviewedBodyAndRetainsItsDecisionWithLearning(boolean mcq) throws Exception {
    Review review = fixture(mcq);
    Learning before = learning(review.trackerId());
    String originalAttempt = attempt(review);
    accept(review, content("reviewed wording"), review.base());
    ConversationNoteCorrection stored = reread(review);
    History downloaded = downloadHistory(review.notebook(), "Subject.md");
    assertThat(stored.getDecision(), is(ConversationNoteCorrection.Decision.ACCEPTED));
    assertThat(stored.getReviewedValue(), is(content("reviewed wording")));
    assertThat(stored.getDecidedBy().getId(), is(currentUser.getUser().getId()));
    assertThat(stored.getDecidedAt(), notNullValue());
    assertThat(
        stored.getAcceptedRevision(), is(downloaded.headsNewestFirst().getFirst().getName()));
    assertThat(downloaded.contentsNewestFirst().getFirst(), is(stored.getReviewedValue()));
    assertThat(learning(review.trackerId()), equalTo(before));
    assertThat(attempt(review), is(originalAttempt));
    assertThat(
        inCommittedTransaction(
            transactionManager,
            () -> noteRepository.findById(review.noteId()).orElseThrow().getContent()),
        is(stored.getReviewedValue()));
  }

  @Test
  void staleReviewRefusesToOverwriteAndCanBeExplicitlyReviewedAgainstCurrentBase()
      throws Exception {
    Review review = fixture(false);
    saveContent(review.noteId(), content("ordinary edit"));
    ResponseStatusException rejected =
        assertThrows(
            ResponseStatusException.class,
            () -> accept(review, content("reviewed wording"), review.base()));
    assertThat(rejected.getStatusCode(), is(HttpStatus.CONFLICT));
    assertThat(reread(review).getDecision(), is(ConversationNoteCorrection.Decision.PENDING));
    assertThat(
        downloadHistory(review.notebook(), "Subject.md").contentsNewestFirst().getFirst(),
        is(content("ordinary edit")));
    var currentReview = conversationController.loadCurrentReview(review.correctionId());
    assertThat(currentReview.noteRealm().getNote().getContent(), is(content("ordinary edit")));
    String currentBase = currentReview.baseRevision();
    assertThat(
        currentBase,
        is(reloadCommittedBinding(review.notebook().getId()).getAcceptedGitObjectId()));
    accept(review, content("reviewed wording"), currentBase);
    assertThat(reread(review).getReviewedValue(), is(content("reviewed wording")));
    assertThat(reread(review).getBaseRevision(), is(currentBase));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void acceptanceRequiresConversationAccessAndCurrentNoteEditRights(boolean participant)
      throws Exception {
    Review review = fixture(false);
    User reader =
        inCommittedTransaction(
            transactionManager,
            () -> {
              User user = createFixtureUser();
              if (participant) {
                entityManager
                    .find(Conversation.class, review.conversationId())
                    .setConversationInitiator(user);
              }
              return user;
            });
    currentUser.setUser(reader);
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> accept(review, content("reviewed wording"), review.base()));
    currentUser.setUser(review.notebook().getOwnership().getUser());
    assertThat(reread(review).getDecision(), is(ConversationNoteCorrection.Decision.PENDING));
  }

  @Test
  void latePublicationFailureRollsBackContentDecisionAndRevisionThenRetryPublishes()
      throws Exception {
    Review review = fixture(false);
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(true);
    assertThrows(
        RuntimeException.class, () -> accept(review, content("reviewed wording"), review.base()));
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(false);
    assertThat(reread(review).getDecision(), is(ConversationNoteCorrection.Decision.PENDING));
    History failed = downloadHistory(review.notebook(), "Subject.md");
    assertThat(failed.headsNewestFirst().getFirst().getName(), is(review.base()));
    assertThat(failed.contentsNewestFirst().getFirst(), is(ACCEPTED_CONTENT));
    assertThat(
        inCommittedTransaction(
            transactionManager,
            () -> noteRepository.findById(review.noteId()).orElseThrow().getContent()),
        is(ACCEPTED_CONTENT));
    accept(review, content("reviewed wording"), review.base());
    assertThat(reread(review).getDecision(), is(ConversationNoteCorrection.Decision.ACCEPTED));
  }

  @Test
  void repeatedAcceptanceAndDeclineReturnOriginalReceiptAfterAnOrdinaryEdit() throws Exception {
    Review review = fixture(false);
    accept(review, content("reviewed wording"), review.base());
    ConversationNoteCorrection receipt = reread(review);
    saveContent(review.noteId(), content("later authored wording"));
    ConversationNoteCorrection repeated = accept(review, content("retry payload"), review.base());
    ConversationNoteCorrection declined =
        conversationController.declineCorrection(review.correctionId());
    assertThat(repeated.getAcceptedRevision(), is(receipt.getAcceptedRevision()));
    assertThat(declined.getReviewedValue(), is(receipt.getReviewedValue()));
    assertThat(repeated.getDecidedAt(), is(receipt.getDecidedAt()));
    assertThat(
        downloadHistory(review.notebook(), "Subject.md").contentsNewestFirst().getFirst(),
        is(content("later authored wording")));
  }

  @Test
  void completedDeclineWinsAgainstLaterAcceptance() throws Exception {
    Review review = fixture(false);
    conversationController.declineCorrection(review.correctionId());
    assertThat(
        accept(review, content("reviewed wording"), review.base()).getDecision(),
        is(ConversationNoteCorrection.Decision.DECLINED));
    assertThat(
        downloadHistory(review.notebook(), "Subject.md").contentsNewestFirst().getFirst(),
        is(ACCEPTED_CONTENT));
  }

  @Test
  void concurrentAcceptanceAndDeclineKeepOnePublishedReceipt() throws Exception {
    Review review = fixture(false);
    var result =
        queuedWriters(
            review.notebook().getId(),
            () -> accept(review, content("reviewed wording"), review.base()),
            () -> conversationController.declineCorrection(review.correctionId()));
    assertThat(result.first().getDecision(), is(ConversationNoteCorrection.Decision.ACCEPTED));
    assertThat(result.second().getAcceptedRevision(), is(result.first().getAcceptedRevision()));
    assertThat(
        downloadHistory(review.notebook(), "Subject.md").headsNewestFirst().getFirst().getName(),
        is(reread(review).getAcceptedRevision()));
  }

  @Test
  void unchangedReviewedBodyRetainsTheExistingAcceptedRevision() throws Exception {
    Review review = fixture(false);
    accept(review, ACCEPTED_CONTENT, review.base());
    assertThat(reread(review).getAcceptedRevision(), is(review.base()));
  }
}
