package com.odde.doughnut.services.ai;

import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.theokanning.openai.completion.chat.*;

public class AiTool {
  public static final String askSingleAnswerMultipleChoiceQuestion =
      "ask_single_answer_multiple_choice_question";

  public void userInstructionToGenerateQuestionWithFunctionCall(
      OpenAIChatRequestBuilder openAIChatRequestBuilder) {
    openAIChatRequestBuilder.functions.add(
        ChatFunction.builder()
            .name(askSingleAnswerMultipleChoiceQuestion)
            .description("Ask a single-answer multiple-choice question to the user")
            .executor(MCQWithAnswer.class, null)
            .build());

    String messageBody =
        """
        Please assume the role of a Memory Assistant, which involves helping me review, recall, and reinforce information from my notes. As a Memory Assistant, focus on creating exercises that stimulate memory and comprehension. Please adhere to the following guidelines:

        1. Generate a MCQ based on the note in the current context path
        2. Only the top-level of the context path is visible to the user.
        3. Provide 2 to 4 choices with only 1 correct answer.
        4. Vary the lengths of the choice texts so that the correct answer isn't consistently the longest.
        5. If there's insufficient information in the note to create a question, leave the 'stem' field empty.

        Note: The specific note of focus and its more detailed contexts are not known. Focus on memory reinforcement and recall across various subjects.
        """;
    openAIChatRequestBuilder.addUserMessage(messageBody);
  }

  public void generatedQuestion(
      OpenAIChatRequestBuilder openAIChatRequestBuilder, MCQWithAnswer preservedQuestion) {
    openAIChatRequestBuilder.addFunctionCallMessage(
        preservedQuestion, askSingleAnswerMultipleChoiceQuestion);
  }
}
