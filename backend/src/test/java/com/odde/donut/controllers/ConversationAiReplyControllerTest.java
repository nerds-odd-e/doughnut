package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.ConversationMessage;
import com.odde.donut.entities.ConversationNoteCorrection;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.OpenAiResponseStreamMocker;
import jakarta.persistence.EntityManager;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class ConversationAiReplyControllerTest extends ConversationMessageControllerTestBase {
  @Autowired EntityManager entityManager;
  @Autowired NoteRepository noteRepository;

  @ParameterizedTest
  @CsvSource({
    "false,NoteContentCompletion,content,CONTENT",
    "true,NoteContentCompletion,content,CONTENT",
    "false,TitleReplacement,newTitle,TITLE",
    "true,TitleReplacement,newTitle,TITLE"
  })
  void retainsFragmentedCorrectionAgainstTheRequestSubjectAndBase(
      boolean mcq, String tool, String property, ConversationNoteCorrection.Field field)
      throws Exception {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).content("original").please();
    var binding = makeMe.aGitBindingFor(note.getNotebook()).please();
    Conversation conversation;
    if (mcq) {
      RecallPrompt prompt =
          makeMe
              .aRecallPrompt()
              .withMcqForNote(note)
              .forMemoryTracker(makeMe.aMemoryTrackerFor(note).please())
              .please();
      conversation = controller.startConversationAboutRecallPrompt(prompt);
    } else {
      conversation = makeMe.aConversation().forANote(note).from(currentUser.getUser()).please();
    }
    ConversationMessage source =
        controller.replyToConversation("Please clarify this note", conversation);
    new OpenAiResponseStreamMocker(officialClient)
        .withMessage("This makes the distinction clear")
        .withToolCall(
            tool,
            "{\"" + property + "\":\"Clarified",
            " value\",\"reason\":\"Common cause example\"}")
        .beforeEvents(() -> binding.setAcceptedGitObjectId("b".repeat(40)))
        .mockStreamResponse();

    controller.getAiReply(conversation);
    entityManager.flush();
    entityManager.clear();
    Conversation fresh = conversationRepository.findById(conversation.getId()).orElseThrow();
    ConversationNoteCorrection proposal = controller.getConversationCorrections(fresh).getFirst();
    assertThat(proposal.getProposedValue(), equalTo("Clarified value"));
    assertThat(proposal.getReason(), equalTo("Common cause example"));
    ConversationMessage assistant = controller.getConversationMessages(fresh).getLast();
    assertThat(proposal.getSourceMessageId(), equalTo(assistant.getId()));
    assertThat(
        controller.getConversationMessages(fresh).getFirst().getId(), equalTo(source.getId()));
    assertThat(proposal.getNote().getId(), equalTo(note.getId()));
    assertThat(proposal.getField(), equalTo(field));
    assertThat(proposal.getBaseRevision(), equalTo("a".repeat(40)));
    assertThat(proposal.getDecision(), equalTo(ConversationNoteCorrection.Decision.PENDING));
    assertThat(assistant.getMessage(), equalTo("This makes the distinction clear"));
    assertThat(
        noteRepository.findById(note.getId()).orElseThrow().getContent(), equalTo("original"));
  }

  @Test
  void repeatedMockCallIdentityRetainsSeparateResponses() throws Exception {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    Conversation conversation = controller.startConversationAboutNote(note, "Clarify");
    new OpenAiResponseStreamMocker(officialClient)
        .withToolCall("NoteContentCompletion", "{\"content\":\"One\",\"reason\":\"Reason\"}")
        .mockStreamResponse();
    controller.getAiReply(conversation);
    controller.getAiReply(conversation);
    var proposals = controller.getConversationCorrections(conversation);
    assertThat(proposals, hasSize(2));
    assertThat(
        proposals.getFirst().getResponseId(), not(equalTo(proposals.getLast().getResponseId())));
  }

  @Test
  void retainsToolOnlyCorrectionFromAnEmptyRecallConversation() throws Exception {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    RecallPrompt prompt =
        makeMe
            .aRecallPrompt()
            .withMcqForNote(note)
            .forMemoryTracker(makeMe.aMemoryTrackerFor(note).please())
            .please();
    Conversation conversation = controller.startConversationAboutRecallPrompt(prompt);
    new OpenAiResponseStreamMocker(officialClient)
        .withToolCall(
            "NoteContentCompletion", "{\"content\":\"Clarification\",\"reason\":\"Reason\"}")
        .mockStreamResponse();

    controller.getAiReply(conversation);
    entityManager.flush();
    entityManager.clear();
    Conversation fresh = conversationRepository.findById(conversation.getId()).orElseThrow();
    var proposal = controller.getConversationCorrections(fresh).getFirst();
    assertThat(
        proposal.getSourceMessageId(),
        equalTo(controller.getConversationMessages(fresh).getFirst().getId()));
    assertThat(proposal.getProposedValue(), equalTo("Clarification"));
  }

  @Test
  void retainsTheExplanationAccompanyingAnExclusionTool() throws Exception {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    RecallPrompt prompt =
        makeMe
            .aRecallPrompt()
            .withMcqForNote(note)
            .forMemoryTracker(makeMe.aMemoryTrackerFor(note).please())
            .please();
    Conversation conversation = controller.startConversationAboutRecallPrompt(prompt);
    new OpenAiResponseStreamMocker(officialClient)
        .withMessage("Both choices are defensible")
        .withToolCall("AmbiguousQuestionExclusionProposal", "{\"reason\":\"Ambiguous wording\"}")
        .mockStreamResponse();

    controller.getAiReply(conversation);
    makeMe.refresh(conversation);
    assertThat(
        controller.getConversationMessages(conversation).getLast().getMessage(),
        equalTo("Both choices are defensible"));
  }

  @Test
  void correctionReadsRequireConversationAccess() throws Exception {
    Note note = makeMe.aNote().please();
    Conversation conversation = makeMe.aConversation().forANote(note).please();
    makeMe.aConversationNoteCorrection(makeMe.aConversationMessage(conversation).please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.getConversationCorrections(conversation));
  }

  @Nested
  class GetAiReply {
    Conversation conversation;

    @BeforeEach
    void setup() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      conversation = makeMe.aConversation().forANote(note).from(currentUser.getUser()).please();
      new OpenAiResponseStreamMocker(officialClient)
          .withMessage("I am a Chatbot")
          .mockStreamResponse();
    }

    @Test
    void persistsAiReplyFromStream() throws UnexpectedNoAccessRightException, BadRequestException {
      makeMe
          .aConversationMessage(conversation)
          .sender(currentUser.getUser())
          .message("Hello!")
          .please();

      SseEmitter res = controller.getAiReply(conversation);
      assertThat(res.getTimeout(), nullValue());

      makeMe.refresh(conversation);
      assertThat(conversation.getConversationMessages(), hasSize(2));
      ConversationMessage aiMessage = conversation.getConversationMessages().get(1);
      assertThat(aiMessage.getMessage(), equalTo("I am a Chatbot"));
      assertThat(aiMessage.getSender(), nullValue());
    }
  }
}
