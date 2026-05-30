package com.odde.doughnut.services.ai.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.controllers.dto.BookLayoutReorganizationSuggestion;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.services.ai.*;
import java.util.List;

public class AiToolFactory {

  public static Class<?> askSingleAnswerMultipleChoiceQuestion() {
    return MCQWithAnswer.class;
  }

  public static InstructionAndSchema mcqWithAnswerAiTool(boolean focusNoteContentEmpty) {
    return new InstructionAndSchema(
        getBaseInstruction(focusNoteContentEmpty), askSingleAnswerMultipleChoiceQuestion());
  }

  private static String getBaseInstruction(boolean focusNoteContentEmpty) {
    String correctAnswerSupportRule =
        focusNoteContentEmpty
            ? "- The correct answer must be supported by its title or direct references."
            : "- The correct answer must be supported by the Focus Note title or content.";
    String verifyGroundingRule =
        focusNoteContentEmpty
            ? "- The correct answer is grounded in its title or direct references."
            : "- The correct answer is grounded in the Focus Note.";
    return """
    Create one memory-stimulating, single-answer MCQ that helps the learner recall the Focus Note.

    You are a Question Designer for a personal knowledge system.

    Context:
 .  '''
    Input:
    - The user message contains hidden "# Focus Context".
    - The learner cannot see this context.
    - "Focus Note" is the primary source.
    - "Retrieved Note" sections are secondary context only.
    '''

    Rules:
    %s
    - Retrieved Notes may clarify context or provide distractors, but must not be necessary to know the correct answer.
    - Do not use external knowledge as the basis for the correct answer.
    - If the Focus Note is weak, generic, uncertain, truncated, or mostly empty, test only the most concrete stable point available and note the limitation in validationRationale.
    - The stem must be self-contained.
    - Learner-facing fields must not say "focus note", "retrieved note", "above context", "this note", or "according to the context".
    - Provide exactly 4 choices.
    - Do not prefix choices with labels such as "A.", "B.", "C.", "1.", "2.", or "3."; provide only the choice text.
    - Exactly one choice must be correct under a reasonable interpretation.
    - Distractors must be plausible but clearly incorrect.
    - Do not use meta-choices such as "All of the above", "None of the above", "Both A and B", "A and C only", or any choice that refers to another choice.
    - Each choice must be independent and safe to reorder.
    - Set choicesMayBeShuffled to true.
    - Avoid making the correct answer consistently longer or more specific than the distractors.
    - Markdown is allowed only when useful for clarity.

    Before output, silently verify:
    - One and only one correct answer.
    %s
    - No hidden-context labels appear in learner-facing text.
    - No choice labels or numbering appear inside the choices.
    - No meta-choices or order-dependent choices are used.
    - choicesMayBeShuffled is true.
    - The question tests recall rather than outside knowledge.

    Output:
    - Return only JSON matching the provided schema.

        """
        .formatted(correctAnswerSupportRule, verifyGroundingRule);
  }

  public static InstructionAndSchema questionEvaluationAiTool(MCQWithAnswer question) {
    MultipleChoicesQuestion mcq = question.getQuestion();

    String messageBody =
        """
        You are an AI assistant evaluating a memory recall question for a user’s personal knowledge management (PKM) system. The user has hidden Markdown context around a focus note (wiki-linked notes and inbound references, possibly beyond one link away) along with a multiple-choice question meant to test recollection of that focus note. The user does not know which note is the focus note when answering.

        Your task is to analyze the provided question and determine whether it effectively tests the user’s memory of the focus note while adhering to the following evaluation criteria:
            1.	Selecting Correct Answers: Try to select all correct answers from the given choices.
            2.	Feasibility of the Question: Consider whether the question, without revealing the focus note title, is logically understandable and answerable based on what a user should reasonably remember.
            3.	Logical Consistency: Ensure that the question logically follows from the focus note and its supporting context without assuming misleading or incorrect relationships.
            4.	Relevance to the Focus Note: The question should not just be related to indirectly linked notes; it should be closely tied to the focus note itself and not merely any related concepts.
            5.	Avoiding Simplicity or Obviousness: The question should not be too trivial or easily guessable without requiring meaningful recall of the focus note.

        Output Requirements:
            •	If the question fails any of the above criteria, set the field "feasibleQuestion": false in the response.
            •	Provide a clear explanation of why the question is not feasible and what could be improved in the field "improvementAdvices". If the question is too far off, please advise to generate completely new question.

        Here is the question to evaluate:

        %s

        """
            .formatted(new ObjectMapperConfig().objectMapper().valueToTree(mcq).toString());

    return new InstructionAndSchema(messageBody, QuestionEvaluation.class);
  }

  public static InstructionAndSchema questionRefineAiTool(MCQWithAnswer question) {
    MultipleChoicesQuestion mcq = question.getQuestion();

    String messageBody =
"""
Please assume the role of a Memory Assistant, which involves helping me recall and reinforce information from my notes. As a Memory Assistant, focus on creating exercises that stimulate memory and comprehension. Please adhere to the following guidelines:

      1. Examine the below MCQ, which may be incomplete or incorrect. It was written against hidden Markdown context that includes the focus note and linked or referencing notes.
      2. The user does not see that hidden context; the stem and choices must stand alone.
      3. Provide 2 to 4 choices with only 1 correct answer.
      4. Vary the lengths of the choice texts so that the correct answer isn't consistently the longest.
      5. Provide a better question based on my question and the note. Please correct any grammar.

      Note: The specific focus note and retrieved neighbors are not shown to the user. Focus on memory reinforcement and recall across various subjects.
%s

"""
            .formatted(new ObjectMapperConfig().objectMapper().valueToTree(mcq).toString());

    return new InstructionAndSchema(messageBody, MCQWithAnswerForRefinement.class);
  }

  public static InstructionAndSchema transcriptionToTextAiTool(String transcriptionFromAudio) {
    return new InstructionAndSchema(
        """
            You convert SRT-format audio transcriptions into coherent paragraphs with proper punctuation, formatted in Markdown. Guidelines:
              •	Output only function calls with a unified diff showing how to append the processed text to existing note content, adding necessary whitespace or a new line at the beginning.
              •	Do not translate the text unless requested.
              • Do not interpret the text. Do not use reported speech.
              •	Leave unclear parts unchanged.
              •	Do not add any information not present in the transcription.
              •	The transcription may be truncated; do not add new lines or whitespace at the end.
              •	Provide the diff in unified diff format (text format) with lines prefixed by '-' for deletions, '+' for additions, and ' ' (space) for context.

             Here's the new transcription from audio:
             ------------
            """
            + transcriptionFromAudio,
        completeNoteContent());
  }

  public static InstructionAndSchema suggestNoteTitleAiTool() {
    return new InstructionAndSchema(
        "Please suggest a better title for the note. Don't change it if it's already good enough.",
        suggestNoteTitle());
  }

  public static InstructionAndSchema generateUnderstandingChecklistAiTool() {
    return new InstructionAndSchema(
        "Please generate an understanding checklist of the note content broken down into key points. Each point should be a complete sentence that captures an important aspect of the note. The checklist should help the user check whether they truly understood the important points in the note. There should only be a maximum of 5 points.",
        generateUnderstandingChecklist());
  }

  public static InstructionAndSchema bookLayoutReorganizationAiTool() {
    String instruction =
        """
        You reorganize the outline nesting of book blocks for a PDF-derived book layout.

        The user message is a JSON array of objects, each with: id (integer), title (string), depth (integer).
        The list order is fixed (preorder: parent before descendants). Titles often encode section numbering (e.g. "1.", "1.1", "Chapter 2").

        Task: propose corrected depths so the outline reflects the true hierarchy. Root-level sections have depth 0.
        Depths must form a valid preorder tree: the first block has depth 0; each next block's depth is at most one greater than the previous block's depth, and never negative.

        Output: return exactly one entry per input id with the suggested depth. Include every id exactly once. Do not add or remove ids. Preserve the logical roles implied by titles and numbering.
        """;
    return new InstructionAndSchema(instruction, bookLayoutReorganizationSuggestion());
  }

  public static Class<?> bookLayoutReorganizationSuggestion() {
    return BookLayoutReorganizationSuggestion.class;
  }

  public static InstructionAndSchema removePointsFromContentAiTool(List<String> pointsToRemove) {
    String pointsBlock = String.join("\n", pointsToRemove);
    String message =
        """
            You need to remove specific points from the note content. Carefully identify and completely remove all content related to each of the following points. After removal, rewrite the remaining content into coherent, well-structured markdown while preserving all other information that is not related to the points to be removed.

            Important guidelines:
            1. Identify content that matches or relates to each point listed below
            2. Completely remove all sentences, paragraphs, or sections that contain or relate to these points
            3. Ensure the remaining content flows naturally and maintains coherence
            4. Preserve all other information that is unrelated to the points to be removed
            5. Output only the new content in markdown format

            Points to remove:
            %s
            """
            .formatted(pointsBlock);
    return new InstructionAndSchema(message, RegeneratedNoteContent.class);
  }

  public static List<Class<?>> getAllAssistantTools() {
    return List.of(
        completeNoteContent(),
        suggestNoteTitle(),
        askSingleAnswerMultipleChoiceQuestion(),
        evaluateQuestion());
  }

  public static Class<?> suggestNoteTitle() {
    return TitleReplacement.class;
  }

  public static Class<?> generateUnderstandingChecklist() {
    return UnderstandingChecklist.class;
  }

  public static Class<?> completeNoteContent() {
    return NoteContentCompletion.class;
  }

  public static Class<?> evaluateQuestion() {
    return QuestionEvaluation.class;
  }

  public static InstructionAndSchema promotePointToSiblingAiTool(String point) {
    String instruction =
        """
        You are helping extract a point from a note to create a new note.

        Point to extract: "%s"

        Tasks:
        1. Generate a concise, meaningful title for the new note based on this point
        2. Identify the related content in the current note for this point
        3. Move that content to the new note's content
        4. Remove the extracted content from the current note

        Guidelines:
        - The new note's content should be based on the extracted content from current note, refined for clarity
        - Do not add new information that was not in the original content
        - Keep all unrelated parts of the current note unchanged
        - Ensure the remaining content in current note still reads naturally
        - You receive focus-note context plus related notes. When helpful, add a wiki link from the original note to the newly promoted note.
        - When helpful, add wiki links from the new note back to the original note or to relevant related notes from the provided context.
        - Wiki links are case-insensitive. Use display text when useful, for example [[Canonical Note Title|visible text]].
        - Do not invent unrelated wiki links.
        """
            .formatted(point);

    return new InstructionAndSchema(instruction, promotePoint());
  }

  public static Class<?> promotePoint() {
    return PointExtractionResult.class;
  }

  public static String buildRegenerateQuestionMessage(
      QuestionContestResult contestResult, MCQWithAnswer mcqWithAnswer) {
    String mcq = null;
    try {
      mcq =
          new ObjectMapperConfig()
              .objectMapper()
              .writerWithDefaultPrettyPrinter()
              .writeValueAsString(mcqWithAnswer);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    return """
                Previously generated non-feasible question:

                %s

                Improvement advice:

                %s

                Please regenerate or refine the question based on the above advice."""
        .formatted(mcq, contestResult.advice);
  }
}
