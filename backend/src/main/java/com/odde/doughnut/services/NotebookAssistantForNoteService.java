package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.AudioUploadDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.ai.*;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.commands.GetAiStreamHelper;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.assistants.message.MessageRequest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public final class NotebookAssistantForNoteService {
  private final OpenAiAssistant assistantService;
  private final Note note;

  public NotebookAssistantForNoteService(OpenAiAssistant openAiAssistant, Note note) {
    this.assistantService = openAiAssistant;
    this.note = note;
  }

  public SseEmitter getAiReplyForConversation(
      Conversation conversation, ConversationService conversationService) {

    AssistantThread thread = getOrCreateThread(conversation, conversationService);

    Timestamp lastAiAssistantThreadSync = conversation.getLastAiAssistantThreadSync();
    if (lastAiAssistantThreadSync != null && note.getUpdatedAt().after(lastAiAssistantThreadSync)) {
      thread.createAssistantMessage(
          "The note content has been update:\n\n%s".formatted(note.getNoteDescription()));
    }
    List<ConversationMessage> unseen = conversation.getUnseenMessagesByAssistant();
    if (!unseen.isEmpty()) {
      thread.createUserMessage(GetAiStreamHelper.formatUnsentMessages(unseen));
    }
    conversationService.updateLastAiAssistantThreadSync(conversation);

    return thread
        .withInstructions(
            "User is seeking for having a conversation, so don't call functions to update the note unless user asks explicitly.")
        .runStream()
        .getSseEmitter(
            (message -> {
              String content = GetAiStreamHelper.extractMessageContent(message);
              conversationService.addMessageToConversation(conversation, null, content);
            }));
  }

  private AssistantThread getOrCreateThread(
      Conversation conversation, ConversationService conversationService) {
    AssistantThread thread;
    if (conversation.getAiAssistantThreadId() == null) {
      thread = createThreadWithNoteInfo(List.of());
      conversationService.setConversationAiAssistantThreadId(conversation, thread.getThreadId());
    } else {
      thread = assistantService.getThread(conversation.getAiAssistantThreadId());
    }
    return thread;
  }

  private AssistantThread createThreadWithNoteInfo(List<MessageRequest> additionalMessages) {
    List<MessageRequest> messages = new ArrayList<>();
    messages.add(
        MessageRequest.builder().role("assistant").content(note.getNoteDescription()).build());
    if (!additionalMessages.isEmpty()) {
      messages.addAll(additionalMessages);
    }
    return assistantService.createThread(messages);
  }

  public String suggestTopicTitle() throws JsonProcessingException {
    String instructions =
        "Please suggest a better topic title for the note by calling the function. Don't change it if it's already good enough.";

    TopicTitleReplacement argument =
        (TopicTitleReplacement)
            createThreadWithNoteInfo(List.of())
                .withTool(AiToolFactory.suggestNoteTopicTitle())
                .withInstructions(instructions)
                .run()
                .getToolCallResponse()
                .cancelRun()
                .getFirstArgument();
    return argument.newTopic;
  }

  private String appendAdditionalInstructions(String instructions, AudioUploadDTO config) {
    if (config.getAdditionalProcessingInstructions() != null
        && !config.getAdditionalProcessingInstructions().isEmpty()) {
      return instructions
          + "\nAdditional instruction:\n"
          + config.getAdditionalProcessingInstructions();
    }
    return instructions;
  }

  public TextFromAudioWithCallInfo audioTranscriptionToArticle(
      String transcriptionFromAudio, AudioUploadDTO config) throws JsonProcessingException {
    OpenAiRun toolCallResponse =
        getOpenAiRunExpectingAction(transcriptionFromAudio, config).getToolCallResponse();
    final TextFromAudioWithCallInfo textFromAudio = new TextFromAudioWithCallInfo();
    NoteDetailsCompletion noteDetails = (NoteDetailsCompletion) toolCallResponse.getFirstArgument();
    textFromAudio.setCompletionMarkdownFromAudio(noteDetails.completion);
    textFromAudio.setToolCallInfo(toolCallResponse.getToolCallInfo());
    return textFromAudio;
  }

  private OpenAiRunExpectingAction getOpenAiRunExpectingAction(
      String transcriptionFromAudio, AudioUploadDTO config) throws JsonProcessingException {
    if (config.getThreadId() != null
        && !config.getThreadId().isEmpty()
        && config.getRunId() != null
        && !config.getRunId().isEmpty()) {
      try {
        String instruction =
            "Previous content was appended. More transcription to process; append it to the previous output. Follow the same instructions as before.";

        instruction = appendAdditionalInstructions(instruction, config);

        return assistantService
            .getThread(config.getThreadId())
            .withTool(AiToolFactory.completeNoteDetails())
            .resumeRun(config.getRunId())
            .submitToolOutputs(
                config.getToolCallId(),
                new AudioToTextToolCallResult(instruction, transcriptionFromAudio));

      } catch (OpenAiHttpException e) {
        // Fallback to creating a new thread if submission fails
      }
    }
    return createNewThreadForTranscription(transcriptionFromAudio, config);
  }

  private OpenAiRunExpectingAction createNewThreadForTranscription(
      String transcription, AudioUploadDTO config) throws JsonProcessingException {
    String content = "Here's the new transcription from audio:\n------------\n" + transcription;
    MessageRequest message = MessageRequest.builder().role("user").content(content).build();

    String instructions =
        """
          You convert SRT-format audio transcriptions into coherent paragraphs with proper punctuation, formatted in Markdown. Guidelines:
          	•	Output only function calls to append the processed text to existing notes, adding necessary whitespace or a new line at the beginning.
          	•	Do not translate the text unless requested.
          	• Do not interpret the text. Do not use reported speech.
          	•	Leave unclear parts unchanged.
          	•	Do not add any information not present in the transcription.
          	•	The transcription may be truncated; do not add new lines or whitespace at the end.
          """;

    instructions = appendAdditionalInstructions(instructions, config);

    return createThreadWithNoteInfo(List.of(message))
        .withTool(AiToolFactory.completeNoteDetails())
        .withInstructions(instructions)
        .run();
  }
}
