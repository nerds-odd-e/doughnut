package com.odde.doughnut.services.ai.tools;

import static com.odde.doughnut.services.ai.tools.AiToolName.ASK_SINGLE_ANSWER_MULTIPLE_CHOICE_QUESTION;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.ai.*;
import java.util.List;

public class AiToolFactory {

  public static AiTool askSingleAnswerMultipleChoiceQuestion() {
    return new AiTool(
        ASK_SINGLE_ANSWER_MULTIPLE_CHOICE_QUESTION.getValue(),
        "Ask a single-answer multiple-choice question to the user",
        MCQWithAnswer.class);
  }

  public static InstructionAndSchema mcqWithAnswerAiTool() {
    return new InstructionAndSchema(
        """
        Please act as a Question Designer, testing my memory, mastery and understanding of my focus note.
        My notes are atomic pieces of knowledge organized hierarchically and can include reifications to form lateral links.
        Your task is to create a memory-stimulating question by adhering to these guidelines:

        1. **Focus on the Focus Note**: Formulate one question EXCLUSIVELY around the focus note (its title / subject-predicate-object and details).
        2. **Leverage the Extended Graph**:
           - Use other focus note info and related notes to enrich the question formulation.
           - Avoid accidental bias by ensuring the focus note isn’t falsely assumed to be the sole specialization of a general concept.
           - Related notes often serve as excellent distractor choices for the MCQs. But avoid more than 1 correct answers.
        3. **Context Visibility**:
           - Avoid explicitly mentioning the focus note title in stem
           - Focus note can appear in the choices when necessary.
        4. **Generate Multiple-Choice Questions (MCQs)**:
           - Provide 2 to 3 options, with ONLY one correct answer.
           - Vary the length of answer choices to avoid patterns where the correct answer is consistently the longest.
           - Use markdown for both the question stem and the answer choices.
        6. **Ensure Question Self-Sufficiency**:
           - Ensure the question provides all necessary context within the stem and choices.
           - Avoid vague phrasing like "this X" or "the following X" unless the X is explicitly defined in the stem or choices.
           - IMPORTANT: Avoid using "this note"!!! User won't know which note you are referring to.
        7. **Empty Stems When Necessary**: Leave the question stem empty if there’s insufficient information to create a meaningful question.
        8. **Make sure correct choice index is accurate**:
           - The correct choice is also exclusive, and plausible.
           - Ensure distractor choices are logical but clearly incorrect (without needing to be obvious).
        9. **Output Handling**:
           - MUST provide the question via the function `%s`. If question generation fails, still output using this function.
           - Create only one question and make only one call to the function.

      """
            .formatted(ASK_SINGLE_ANSWER_MULTIPLE_CHOICE_QUESTION.getValue()),
        askSingleAnswerMultipleChoiceQuestion());
  }

  public static InstructionAndSchema questionEvaluationAiTool(MCQWithAnswer question) {
    MultipleChoicesQuestion mcq = question.getMultipleChoicesQuestion();

    String messageBody =
        """
        Please assume the role of a learner, who has learned the note of focus as well as many other notes.
        Only the top-level of the contextual path is visible to you.
        Without the specific note of focus and its more detailed contexts revealed to you,
        please critically check if the following question makes sense and is possible to you:

        %s

        """
            .formatted(new ObjectMapper().valueToTree(mcq).toString());

    return new InstructionAndSchema(
        messageBody,
        new AiTool(
            "evaluate_question",
            "answer and evaluate the feasibility of the question",
            QuestionEvaluation.class));
  }

  public static InstructionAndSchema questionRefineAiTool(MCQWithAnswer question) {
    MultipleChoicesQuestion mcq = question.getMultipleChoicesQuestion();

    String messageBody =
        """
Please assume the role of a Memory Assistant, which involves helping me review, recall, and reinforce information from my notes. As a Memory Assistant, focus on creating exercises that stimulate memory and comprehension. Please adhere to the following guidelines:

      1. Review the below MCQ which is based on the note in the current contextual path, the MCQ could be incomplete or incorrect.
      2. Only the top-level of the contextual path is visible to the user.
      3. Provide 2 to 4 choices with only 1 correct answer.
      4. Vary the lengths of the choice texts so that the correct answer isn't consistently the longest.
      5. Provide a better question based on my question and the note. Please correct any grammar.

      Note: The specific note of focus and its more detailed contexts are not known. Focus on memory reinforcement and recall across various subjects.
%s

"""
            .formatted(new ObjectMapper().valueToTree(mcq).toString());

    return new InstructionAndSchema(
        messageBody, new AiTool("refine_question", "refine the question", MCQWithAnswer.class));
  }

  public static InstructionAndSchema transcriptionToTextAiTool(String transcriptionFromAudio) {
    return new InstructionAndSchema(
        """
            You convert SRT-format audio transcriptions into coherent paragraphs with proper punctuation, formatted in Markdown. Guidelines:
              •	Output only function calls to append the processed text to existing note details, adding necessary whitespace or a new line at the beginning.
              •	Do not translate the text unless requested.
              • Do not interpret the text. Do not use reported speech.
              •	Leave unclear parts unchanged.
              •	Do not add any information not present in the transcription.
              •	The transcription may be truncated; do not add new lines or whitespace at the end.

             Here's the new transcription from audio:
             ------------
            """
            + transcriptionFromAudio,
        completeNoteDetails());
  }

  public static List<AiTool> getAllAssistantTools() {
    return List.of(
        completeNoteDetails(),
        suggestNoteTitle(),
        askSingleAnswerMultipleChoiceQuestion(),
        evaluateQuestion());
  }

  public static AiTool suggestNoteTitle() {
    return new AiTool(
        AiToolName.SUGGEST_NOTE_TITLE.getValue(),
        "Generate a concise and accurate note title based on the note content and pass it to the function for the use to update their note. The title should be a single word, a phrase or at most a single sentence that captures the atomic concept of the note. It should be specific within the note's contextual path and do not need to include general information that's already in the contextual path. Keep the existing title if it's already correct and concise.",
        TitleReplacement.class);
  }

  public static AiTool completeNoteDetails() {
    return new AiTool(
        AiToolName.COMPLETE_NOTE_DETAILS.getValue(),
        "Text completion for the details of the note of focus",
        NoteDetailsCompletion.class);
  }

  public static AiTool evaluateQuestion() {
    return new AiTool(
        "evaluate_question",
        "answer and evaluate the question to check its quality",
        QuestionEvaluation.class);
  }
}
