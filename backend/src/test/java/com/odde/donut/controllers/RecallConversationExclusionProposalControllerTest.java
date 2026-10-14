package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.testability.OpenAiResponseStreamMocker;
import com.openai.models.responses.ResponseCreateParams;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RecallConversationExclusionProposalControllerTest
    extends ConversationMessageControllerTestBase {
  private static final String PROPOSAL_TOOL = "AmbiguousQuestionExclusionProposal";

  @Autowired RecallPromptController recallPromptController;
  @Autowired ObjectMapper objectMapper;

  Note note;
  MemoryTracker tracker;

  @BeforeEach
  void setup() {
    note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    tracker = makeMe.aMemoryTrackerFor(note).please();
    new OpenAiResponseStreamMocker(officialClient)
        .withMessage("Both completions are grammatical")
        .mockStreamResponse();
  }

  @Test
  void offersTheProposalToolForAnMcqAttemptAndSaysOnlyTheLearnerConfirms() throws Exception {
    ResponseCreateParams sent = requestSentFor(conversationAbout(wronglyAnsweredMcqPrompt()));

    assertThat(toolNames(sent), hasItem(PROPOSAL_TOOL));
    assertThat(
        developerMessage(sent, 2),
        allOf(
            containsString("does not support a unique answer"),
            containsString("call AmbiguousQuestionExclusionProposal"),
            containsString("only for genuine ambiguity"),
            containsString("a choice that is grammatical but means something else is wrong"),
            containsString("A proposal changes nothing."),
            containsString("Only the learner's confirmation excludes the attempt."),
            containsString("do not propose it again")));
  }

  @Test
  void doesNotOfferTheProposalToolInANoteConversation() throws Exception {
    Conversation conversation =
        makeMe.aConversation().forANote(note).from(currentUser.getUser()).please();

    assertThat(toolNames(requestSentFor(conversation)), not(hasItem(PROPOSAL_TOOL)));
  }

  @Test
  void tellsTheTutorThatTheAttemptIsAlreadyExcludedAndWhy() throws Exception {
    RecallPrompt prompt = wronglyAnsweredMcqPrompt();
    recallPromptController.excludeAsAmbiguous(prompt, ambiguousExclusion("both tenses fit"));

    String attemptContext = developerMessage(requestSentFor(conversationAbout(prompt)), 1);

    JsonNode attempt = objectMapper.readTree(attemptContext.substring(attemptContext.indexOf('{')));
    assertThat(
        attempt.get("attemptExcludedAsAmbiguous").get("reason").asText(),
        equalTo("both tenses fit"));
    assertThat(attempt.get("userAnswerWasMarkedAs").asText(), equalTo("incorrect"));
  }

  private RecallPrompt wronglyAnsweredMcqPrompt() throws Exception {
    RecallPrompt prompt =
        makeMe.aRecallPrompt().withMcqForNote(note).forMemoryTracker(tracker).please();
    recallPromptController.answer(prompt, choiceAnswer(1));
    return prompt;
  }

  private Conversation conversationAbout(RecallPrompt prompt) throws Exception {
    return controller.startConversationAboutRecallPrompt(prompt);
  }

  private ResponseCreateParams requestSentFor(Conversation conversation) throws Exception {
    controller.getAiReply(conversation);
    return lastRequestSentToAi();
  }

  private List<String> toolNames(ResponseCreateParams sent) {
    return sent.tools().orElseThrow().stream().map(tool -> tool.asFunction().name()).toList();
  }

  private String developerMessage(ResponseCreateParams sent, int index) {
    return sent.input()
        .flatMap(input -> input.response())
        .orElseThrow()
        .get(index)
        .easyInputMessage()
        .orElseThrow()
        .content()
        .asTextInput();
  }
}
