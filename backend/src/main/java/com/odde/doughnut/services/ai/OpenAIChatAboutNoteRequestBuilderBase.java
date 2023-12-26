package com.odde.doughnut.services.ai;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.odde.doughnut.controllers.json.AiCompletionParams;
import com.odde.doughnut.controllers.json.ClarifyingQuestionAndAnswer;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.theokanning.openai.completion.chat.*;
import java.util.HashMap;
import java.util.List;

public class OpenAIChatAboutNoteRequestBuilderBase {
  public static String askClarificationQuestion = "ask_clarification_question";
  protected final OpenAIChatRequestBuilder openAIChatRequestBuilder =
      new OpenAIChatRequestBuilder();

  public OpenAIChatAboutNoteRequestBuilderBase() {}

  public OpenAIChatAboutNoteRequestBuilderBase userInstructionToGenerateQuestionWithFunctionCall() {
    openAIChatRequestBuilder.functions.add(
        ChatFunction.builder()
            .name("ask_single_answer_multiple_choice_question")
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
    openAIChatRequestBuilder.addTextMessage(ChatMessageRole.USER, messageBody);
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilderBase instructionForDetailsCompletion(
      AiCompletionParams aiCompletionParams) {
    openAIChatRequestBuilder.functions.add(
        ChatFunction.builder()
            .name("complete_note_details")
            .description("Text completion for the details of the note of focus")
            .executor(NoteDetailsCompletion.class, null)
            .build());
    openAIChatRequestBuilder.functions.add(
        ChatFunction.builder()
            .name(askClarificationQuestion)
            .description("Ask question to get more context")
            .executor(ClarifyingQuestion.class, null)
            .build());

    HashMap<String, String> arguments = new HashMap<>();
    arguments.put("details_to_complete", aiCompletionParams.getDetailsToComplete());
    openAIChatRequestBuilder.addTextMessage(
        ChatMessageRole.USER,
        ("Please complete the concise details of the note of focus. Keep it short."
                + " Don't make assumptions about the context. Ask for clarification through tool function `%s` if my request is ambiguous."
                + " The current details in JSON format are: \n%s")
            .formatted(
                askClarificationQuestion,
                defaultObjectMapper().valueToTree(arguments).toPrettyString()));
    aiCompletionParams.getClarifyingQuestionAndAnswers().forEach(this::answeredClarifyingQuestion);

    return this;
  }

  public OpenAIChatAboutNoteRequestBuilderBase evaluateQuestion(MCQWithAnswer question) {
    openAIChatRequestBuilder.functions.add(
        ChatFunction.builder()
            .name("evaluate_question")
            .description("answer and evaluate the feasibility of the question")
            .executor(QuestionEvaluation.class, null)
            .build());

    MultipleChoicesQuestion clone = new MultipleChoicesQuestion();
    clone.stem = question.stem;
    clone.choices = question.choices;

    String messageBody =
        """
Please assume the role of a learner, who has learned the note of focus as well as many other notes.
Only the top-level of the context path is visible to you.
Without the specific note of focus and its more detailed contexts revealed to you,
please critically check if the following question makes sense and is possible to you:

%s

"""
            .formatted(clone.toJsonString());
    openAIChatRequestBuilder.addTextMessage(ChatMessageRole.USER, messageBody);
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilderBase evaluationResult(
      QuestionEvaluation questionEvaluation) {
    openAIChatRequestBuilder.addFunctionCallMessage(questionEvaluation, "evaluate_question");
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilderBase generatedQuestion(MCQWithAnswer preservedQuestion) {
    openAIChatRequestBuilder.addFunctionCallMessage(
        preservedQuestion, "ask_single_answer_multiple_choice_question");
    return this;
  }

  private void answeredClarifyingQuestion(ClarifyingQuestionAndAnswer qa) {
    ChatMessage functionCall =
        new ChatMessage(ChatMessageRole.ASSISTANT.value(), qa.answerFromUser);
    functionCall.setFunctionCall(
        new ChatFunctionCall(
            askClarificationQuestion, defaultObjectMapper().valueToTree(qa.questionFromAI)));
    openAIChatRequestBuilder.messages.add(functionCall);
    ChatMessage callResponse = new ChatMessage(ChatMessageRole.FUNCTION.value(), qa.answerFromUser);
    callResponse.setName(askClarificationQuestion);
    openAIChatRequestBuilder.messages.add(callResponse);
  }

  public OpenAIChatAboutNoteRequestBuilderBase rawNoteContent(String noteContent) {
    openAIChatRequestBuilder.addTextMessage(ChatMessageRole.SYSTEM, noteContent);
    return this;
  }

  public List<ChatMessage> buildMessages() {
    return openAIChatRequestBuilder.buildMessages();
  }

  public OpenAIChatAboutNoteRequestBuilderBase chatMessage(String userMessage) {
    openAIChatRequestBuilder.addTextMessage(ChatMessageRole.USER, userMessage);
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilderBase maxTokens(int maxTokens) {
    openAIChatRequestBuilder.maxTokens(maxTokens);
    return this;
  }

  public ChatCompletionRequest build() {
    return openAIChatRequestBuilder.build();
  }
}
