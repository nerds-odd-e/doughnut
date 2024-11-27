package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.AudioUploadDTO;
import com.odde.doughnut.services.ai.*;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.assistants.message.MessageRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NoteAutomationService {
  private final NotebookAssistantForNoteService notebookAssistantForNoteService;

  public NoteAutomationService(NotebookAssistantForNoteService notebookAssistantForNoteService) {
    this.notebookAssistantForNoteService = notebookAssistantForNoteService;
  }

  public String suggestTopicTitle() throws JsonProcessingException {
    String instructions =
        "Please suggest a better topic title for the note by calling the function. Don't change it if it's already good enough.";

    AiTool tool = AiToolFactory.suggestNoteTopicTitle();
    TopicTitleReplacement replacement =
        notebookAssistantForNoteService
            .createThreadWithNoteInfo(List.of())
            .withTool(tool)
            .withAdditionalInstructions(instructions)
            .run()
            .getRunResult()
            .getAssumedToolCallArgument(TopicTitleReplacement.class);
    return replacement != null ? replacement.newTopic : null;
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
    OpenAiRunExpectingAction runExpectingAction = null;
    boolean finished = false;
    if (config.hasValidThreadAndRunId()) {
      try {
        String instruction =
            "Previous content was appended. More transcription to process; append it to the previous output. The previous output could be incomplete sentence, so only start new sentence or new line when make sense. Follow the same instructions as before.";

        instruction = appendAdditionalInstructions(instruction, config);

        Map<String, Object> results = new HashMap<>();
        results.put(
            config.getToolCallId(),
            new AudioToTextToolCallResult(instruction, transcriptionFromAudio));
        runExpectingAction =
            notebookAssistantForNoteService
                .getThread(config.getThreadId())
                .withTool(AiToolFactory.completeNoteDetails())
                .resumeRun(config.getRunId())
                .submitToolOutputs(results);
        finished = true;

      } catch (OpenAiHttpException e) {
        // Fallback to creating a new thread if submission fails
      }
    }
    if (!finished) {
      String content =
          "Here's the new transcription from audio:\n------------\n" + transcriptionFromAudio;
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

      runExpectingAction =
          notebookAssistantForNoteService
              .createThreadWithNoteInfo(List.of(message))
              .withTool(AiToolFactory.completeNoteDetails())
              .withAdditionalInstructions(instructions)
              .run();
    }

    return switch (runExpectingAction.getRunResult()) {
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
}
