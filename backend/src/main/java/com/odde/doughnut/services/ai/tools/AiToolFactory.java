package com.odde.doughnut.services.ai.tools;

import static com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder.askClarificationQuestion;

import com.odde.doughnut.services.ai.*;
import com.theokanning.openai.completion.chat.ChatFunction;
import java.util.List;

public class AiToolFactory {
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
            ChatFunction.builder()
                .name("ask_single_answer_multiple_choice_question")
                .description("Ask a single-answer multiple-choice question to the user")
                .executor(MCQWithAnswer.class, null)
                .build()));
  }

  public static AiToolList questionEvaluationAiTool(MCQWithAnswer question) {
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

    return new AiToolList(
        messageBody,
        List.of(
            ChatFunction.builder()
                .name("evaluate_question")
                .description("answer and evaluate the feasibility of the question")
                .executor(QuestionEvaluation.class, null)
                .build()));
  }

  public static AiToolList getNoteContentCompletionTools(String completionPrompt) {
    List<ChatFunction> functions =
        List.of(
            ChatFunction.builder()
                .name("complete_note_details")
                .description("Text completion for the details of the note of focus")
                .executor(NoteDetailsCompletion.class, null)
                .build(),
            ChatFunction.builder()
                .name(askClarificationQuestion)
                .description("Ask question to get more context")
                .executor(ClarifyingQuestion.class, null)
                .build());
    return new AiToolList(completionPrompt, functions);
  }
}
