package com.odde.donut.services.ai;

import com.odde.donut.entities.Conversation;
import com.odde.donut.entities.ConversationMessage;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import com.odde.donut.services.ai.tools.AiToolFactory;
import com.odde.donut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.donut.services.focusContext.FocusContextResult;
import com.odde.donut.services.focusContext.FocusContextRetrievalService;
import com.odde.donut.services.focusContext.RetrievalConfig;
import com.openai.models.ChatModel;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseInputItem;
import java.util.ArrayList;
import java.util.List;

public class ConversationAiRequestBuilder {
  private final FocusContextRetrievalService focusContextRetrievalService;
  private final FocusContextMarkdownRenderer focusContextMarkdownRenderer;

  public ConversationAiRequestBuilder(
      FocusContextRetrievalService focusContextRetrievalService,
      FocusContextMarkdownRenderer focusContextMarkdownRenderer) {
    this.focusContextRetrievalService = focusContextRetrievalService;
    this.focusContextMarkdownRenderer = focusContextMarkdownRenderer;
  }

  public ResponseCreateParams buildResponseCreateParams(
      Conversation conversation, String modelName) {
    List<ResponseInputItem> inputItems = new ArrayList<>();
    addNoteContext(inputItems, conversation);
    for (ConversationMessage msg : conversation.getConversationMessages()) {
      if (msg.getSender() == null) {
        inputItems.add(
            ResponseInputItem.ofEasyInputMessage(
                EasyInputMessage.builder()
                    .role(EasyInputMessage.Role.ASSISTANT)
                    .content(msg.getMessage())
                    .build()));
      } else {
        inputItems.add(
            ResponseInputItem.ofEasyInputMessage(
                EasyInputMessage.builder()
                    .role(EasyInputMessage.Role.USER)
                    .content(msg.getMessage())
                    .build()));
      }
    }

    ResponseCreateParams.Builder builder =
        ResponseCreateParams.builder().model(ChatModel.of(modelName)).inputOfResponse(inputItems);
    for (Class<?> toolClass : AiToolFactory.getAllAssistantTools()) {
      @SuppressWarnings("unchecked")
      Class<Object> paramClass = (Class<Object>) toolClass;
      builder.addTool(paramClass);
    }
    if (conversation.getSubject().getRecallPrompt() != null) {
      builder.addTool(AmbiguousQuestionExclusionProposal.class);
    }
    return builder.build();
  }

  private void addNoteContext(List<ResponseInputItem> items, Conversation conversation) {
    Note note = conversation.getSubjectNote();
    if (note == null) {
      return;
    }
    RetrievalConfig config = RetrievalConfig.defaultMaxDepth();
    User viewer = conversation.getConversationInitiator();
    FocusContextResult focusContextResult =
        viewer != null
            ? focusContextRetrievalService.retrieve(note, viewer, config)
            : focusContextRetrievalService.retrieve(note, config);
    String noteDescription = focusContextMarkdownRenderer.render(focusContextResult, config);

    items.add(
        ResponseInputItem.ofEasyInputMessage(
            EasyInputMessage.builder()
                .role(EasyInputMessage.Role.DEVELOPER)
                .content(noteDescription)
                .build()));

    String additionalContext = conversation.getAdditionalContextForSubject();
    if (additionalContext != null) {
      items.add(
          ResponseInputItem.ofEasyInputMessage(
              EasyInputMessage.builder()
                  .role(EasyInputMessage.Role.DEVELOPER)
                  .content(additionalContext)
                  .build()));
    }

    if (conversation.getSubject().getRecallPrompt() != null) {
      items.add(
          ResponseInputItem.ofEasyInputMessage(
              EasyInputMessage.builder()
                  .role(EasyInputMessage.Role.DEVELOPER)
                  .content(
                      """
                        This conversation helps the learner understand an answered recall question.

                        Use the original question, choices, selected answer, marked result, tested focus,
                        and rationale as the record of that attempt. Current accessible notes are source
                        context, and may differ from the attempt's original context.

                        Address the learner's actual reasoning and the distinction behind the selected
                        choice. Reasoning is optional: when the learner asks for an explanation, explain
                        without requiring them to describe their thought process first. Any interpretation
                        of why they answered that way is tentative; accept corrections and adapt the
                        explanation.

                        Examine objections to the wording, choices, answer key, or source evidence on
                        their merits. A marked result is not proof that the question is sound or that the
                        learner forgot. Acknowledge ambiguity, defensible alternative answers, and missing
                        evidence when present, and state what remains uncertain.

                        When requested, give one related practice question that changes the example while
                        checking the relevant distinction. Do not reveal its answer or explanation before
                        the learner responds. Then give feedback on that response and explain the reasoning.

                        Ask conversational practice questions as ordinary assistant text, not as tool
                        calls. Do not use GeneratedMcq or question-generation tools for this practice:
                        those structured payloads carry an answer and rationale before the learner responds.

                        Practice is an ordinary conversation exchange. Describe what the learner's response
                        demonstrates on this occasion without claiming lasting mastery. Conversational
                        agreement is not a formal contest verdict and does not change the original Grade,
                        answer, memory tracker, or recall schedule.

                        When the wording of the original question does not support a unique answer, so
                        that the learner's selected choice is also defensible, you may propose excluding
                        this attempt as an ambiguous question: call AmbiguousQuestionExclusionProposal
                        once, with a reason that explains the defensible interpretation using the original
                        question, the selected answer, and the available evidence. Propose it only for a
                        selected answer that was marked incorrect, and only for genuine ambiguity. Do not
                        propose it for a misconception about meaning: when the question or its source
                        fixes one meaning, a choice that is grammatical but means something else is wrong,
                        and ordinary feedback explains the difference in meaning.

                        A proposal changes nothing. Only the learner's confirmation excludes the attempt.
                        Until the learner confirms, and when the learner declines, the mark, Grade, and
                        recall schedule stay as they were. Do not say or imply that the attempt has been
                        excluded or corrected unless the attempt record contains
                        attemptExcludedAsAmbiguous. When it does, the learner has already confirmed the
                        exclusion: do not propose it again, and explain that the attempt no longer counts
                        as failed or successful recall while the original mark stays visible.

                        Keep using the ordinary conversation and available tools. Discussing a question or
                        source problem alone is not a request to update a note.
                        """)
                  .build()));
    }

    items.add(
        ResponseInputItem.ofEasyInputMessage(
            EasyInputMessage.builder()
                .role(EasyInputMessage.Role.DEVELOPER)
                .content("Make tool calls when user asks to update the note.")
                .build()));
  }
}
