package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.Grade;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.testability.OpenAiResponseStreamMocker;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseInputItem;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

class RecallConversationReasoningControllerTest extends ConversationMessageControllerTestBase {
  @Autowired EntityManager entityManager;
  @Autowired ObjectMapper objectMapper;

  Conversation conversation;

  @BeforeEach
  void setupAttempt() throws Exception {
    Note note =
        makeMe.aNote().notebookOwnedBy(currentUser.getUser()).content("Original source").please();
    Mcq mcq =
        makeMe
            .anMcq()
            .forNote(note)
            .stem("What does co-movement establish?")
            .choices("Correlation", "Causation", "Independence")
            .correctAnswerIndex(0)
            .testedFocus("Correlation versus causal inference")
            .validationRationale("The observation alone does not establish causation")
            .please();
    RecallPrompt prompt =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(makeMe.aMemoryTrackerFor(note).please())
            .withMcq(mcq)
            .answerChoiceIndex(1)
            .please();
    makeMe
        .aRecallLogFor(prompt.getMemoryTracker())
        .answer(prompt.getAnswer())
        .grade(Grade.AGAIN)
        .please();
    note.setContent("Current source: evidence changed after the attempted question");
    conversation = controller.startConversationAboutRecallPrompt(prompt);
    entityManager.flush();
    entityManager.clear();
    conversation = entityManager.find(Conversation.class, conversation.getId());
    new OpenAiResponseStreamMocker(officialClient)
        .withMessage("Tentative explanation")
        .mockStreamResponse();
  }

  @Test
  void sendsOriginalAttemptSeparatelyFromCurrentSourceContext() throws Exception {
    controller.replyToConversation(
        "Explain why my selected answer was marked incorrect", conversation);

    controller.getAiReply(conversation);
    List<ResponseInputItem> input = sentInput();

    assertThat(
        message(input, 0).content().asTextInput(),
        containsString("Current source: evidence changed after the attempted question"));
    String attemptContext = message(input, 1).content().asTextInput();
    JsonNode attempt = objectMapper.readTree(attemptContext.substring(attemptContext.indexOf('{')));
    JsonNode original = attempt.get("originalQuestionDefinition");
    assertThat(original.get("questionStem").asText(), equalTo("What does co-movement establish?"));
    assertThat(
        original.get("responseChoices"),
        equalTo(objectMapper.valueToTree(List.of("Correlation", "Causation", "Independence"))));
    assertThat(original.get("correctAnswerIndex").asInt(), equalTo(0));
    assertThat(attempt.get("userAnswer").asInt(), equalTo(1));
    assertThat(attempt.get("userAnswerWasMarkedAs").asText(), equalTo("incorrect"));
    assertThat(
        original.get("testedFocus").asText(), equalTo("Correlation versus causal inference"));
    assertThat(
        original.get("validationRationale").asText(),
        equalTo("The observation alone does not establish causation"));
    assertThat(
        input.subList(0, 4).stream()
            .map(item -> item.easyInputMessage().orElseThrow().role())
            .toList(),
        contains(
            EasyInputMessage.Role.DEVELOPER,
            EasyInputMessage.Role.DEVELOPER,
            EasyInputMessage.Role.DEVELOPER,
            EasyInputMessage.Role.DEVELOPER));
    assertThat(
        message(input, 2).content().asTextInput(),
        containsString(
            "This conversation helps the learner understand an answered recall question."));
    assertThat(
        message(input, 3).content().asTextInput(),
        equalTo("Make tool calls when user asks to update the note."));
  }

  @Test
  void sendsPersistedReasoningAndCorrectionWithTheirConversationRoles() throws Exception {
    controller.replyToConversation("I thought co-movement showed a cause", conversation);
    controller.getAiReply(conversation);
    controller.replyToConversation(
        "Actually, I meant a plausible common cause, not proof", conversation);
    entityManager.flush();
    entityManager.clear();
    Conversation reopened = entityManager.find(Conversation.class, conversation.getId());

    controller.getAiReply(reopened);

    List<ResponseInputItem> history = sentInput().subList(4, 7);
    assertThat(
        history.stream().map(item -> item.easyInputMessage().orElseThrow().role()).toList(),
        contains(
            EasyInputMessage.Role.USER,
            EasyInputMessage.Role.ASSISTANT,
            EasyInputMessage.Role.USER));
    assertThat(
        history.stream()
            .map(item -> item.easyInputMessage().orElseThrow().content().asTextInput())
            .toList(),
        contains(
            "I thought co-movement showed a cause",
            "Tentative explanation",
            "Actually, I meant a plausible common cause, not proof"));
  }

  private List<ResponseInputItem> sentInput() {
    ArgumentCaptor<ResponseCreateParams> captor =
        ArgumentCaptor.forClass(ResponseCreateParams.class);
    verify(officialClient.async().responses(), atLeastOnce()).createStreaming(captor.capture());
    return captor.getValue().input().flatMap(i -> i.response()).orElseThrow();
  }

  private EasyInputMessage message(List<ResponseInputItem> input, int index) {
    return input.get(index).easyInputMessage().orElseThrow();
  }
}
