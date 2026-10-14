package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;

import com.odde.donut.controllers.dto.ConversationCorrectionAcceptance;
import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.ConversationNoteCorrection;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

abstract class ConversationCorrectionAcceptanceControllerTestSupport
    extends NotebookGitWebContentHistoryControllerTestSupport {
  @Autowired ConversationMessageController conversationController;
  private Review fixtureReview;

  @AfterEach
  void cleanReviewFixtureAndResetPublicationFailure() {
    NotebookGitPublicationAtomicTestSupport.FAIL_ON_BINDING_SAVE.set(false);
    if (fixtureReview != null) {
      inCommittedTransaction(
          transactionManager,
          () -> {
            Conversation exchange =
                entityManager.find(Conversation.class, fixtureReview.conversationId());
            RecallPrompt prompt = exchange.getSubject().getRecallPrompt();
            entityManager.remove(exchange);
            if (prompt != null) {
              entityManager.remove(prompt);
            }
          });
    }
  }

  Review fixture(boolean mcq) throws Exception {
    Notebook notebook = createGitBackedNotebook();
    fixtureReview =
        inCommittedTransaction(
            transactionManager,
            () -> {
              Notebook managedNotebook =
                  notebookRepository.findById(notebook.getId()).orElseThrow();
              Note note =
                  makeMe
                      .aNote()
                      .notebook(managedNotebook)
                      .title("Subject")
                      .content(ACCEPTED_CONTENT)
                      .please();
              MemoryTracker tracker =
                  makeMe.aMemoryTrackerFor(note).afterNthStrictRecall(2).recallCount(2).please();
              var conversation = makeMe.aConversation().from(currentUser.getUser());
              if (mcq) {
                RecallPrompt prompt =
                    makeMe
                        .aRecallPrompt()
                        .forMemoryTracker(tracker)
                        .withMcqForNote(note)
                        .answerChoiceIndex(1)
                        .please();
                conversation.forARecallPrompt(prompt);
              } else {
                conversation.forANote(note);
              }
              Conversation exchange = conversation.please();
              String base = snapshotCurrentPortableTree(notebook).getAcceptedGitObjectId();
              var source =
                  makeMe
                      .aConversationMessage(exchange)
                      .message("private exchange evidence")
                      .please();
              ConversationNoteCorrection proposal =
                  makeMe.aConversationNoteCorrection(source).baseRevision(base).please();
              return new Review(
                  notebook,
                  note.getId(),
                  exchange.getId(),
                  proposal.getId(),
                  tracker.getId(),
                  base);
            });
    return fixtureReview;
  }

  ConversationNoteCorrection accept(Review review, String value, String base) throws Exception {
    return conversationController.acceptCorrection(
        review.correctionId(), new ConversationCorrectionAcceptance(value, base));
  }

  ConversationNoteCorrection reread(Review review) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          try {
            Conversation exchange = entityManager.find(Conversation.class, review.conversationId());
            return conversationController.getConversationCorrections(exchange).getFirst();
          } catch (UnexpectedNoAccessRightException exception) {
            throw new IllegalStateException(exception);
          }
        });
  }

  String attempt(Review review) {
    return inCommittedTransaction(
        transactionManager,
        () -> {
          try {
            Conversation exchange =
                conversationController.getConversation(
                    entityManager.find(Conversation.class, review.conversationId()));
            RecallPrompt prompt = exchange.getSubject().getRecallPrompt();
            return prompt == null
                ? "null"
                : objectMapper.writeValueAsString(
                    List.of(prompt.getId(), prompt.getMcq(), prompt.getAnswer()));
          } catch (Exception exception) {
            throw new IllegalStateException(exception);
          }
        });
  }

  record Review(
      Notebook notebook,
      Integer noteId,
      Integer conversationId,
      Integer correctionId,
      Integer trackerId,
      String base) {}
}
