package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.odde.donut.entities.Answer;
import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.ConversationMessage;
import com.odde.donut.entities.ConversationNoteCorrection;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.QuestionGenerationBatchRequest;
import com.odde.donut.entities.RecallLog;
import com.odde.donut.entities.RecallPrompt;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MemoryTrackerQuestionDeletionControllerTest extends MemoryTrackerControllerTestBase {
  @Autowired EntityManager entityManager;

  @Test
  void deletesOwnedQuestionsAndKeepsOtherHistoryAndConversationEvidence() throws Exception {
    Note note = ownedNote();
    MemoryTracker tracker = ownedTracker(note);
    RecallPrompt prompt =
        makeMe.aRecallPrompt().forMemoryTracker(tracker).answerChoiceIndex(1).recorded().please();
    Integer questionId = prompt.getMcq().getId();
    Integer answerId = prompt.getAnswer().getId();
    Integer logId = controller.getRecallHistory(tracker).getFirst().getRecallLog().getId();
    QuestionGenerationBatchRequest request =
        makeMe
            .aQuestionGenerationBatchRequest()
            .batch(makeMe.aQuestionGenerationBatch().forUser(currentUser.getUser()).please())
            .memoryTracker(tracker)
            .please();
    Conversation conversation =
        makeMe.aConversation().forARecallPrompt(prompt).from(currentUser.getUser()).please();
    ConversationMessage message =
        makeMe
            .aConversationMessage(conversation)
            .message("Keep this conversation evidence")
            .please();
    ConversationNoteCorrection correction =
        makeMe.aConversationNoteCorrection(message).acceptedBy(currentUser.getUser()).please();
    MemoryTracker other = ownedTracker();
    RecallPrompt retained =
        makeMe.aRecallPrompt().forMemoryTracker(other).answerChoiceIndex(1).recorded().please();
    var nextRecallAt = other.getNextRecallAt();
    makeMe.entityPersister.flushAndClear();

    controller.delete(entityManager.find(MemoryTracker.class, tracker.getId()));
    makeMe.entityPersister.flushAndClear();

    assertThat(entityManager.find(MemoryTracker.class, tracker.getId()), nullValue());
    assertThat(entityManager.find(RecallPrompt.class, prompt.getId()), nullValue());
    assertThat(entityManager.find(Mcq.class, questionId), nullValue());
    assertThat(entityManager.find(RecallLog.class, logId), nullValue());
    assertThat(
        entityManager.find(QuestionGenerationBatchRequest.class, request.getId()), nullValue());
    assertThat(entityManager.find(Note.class, note.getId()), notNullValue());
    assertThat(entityManager.find(Answer.class, answerId), notNullValue());
    assertConversationHasNoRecallPrompt(
        entityManager.find(Conversation.class, conversation.getId()));
    assertThat(
        entityManager.find(ConversationMessage.class, message.getId()).getMessage(),
        equalTo("Keep this conversation evidence"));
    assertThat(
        entityManager.find(ConversationNoteCorrection.class, correction.getId()).getDecision(),
        equalTo(ConversationNoteCorrection.Decision.ACCEPTED));
    MemoryTracker reloadedOther = entityManager.find(MemoryTracker.class, other.getId());
    var history = controller.getRecallHistory(reloadedOther).getFirst();
    assertThat(history.getRecallPrompt().getId(), equalTo(retained.getId()));
    assertThat(history.getRecallPrompt().getAnswer().getChoiceIndex(), equalTo(1));
    assertThat(reloadedOther.getNextRecallAt(), equalTo(nextRecallAt));
  }

  @Test
  void preservesASharedQuestionUntilItsFinalOwningPromptEnds() throws Exception {
    MemoryTracker tracker = ownedTracker();
    RecallPrompt answered =
        makeMe.aRecallPrompt().forMemoryTracker(tracker).answerChoiceIndex(0).recorded().please();
    makeMe.aRecallPrompt().forMemoryTracker(tracker).withMcq(answered.getMcq()).please();
    RecallPrompt unanswered = promptFor(tracker);
    Integer sharedQuestionId = answered.getMcq().getId();
    Integer unansweredQuestionId = unanswered.getMcq().getId();
    Conversation conversation =
        makeMe.aConversation().forARecallPrompt(unanswered).from(currentUser.getUser()).please();
    ConversationMessage message = makeMe.aConversationMessage(conversation).please();
    ConversationNoteCorrection correction =
        makeMe.aConversationNoteCorrection(message).acceptedBy(currentUser.getUser()).please();
    makeMe.entityPersister.flushAndClear();

    controller.deleteUnansweredRecallPrompts(
        entityManager.find(MemoryTracker.class, tracker.getId()));
    makeMe.entityPersister.flushAndClear();

    assertThat(entityManager.find(Mcq.class, unansweredQuestionId), nullValue());
    assertThat(entityManager.find(Mcq.class, sharedQuestionId), notNullValue());
    assertThat(
        controller
            .getRecallPrompts(entityManager.find(MemoryTracker.class, tracker.getId()))
            .getFirst()
            .getId(),
        equalTo(answered.getId()));
    assertConversationHasNoRecallPrompt(
        entityManager.find(Conversation.class, conversation.getId()));
    assertThat(entityManager.find(ConversationMessage.class, message.getId()), notNullValue());
    assertThat(
        entityManager.find(ConversationNoteCorrection.class, correction.getId()), notNullValue());

    makeMe.entityPersister.flushAndClear();
    controller.delete(entityManager.find(MemoryTracker.class, tracker.getId()));
    makeMe.entityPersister.flushAndClear();

    assertThat(entityManager.find(Mcq.class, sharedQuestionId), nullValue());
  }
}
