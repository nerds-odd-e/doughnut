package com.odde.doughnut.services.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.ai.*;
import com.theokanning.openai.function.FunctionDefinition;
import java.util.List;

public class AiToolFactory {

  public static final String COMPLETE_NOTE_DETAILS = "complete_note_details";
  public static final String GENERATE_TOPIC_TITLE = "generate_topic_title";

  public static AiToolList mcqWithAnswerAiTool() {
    return new AiToolList(
        """
      Please assume the role of a Memory Assistant, which involves helping me review, recall, and reinforce information from my notes. As a Memory Assistant, focus on creating exercises that stimulate memory and comprehension. Please adhere to the following guidelines:

      1. Generate a MCQ based on the note in the current context path
      2. Only the top-level of the context path is visible to the user.
      3. Provide 2 to 4 choices with only 1 correct answer.
      4. Vary the lengths of the choice texts so that the correct answer isn't consistently the longest.
      5. If there's insufficient information in the note to create a question, leave the 'stem' field empty.

      Note: The specific note of focus and its more detailed contexts are not known. Focus on memory reinforcement and recall across various subjects.
      """,
        List.of(
            FunctionDefinition.<MCQWithAnswer>builder()
                .name("ask_single_answer_multiple_choice_question")
                .description("Ask a single-answer multiple-choice question to the user")
                .parametersDefinitionByClass(MCQWithAnswer.class)
                .build()));
  }

  public static AiToolList questionEvaluationAiTool(MCQWithAnswer question) {
    MultipleChoicesQuestion mcq = question.getMultipleChoicesQuestion();

    String messageBody =
        """
Please assume the role of a learner, who has learned the note of focus as well as many other notes.
Only the top-level of the context path is visible to you.
Without the specific note of focus and its more detailed contexts revealed to you,
please critically check if the following question makes sense and is possible to you:

%s

"""
            .formatted(new ObjectMapper().valueToTree(mcq).toString());

    return new AiToolList(
        messageBody,
        List.of(
            FunctionDefinition.<QuestionEvaluation>builder()
                .name("evaluate_question")
                .description("answer and evaluate the feasibility of the question")
                .parametersDefinitionByClass(QuestionEvaluation.class)
                .build()));
  }

  public static AiToolList questionRefineAiTool(MCQWithAnswer question) {
    MultipleChoicesQuestion mcq = question.getMultipleChoicesQuestion();

    String messageBody =
        """
Please assume the role of a Memory Assistant, which involves helping me review, recall, and reinforce information from my notes. As a Memory Assistant, focus on creating exercises that stimulate memory and comprehension. Please adhere to the following guidelines:

      1. Review the below MCQ which is based on the note in the current context path, the MCQ could be incomplete or incorrect.
      2. Only the top-level of the context path is visible to the user.
      3. Provide 2 to 4 choices with only 1 correct answer.
      4. Vary the lengths of the choice texts so that the correct answer isn't consistently the longest.
      5. Provide a better question based on my question and the note. Please correct any grammar.

      Note: The specific note of focus and its more detailed contexts are not known. Focus on memory reinforcement and recall across various subjects.
%s

"""
            .formatted(new ObjectMapper().valueToTree(mcq).toString());

    return new AiToolList(
        messageBody,
        List.of(
            FunctionDefinition.<MCQWithAnswer>builder()
                .name("refine_question")
                .description("refine the question")
                .parametersDefinitionByClass(MCQWithAnswer.class)
                .build()));
  }

  public static AiToolList transcriptionToTextAiTool(String transcriptionFromAudio) {
    return new AiToolList(
        """
            You are a helpful assistant for converting audio transcription in SRT format to text of paragraphs. Your task is to convert the following audio transcription to text with meaningful punctuations and paragraphs.
             * fix obvious audio transcription mistakes.
             * Do not translate the text to another language.
             * If the transcription is not clear, leave the text as it is.
             * Don't add any additional information than what is in the transcription.
             * the completionMarkdownFromAudio is to be appended after the previousTrailingNoteDetails, so add necessary white space or new line at the beginning to connect to existing text.
             * The context should be in markdown format.
            """
            + transcriptionFromAudio,
        List.of(
            FunctionDefinition.<TextFromAudio>builder()
                .name("audio_transcription_to_text")
                .description("Convert audio transcription to text")
                .parametersDefinitionByClass(TextFromAudio.class)
                .build()));
  }

  public static List<AiTool> getCompletionAiTools() {
    return List.of(
        new AiTool(
            COMPLETE_NOTE_DETAILS,
            "Text completion for the details of the note of focus",
            NoteDetailsCompletion.class),
        new AiTool(
            GENERATE_TOPIC_TITLE,
            "Generate a concise and descriptive title based on the note content",
            TopicTitleGeneration.class));
  }
}
