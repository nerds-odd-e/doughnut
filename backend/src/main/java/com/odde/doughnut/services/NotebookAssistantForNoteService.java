package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.AiAssistantResponse;
import com.odde.doughnut.controllers.dto.ToolCallResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.ai.*;
import com.odde.doughnut.services.commands.GetAiStreamCommand;
import com.theokanning.openai.assistants.message.MessageRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public final class NotebookAssistantForNoteService {
  private final AssistantService assistantService;
  private final Note note;
  private final ObjectMapper objectMapper;

  public NotebookAssistantForNoteService(AssistantService assistantService, Note note) {
    this.assistantService = assistantService;
    this.note = note;
    this.objectMapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public SseEmitter getAiReplyForConversation(
      Conversation conversation, ConversationService conversationService) {

    String threadId = conversation.getAiAssistantThreadId();
    if (threadId == null) {
      threadId = createThread(List.of());
      conversationService.setConversationAiAssistantThreadId(conversation, threadId);
    }

    return new GetAiStreamCommand(conversation, conversationService, note, assistantService)
        .execute(threadId);
  }

  private String createThread(List<MessageRequest> additionalMessages) {
    List<MessageRequest> messages =
        new ArrayList<>(
            List.of(
                MessageRequest.builder()
                    .role("assistant")
                    .content(note.getNoteDescription())
                    .build()));
    if (additionalMessages != null && !additionalMessages.isEmpty()) {
      messages.addAll(additionalMessages);
    }
    return assistantService.createThread(messages);
  }

  public String suggestTopicTitle() {
    List<MessageRequest> messages =
        List.of(
            MessageRequest.builder()
                .role("user")
                .content("Please suggest a better topic title for the note.")
                .build());

    try {
      TopicTitleReplacement replacement =
          executeAssistantProcess(
              messages,
              TopicTitleReplacement.class,
              (runService, threadResponse) -> runService.cancelRun());

      return replacement.newTopic;
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to parse topic title replacement", e);
    }
  }

  public TextFromAudio audioTranscriptionToArticle(String transcription)
      throws JsonProcessingException {
    List<MessageRequest> messages =
        List.of(
            MessageRequest.builder()
                .role("user")
                .content(
                    """
          You are a helpful assistant for converting audio transcription in SRT format to text of paragraphs. Your task is to convert the following audio transcription to text with meaningful punctuations and paragraphs.
           * Fix obvious audio transcription mistakes.
           * Do not translate the text to another language (unless asked to).
           * If the transcription is not clear, leave the text as it is.
           * Don't add any additional information than what is in the transcription.
           * Call function to append text from audio to complete the current note details, so add necessary white space or new line at the beginning to connect to existing text.
           * The context should be in markdown format.

           Here's the transcription from audio:
           ------------
          """
                        + transcription)
                .build());

    NoteDetailsCompletion noteDetails =
        executeAssistantProcess(
            messages,
            NoteDetailsCompletion.class,
            (runService, threadResponse) -> {
              try {
                runService.submitToolOutputs(
                    threadResponse.getToolCalls().getFirst().getId(),
                    new ToolCallResult("appended"));
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            });

    TextFromAudio textFromAudio = new TextFromAudio();
    textFromAudio.setCompletionMarkdownFromAudio(noteDetails.completion);
    return textFromAudio;
  }

  private <T> T executeAssistantProcess(
      List<MessageRequest> userMessages,
      Class<T> responseType,
      BiConsumer<AssistantRunService, AiAssistantResponse> runServiceAction)
      throws JsonProcessingException {
    String threadId = createThread(userMessages);
    AiAssistantResponse threadResponse = assistantService.createRunAndGetThreadResponse(threadId);
    AssistantRunService runService =
        assistantService.getAssistantRunService(threadId, threadResponse.getRunId());
    if (runServiceAction != null) {
      runServiceAction.accept(runService, threadResponse);
    }

    // Parse the first tool call's function's arguments into responseType
    return objectMapper.readValue(
        threadResponse.getToolCalls().getFirst().getFunction().getArguments().toString(),
        responseType);
  }
}
