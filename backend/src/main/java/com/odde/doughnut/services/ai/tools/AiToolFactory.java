package com.odde.doughnut.services.ai.tools;

import com.odde.doughnut.controllers.json.ClarifyingQuestionAndAnswer;
import com.odde.doughnut.services.ai.*;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.service.FunctionExecutor;
import java.util.Collections;

public class AiToolFactory {
  public static AiTool<MCQWithAnswer> mcqWithAnswerAiTool() {
    return new AiTool<>(
        MCQWithAnswer.class,
        "ask_single_answer_multiple_choice_question",
        "Ask a single-answer multiple-choice question to the user",
        """
      Please assume the role of a Memory Assistant, which involves helping me review, recall, and reinforce information from my notes. As a Memory Assistant, focus on creating exercises that stimulate memory and comprehension. Please adhere to the following guidelines:

      1. Generate a MCQ based on the note in the current context path
      2. Only the top-level of the context path is visible to the user.
      3. Provide 2 to 4 choices with only 1 correct answer.
      4. Vary the lengths of the choice texts so that the correct answer isn't consistently the longest.
      5. If there's insufficient information in the note to create a question, leave the 'stem' field empty.

      Note: The specific note of focus and its more detailed contexts are not known. Focus on memory reinforcement and recall across various subjects.
      """);
  }

  public static AiTool<QuestionEvaluation> questionEvaluationAiTool(MCQWithAnswer question) {
    MultipleChoicesQuestion clone = question.getMultipleChoicesQuestion();

    String messageBody =
        """
Please assume the role of a learner, who has learned the note of focus as well as many other notes.
Only the top-level of the context path is visible to you.
Without the specific note of focus and its more detailed contexts revealed to you,
please critically check if the following question makes sense and is possible to you:

%s

"""
            .formatted(clone.toJsonString());

    return new AiTool<>(
        QuestionEvaluation.class,
        "evaluate_question",
        "answer and evaluate the feasibility of the question",
        messageBody);
  }

  public static FunctionExecutor getFunctionExecutor(ClarifyingQuestionAndAnswer qa) {
    FunctionExecutor functionExecutor =
        new FunctionExecutor(
            Collections.singletonList(
                ChatFunction.builder()
                    .name("ask_clarification_question")
                    .description("Ask question to get more context")
                    .executor(
                        ClarifyingQuestion.class,
                        w ->
                            new OpenAIChatAboutNoteRequestBuilder.UserResponseToClarifyingQuestion(
                                qa.answerFromUser))
                    .build()));
    return functionExecutor;
  }
}
