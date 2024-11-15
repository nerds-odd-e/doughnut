package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.AiAssistantResponse;
import com.odde.doughnut.controllers.dto.AudioTranscriptConversionConfig;
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
      thread = createThread(List.of());
      conversationService.setConversationAiAssistantThreadId(conversation, thread.getThreadId());
    } else {
      thread = assistantService.getThread(conversation.getAiAssistantThreadId());
    }
    return thread;
  }

  private AssistantThread createThread(List<MessageRequest> additionalMessages) {
    List<MessageRequest> messages = new ArrayList<>();
    messages.add(getNoteDescriptionMessage());
    if (!additionalMessages.isEmpty()) {
      messages.addAll(additionalMessages);
    }
    return assistantService.createThread(messages);
  }

  private MessageRequest getNoteDescriptionMessage() {
    return MessageRequest.builder().role("assistant").content(note.getNoteDescription()).build();
  }

  public String suggestTopicTitle() throws JsonProcessingException {
    OpenAiRun openAiRun =
        createThread(List.of())
            .withTool(AiToolFactory.suggestNoteTopicTitle())
            .withInstructions(
                "Please suggest a better topic title for the note by calling the function. Don't change it if it's already good enough.")
            .run();
    AiAssistantResponse toolCallResponse = openAiRun.getToolCallResponse();
    openAiRun.cancelRun();
    TopicTitleReplacement replacement = (TopicTitleReplacement) toolCallResponse.getFirstArgument();
    return replacement.newTopic;
  }

  public TextFromAudio audioTranscriptionToArticle(
      String transcriptionFromAudio, AudioTranscriptConversionConfig config)
      throws JsonProcessingException {

    if (config.getThreadId() != null
        && !config.getThreadId().isEmpty()
        && config.getRunId() != null
        && !config.getRunId().isEmpty()) {
      try {
        String instruction =
            "Previous content was appended, now there's more to process. Note that this is to be appended to the previous note details and the transcription could be from audio that was truncated in the middle of a sentence or word. Follow the same run instructions.";

        if (config.getAdditionalProcessingInstructions() != null
            && !config.getAdditionalProcessingInstructions().isEmpty()) {
          instruction +=
              "\nAdditional instruction:\n" + config.getAdditionalProcessingInstructions();
        }

        OpenAiRun openAiRun =
            assistantService
                .getThread(config.getThreadId())
                .withTool(AiToolFactory.completeNoteDetails())
                .resumeRun(config.getRunId())
                .submitToolOutputs(
                    config.getToolCallId(),
                    new AudioToTextToolCallResult(
                        instruction, transcriptionFromAudio, config.getPreviousNoteDetails()));

        if (openAiRun.isRequiresAction()) {
          return getTextFromAudioFromOngoingRun(transcriptionFromAudio, openAiRun);
        }

      } catch (OpenAiHttpException e) {
        // Fallback to creating a new thread if submission fails
      }
    }

    return createNewThreadForTranscription(transcriptionFromAudio, config);
  }

  private static TextFromAudio getTextFromAudioFromOngoingRun(
      String transcription, OpenAiRun openAiRun) throws JsonProcessingException {
    AiAssistantResponse toolCallResponse = openAiRun.getToolCallResponse();

    NoteDetailsCompletion noteDetails = (NoteDetailsCompletion) toolCallResponse.getFirstArgument();
    final TextFromAudio textFromAudio = new TextFromAudio();
    textFromAudio.setRawSRT(transcription);
    textFromAudio.setCompletionMarkdownFromAudio(noteDetails.completion);
    textFromAudio.setThreadId(openAiRun.getThreadId());
    textFromAudio.setRunId(openAiRun.getRunId());
    textFromAudio.setToolCallId(toolCallResponse.getFirstToolCallId());
    return textFromAudio;
  }

  private TextFromAudio createNewThreadForTranscription(
      String transcription, AudioTranscriptConversionConfig config) throws JsonProcessingException {
    String content = "Here's the new transcription from audio:\n------------\n" + transcription;
    MessageRequest message = MessageRequest.builder().role("user").content(content).build();

    String instructions =
        """
          You are a helpful assistant for converting audio transcription in SRT format to text of paragraphs. Your task is to convert the following audio transcription to text with meaningful punctuations and paragraphs.
           * Fix obvious audio transcription mistakes.
           * Do not translate the text to another language (unless asked to).
           * If the transcription is not clear, leave the text as it is.
           * Don't add any additional information than what is in the transcription.
           * Call function to append text from audio to complete the current note details, so add necessary white space or new line at the beginning to connect to existing text.
           * The transcription could be from audio that was truncated in the middle of a sentence or word. So never add new lines or white spaces at the end of the output.
           * The context should be in markdown format.
          """;

    if (config.getAdditionalProcessingInstructions() != null
        && !config.getAdditionalProcessingInstructions().isEmpty()) {
      instructions += "\nAdditional instruction:\n" + config.getAdditionalProcessingInstructions();
    }

    OpenAiRun openAiRun =
        createThread(List.of(message))
            .withTool(AiToolFactory.completeNoteDetails())
            .withInstructions(instructions)
            .run();
    return getTextFromAudioFromOngoingRun(transcription, openAiRun);
  }
}
