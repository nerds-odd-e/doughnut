package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.ConversationNoteCorrection;
import com.odde.donut.entities.User;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConversationCorrectionReviewControllerTest
    extends ConversationCorrectionAcceptanceControllerTestSupport {
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void loadsTheSubjectAndCurrentAcceptedRevisionWhileKeepingItsProposalPending(boolean mcq)
      throws Exception {
    Review review = fixture(mcq, true);
    saveContent(review.noteId(), content("current authored content"));
    var snapshot = conversationController.loadCurrentReview(review.correctionId());
    assertThat(snapshot.noteRealm().getId(), is(review.noteId()));
    assertThat(
        snapshot.noteRealm().getNote().getContent(), is(content("current authored content")));
    assertThat(snapshot.noteRealm().getNote().getTitle(), is("Subject"));
    assertThat(
        snapshot.baseRevision(),
        is(reloadCommittedBinding(review.notebook().getId()).getAcceptedGitObjectId()));
    assertThat(reread(review).getDecision(), is(ConversationNoteCorrection.Decision.PENDING));
  }

  @Test
  void pairsTheNoteAndRevisionAfterAnAlreadyQueuedAuthorEdit() throws Exception {
    Review review = fixture(false);
    var result =
        queuedWriters(
            review.notebook().getId(),
            () -> saveContent(review.noteId(), content("queued author edit")),
            () -> conversationController.loadCurrentReview(review.correctionId()));
    assertThat(
        result.second().noteRealm().getNote().getContent(), is(content("queued author edit")));
    assertThat(
        result.second().baseRevision(),
        is(reloadCommittedBinding(review.notebook().getId()).getAcceptedGitObjectId()));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void requiresBothConversationAccessAndCurrentNoteReadRights(boolean participant)
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
        () -> conversationController.loadCurrentReview(review.correctionId()));
    currentUser.setUser(review.notebook().getOwnership().getUser());
  }
}
