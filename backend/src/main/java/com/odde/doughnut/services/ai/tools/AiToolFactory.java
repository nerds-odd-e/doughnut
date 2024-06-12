package com.odde.doughnut.services.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.ai.*;
import com.theokanning.openai.function.FunctionDefinition;
import java.util.List;

public class AiToolFactory {

  public static final String COMPLETE_NOTE_DETAILS = "complete_note_details";

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

      1. Generate a MCQ based on the note in the current context path
      2. Only the top-level of the context path is visible to the user.
      3. Provide 2 to 4 choices with only 1 correct answer.
      4. Vary the lengths of the choice texts so that the correct answer isn't consistently the longest.
      5. If there's insufficient information in the note to create a question, keep the existing question as it is.
      6. Provide a better question based on my question and the note. Please correct any grammar.

      Note: The specific note of focus and its more detailed contexts are not known. Focus on memory reinforcement and recall across various subjects.
%s

"""
            .formatted(new ObjectMapper().valueToTree(mcq).toString());

    return new AiToolList(
        messageBody,
        List.of(
            FunctionDefinition.<MCQWithAnswer>builder()
                .name("refine_question")
                .description("answer and evaluate the feasibility of the question")
                .parametersDefinitionByClass(MCQWithAnswer.class)
                .build()));
  }
}
