package com.odde.doughnut.services.ai.tools;

import static com.odde.doughnut.services.ai.tools.AiToolName.ASK_SINGLE_ANSWER_MULTIPLE_CHOICE_QUESTION;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.ai.*;
import com.theokanning.openai.function.FunctionDefinition;
import java.util.List;

public class AiToolFactory {

  public static AiTool askSingleAnswerMultipleChoiceQuestion() {
    return new AiTool(
        ASK_SINGLE_ANSWER_MULTIPLE_CHOICE_QUESTION.getValue(),
        "Ask a single-answer multiple-choice question to the user",
        MCQWithAnswer.class);
  }

  public static AiToolList mcqWithAnswerAiTool() {
    return new AiToolList(
        """
      Please assume the role of a Memory Assistant, which involves helping me review, recall, and reinforce information from my notes. As a Memory Assistant, focus on creating exercises that stimulate memory and comprehension. Please adhere to the following guidelines:

      1. Generate a MCQ based on the note in the current context path
      2. Only the top-level of the context path is visible to the user; Avoid referencing the “note of focus”; frame questions naturally without revealing its existence.
      3. Provide 2 to 4 choices with only 1 correct answer.
      4. Vary the lengths of the choice texts so that the correct answer isn't consistently the longest.
      5. The question should focus exclusively on the details of the note of focus, but the assistant must ensure accuracy by cross-referencing related notes (with file search) to avoid conflicts or ambiguities.
      6. If there's insufficient information in the note to create a question, leave the 'stem' field empty.

      """,
        List.of(askSingleAnswerMultipleChoiceQuestion().getFunctionDefinition()));
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
             * Do not translate the text to another language (unless asked to).
             * If the transcription is not clear, leave the text as it is.
             * Don't add any additional information than what is in the transcription.
             * the completionMarkdownFromAudio is to be appended after the previousTrailingNoteDetails, so add necessary white space or new line at the beginning to connect to existing text.
             * The context should be in markdown format.

             Here's the transcription from audio:
             ------------
            """
            + transcriptionFromAudio,
        List.of(
            FunctionDefinition.<TextFromAudio>builder()
                .name("audio_transcription_to_text")
                .description("Convert audio transcription to text")
                .parametersDefinitionByClass(TextFromAudio.class)
                .build()));
  }

  public static List<AiTool> getAllAssistantTools() {
    return List.of(
        completeNoteDetails(), suggestNoteTopicTitle(), askSingleAnswerMultipleChoiceQuestion());
  }

  public static AiTool suggestNoteTopicTitle() {
    return new AiTool(
        AiToolName.SUGGEST_NOTE_TOPIC_TITLE.getValue(),
        "Generate a concise and accurate note topic (a title) based on the note content and pass it to the function for the use to update their note. The topic should be a single word, a phrase or at most a single sentence that captures the atomic concept of the note. It should be specific within the note's context path and do not need to include general information that's already in the context path. Keep the existing topic if it's already correct and concise.",
        TopicTitleReplacement.class);
  }

  public static AiTool completeNoteDetails() {
    return new AiTool(
        AiToolName.COMPLETE_NOTE_DETAILS.getValue(),
        "Text completion for the details of the note of focus",
        NoteDetailsCompletion.class);
  }
}
