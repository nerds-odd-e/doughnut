package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.ai.*;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.commands.GetAiStreamHelper;
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
    final String[] result = new String[1];
    createThread(List.of())
        .withTool(AiToolFactory.suggestNoteTopicTitle())
        .withInstructions(
            "Please suggest a better topic title for the note by calling the function. Don't change it if it's already good enough.")
        .run()
        .getToolCallResponse(
            (runService, threadResponse, parsedResponse) -> {
              TopicTitleReplacement replacement = (TopicTitleReplacement) parsedResponse;
              result[0] = replacement.newTopic;
              runService.cancelRun();
            });
    return result[0];
  }

  public TextFromAudio audioTranscriptionToArticle(
      String previousNoteDetails,
      String transcription,
      String threadId,
      String runId,
      String toolCallId)
      throws JsonProcessingException {
    final TextFromAudio textFromAudio = new TextFromAudio();
    textFromAudio.setRawSRT(transcription);

    if (threadId != null && !threadId.isEmpty() && runId != null && !runId.isEmpty()) {
      // Use existing thread and run
      assistantService
          .getThread(threadId)
          .withTool(AiToolFactory.completeNoteDetails())
          .resumeRun(runId)
          .submitToolOutputs(
              toolCallId,
              new AudioToTextToolCallResult(
                  "Previous content was appended, now there's more to process",
                  transcription,
                  previousNoteDetails))
          .getToolCallResponse(
              (runService, tcId, parsedResponse) -> {
                NoteDetailsCompletion noteDetails = (NoteDetailsCompletion) parsedResponse;
                textFromAudio.setCompletionMarkdownFromAudio(noteDetails.completion);
                textFromAudio.setThreadId(threadId);
                textFromAudio.setRunId(runService.getRunId());
                textFromAudio.setToolCallId(tcId);
              });

      return textFromAudio;
    }

    // Original flow for new threads
    String content = "Here's the new transcription from audio:\n------------\n" + transcription;
    MessageRequest message = MessageRequest.builder().role("user").content(content).build();

    createThread(List.of(message))
        .withTool(AiToolFactory.completeNoteDetails())
        .withInstructions(
            """
      You are a helpful assistant for converting audio transcription in SRT format to text of paragraphs. Your task is to convert the following audio transcription to text with meaningful punctuations and paragraphs.
       * Fix obvious audio transcription mistakes.
       * Do not translate the text to another language (unless asked to).
       * If the transcription is not clear, leave the text as it is.
       * Don't add any additional information than what is in the transcription.
       * Call function to append text from audio to complete the current note details, so add necessary white space or new line at the beginning to connect to existing text.
       * The context should be in markdown format.

      """)
        .run()
        .getToolCallResponse(
            (runService, tcId, parsedResponse) -> {
              NoteDetailsCompletion noteDetails = (NoteDetailsCompletion) parsedResponse;
              textFromAudio.setCompletionMarkdownFromAudio(noteDetails.completion);
              textFromAudio.setThreadId(runService.getThreadId());
              textFromAudio.setRunId(runService.getRunId());
              textFromAudio.setToolCallId(tcId);
            });

    return textFromAudio;
  }
}
