package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.AudioUploadDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.ai.*;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.commands.GetAiStreamHelper;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.assistants.message.MessageRequest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public final class NotebookAssistantForNoteService {
  private final OpenAiAssistant assistantService;
  private final Note note;
  private final GlobalSettingsService globalSettingsService;

  public NotebookAssistantForNoteService(
      OpenAiAssistant openAiAssistant, Note note, GlobalSettingsService globalSettingsService) {
    this.assistantService = openAiAssistant;
    this.note = note;
    this.globalSettingsService = globalSettingsService;
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
        .withAdditionalInstructions(
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

  // Common method to handle OpenAiRunResult processing
  private <T> T processRunResult(OpenAiRunResult result, Class<T> expectedType)
      throws JsonProcessingException {
    return switch (result) {
      case OpenAiRunRequiredAction action -> {
        T argument = expectedType.cast(action.getFirstArgument());
        action.cancelRun();
        yield argument;
      }
      case OpenAiRunCompleted _ -> null;
    };
  }

  // Common method to create and run thread with tools
  private OpenAiRunResult createAndRunThread(
      List<MessageRequest> messages, AiTool tool, String instructions)
      throws JsonProcessingException {
    return createThreadWithNoteInfo(messages)
        .withTool(tool)
        .withAdditionalInstructions(instructions)
        .run()
        .getRunResult();
  }

  public String suggestTopicTitle() throws JsonProcessingException {
    String instructions =
        "Please suggest a better topic title for the note by calling the function. Don't change it if it's already good enough.";
    OpenAiRunResult result =
        createAndRunThread(List.of(), AiToolFactory.suggestNoteTopicTitle(), instructions);

    TopicTitleReplacement replacement = processRunResult(result, TopicTitleReplacement.class);
    return replacement != null ? replacement.newTopic : null;
  }

  public MCQWithAnswer generateQuestion() throws JsonProcessingException {
    MessageRequest message =
        MessageRequest.builder()
            .role("user")
            .content(AiToolFactory.mcqWithAnswerAiTool().getMessageBody())
            .build();

    OpenAiRunResult result =
        createThreadWithNoteInfo(List.of(message))
            .withTool(AiToolFactory.askSingleAnswerMultipleChoiceQuestion())
            .withFileSearch()
            .withModelName(globalSettingsService.globalSettingQuestionGeneration().getValue())
            .run()
            .getRunResult();

    MCQWithAnswer question = processRunResult(result, MCQWithAnswer.class);
    if (question != null
        && question.getMultipleChoicesQuestion().getStem() != null
        && !Strings.isBlank(question.getMultipleChoicesQuestion().getStem())) {
      return question;
    }
    return null;
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
    OpenAiRunResult result =
        getOpenAiRunExpectingAction(transcriptionFromAudio, config).getRunResult();

    return switch (result) {
      case OpenAiRunRequiredAction action -> {
        final TextFromAudioWithCallInfo textFromAudio = new TextFromAudioWithCallInfo();
        NoteDetailsCompletion noteDetails = (NoteDetailsCompletion) action.getFirstArgument();
        textFromAudio.setCompletionMarkdownFromAudio(noteDetails.completion);
        textFromAudio.setToolCallInfo(action.getToolCallInfo());
        yield textFromAudio;
      }
      case OpenAiRunCompleted _ ->
          throw new IllegalStateException("Expected tool call action but got completed run");
    };
  }

  private OpenAiRunExpectingAction getOpenAiRunExpectingAction(
      String transcriptionFromAudio, AudioUploadDTO config) throws JsonProcessingException {
    if (config.hasValidThreadAndRunId()) {
      try {
        String instruction =
            "Previous content was appended. More transcription to process; append it to the previous output. The previous output could be incomplete sentence, so only start new sentence or new line when make sense. Follow the same instructions as before.";

        instruction = appendAdditionalInstructions(instruction, config);

        Map<String, Object> results = new HashMap<>();
        results.put(
            config.getToolCallId(),
            new AudioToTextToolCallResult(instruction, transcriptionFromAudio));
        return assistantService
            .getThread(config.getThreadId())
            .withTool(AiToolFactory.completeNoteDetails())
            .resumeRun(config.getRunId())
            .submitToolOutputs(results);

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
        .withAdditionalInstructions(instructions)
        .run();
  }
}
