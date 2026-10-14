package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.controllers.dto.AnswerDTO;
import com.odde.donut.entities.Answer;
import com.odde.donut.entities.AnswerOutcome;
import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.Grade;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.testability.OpenAiResponseStreamMocker;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RecallConversationPracticeControllerTest extends ConversationMessageControllerTestBase {
  @Autowired RecallPromptController recallPromptController;
  @Autowired EntityManager entityManager;

  @Test
  void retainsPracticeAndFeedbackWhileTheOriginalAttemptKeepsItsGradeAndSchedule()
      throws Exception {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    var mcq =
        makeMe
            .anMcq()
            .forNote(note)
            .stem("What does co-movement establish?")
            .choices("Correlation", "Causation", "Independence")
            .correctAnswerIndex(0)
            .please();
    RecallPrompt prompt =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(makeMe.aMemoryTrackerFor(note).please())
            .withMcq(mcq)
            .please();
    AnswerDTO answer = new AnswerDTO();
    answer.setChoiceIndex(1);
    recallPromptController.answer(prompt, answer);
    entityManager.flush();
    entityManager.clear();
    prompt = entityManager.find(RecallPrompt.class, prompt.getId());
    AttemptSnapshot original = snapshot(prompt);
    Conversation conversation = controller.startConversationAboutRecallPrompt(prompt);
    String explanationRequest = "Explain why my selected answer was marked incorrect";
    String explanation = "Co-movement alone establishes correlation, not a causal direction.";
    String practiceRequest =
        "Give me one different, related practice question. Wait for my answer before explaining it.";
    String question =
        "Umbrella use and traffic accidents increase together on rainy days. "
            + "Does this establish that umbrellas cause accidents? Explain your reasoning.";
    String response =
        "No. Rain could cause both, so correlation alone does not establish causation.";
    String feedback =
        "Your common-cause explanation correctly distinguishes correlation from causation.";

    exchange(conversation, explanationRequest, explanation);
    exchange(conversation, practiceRequest, question);
    exchange(conversation, response, feedback);
    entityManager.flush();
    entityManager.clear();
    Conversation reopened = entityManager.find(Conversation.class, conversation.getId());
    var transcript = controller.getConversationMessages(reopened);

    assertThat(
        transcript.stream().map(message -> message.getMessage()).toList(),
        contains(explanationRequest, explanation, practiceRequest, question, response, feedback));
    assertThat(
        transcript.stream()
            .map(message -> message.getSender() == null ? "assistant" : "learner")
            .toList(),
        contains("learner", "assistant", "learner", "assistant", "learner", "assistant"));
    assertThat(snapshot(reopened.getSubject().getRecallPrompt()), equalTo(original));
  }

  private void exchange(Conversation conversation, String request, String reply) throws Exception {
    controller.replyToConversation(request, conversation);
    new OpenAiResponseStreamMocker(officialClient).withMessage(reply).mockStreamResponse();
    controller.getAiReply(conversation);
  }

  private AttemptSnapshot snapshot(RecallPrompt prompt) throws Exception {
    Answer answer = prompt.getAnswer();
    MemoryTracker tracker = prompt.requireMemoryTracker();
    return new AttemptSnapshot(
        answer.getId(),
        answer.getChoiceIndex(),
        answer.getOutcome(),
        answer.getCorrect(),
        recallLogsOf(tracker).stream()
            .map(log -> new LogSnapshot(log.getId(), log.getGrade(), log.getAnswerId()))
            .toList(),
        tracker.getStability(),
        tracker.getDifficulty(),
        tracker.getLastRecalledAt().toInstant(),
        tracker.getNextRecallAt().toInstant(),
        tracker.getRecallCount());
  }

  private record LogSnapshot(Integer id, Grade grade, Integer answerId) {}

  private record AttemptSnapshot(
      Integer answerId,
      Integer selectedChoice,
      AnswerOutcome outcome,
      Boolean correct,
      List<LogSnapshot> recallLogs,
      Float stability,
      Float difficulty,
      Instant lastRecalledAt,
      Instant nextRecallAt,
      Integer recallCount) {}
}
