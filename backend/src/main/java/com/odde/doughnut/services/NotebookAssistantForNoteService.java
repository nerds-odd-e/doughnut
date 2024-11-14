package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.AiAssistantResponse;
import com.odde.doughnut.controllers.dto.ToolCallResult;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.ai.*;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.commands.GetAiStreamCommand;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.run.RunCreateRequest;
import java.util.List;
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
      threadId = assistantService.createThread(List.of(getNoteDescriptionMessage()));
      conversationService.setConversationAiAssistantThreadId(conversation, threadId);
    }

    return new GetAiStreamCommand(conversation, conversationService, note, assistantService)
        .execute(threadId);
  }

  private MessageRequest getNoteDescriptionMessage() {
    return MessageRequest.builder().role("assistant").content(note.getNoteDescription()).build();
  }

  public String suggestTopicTitle() {
    MessageRequest message =
        MessageRequest.builder()
            .role("user")
            .content("Please suggest a better topic title for the note.")
            .build();

    try {
      final String[] result = new String[1];
      executeAssistantProcess(
          message,
          AiToolFactory.suggestNoteTopicTitle(),
          (runService, threadResponse, parsedResponse) -> {
            TopicTitleReplacement replacement = (TopicTitleReplacement) parsedResponse;
            result[0] = replacement.newTopic;
            runService.cancelRun();
          });
      return result[0];
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to parse topic title replacement", e);
    }
  }

  public TextFromAudio audioTranscriptionToArticle(String transcription)
      throws JsonProcessingException {
    String content =
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
            + transcription;
    MessageRequest message = MessageRequest.builder().role("user").content(content).build();

    final TextFromAudio textFromAudio = new TextFromAudio();
    executeAssistantProcess(
        message,
        AiToolFactory.completeNoteDetails(),
        (runService, toolCallId, parsedResponse) -> {
          try {
            NoteDetailsCompletion noteDetails = (NoteDetailsCompletion) parsedResponse;
            textFromAudio.setCompletionMarkdownFromAudio(noteDetails.completion);
            runService.submitToolOutputs(toolCallId, new ToolCallResult("appended"));
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        });

    return textFromAudio;
  }

  private void executeAssistantProcess(
      MessageRequest userMessage,
      AiTool tool,
      TriConsumer<AssistantRunService, String, Object> runServiceAction)
      throws JsonProcessingException {
    String threadId =
        assistantService.createThread(List.of(getNoteDescriptionMessage(), userMessage));
    RunCreateRequest.RunCreateRequestBuilder builder =
        RunCreateRequest.builder().tools(List.of(tool.getTool()));
    AiAssistantResponse threadResponse =
        assistantService.createRunAndGetThreadResponse(threadId, builder);
    AssistantRunService runService =
        assistantService.getAssistantRunService(threadId, threadResponse.getRunId());
    if (runServiceAction != null) {
      Object parsedResponse =
          objectMapper.readValue(
              threadResponse.getToolCalls().getFirst().getFunction().getArguments().toString(),
              tool.parameterClass());
      runServiceAction.accept(
          runService, threadResponse.getToolCalls().getFirst().getId(), parsedResponse);
    }
  }
}
