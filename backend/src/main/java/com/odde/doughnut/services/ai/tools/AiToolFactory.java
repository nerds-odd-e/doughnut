package com.odde.doughnut.services.ai.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.entities.NoteType;
import com.odde.doughnut.entities.RelationType;
import com.odde.doughnut.services.ai.*;
import java.util.List;

public class AiToolFactory {

  public static Class<?> askSingleAnswerMultipleChoiceQuestion() {
    return MCQWithAnswer.class;
  }

  public static InstructionAndSchema mcqWithAnswerAiTool() {
    return mcqWithAnswerAiTool(null, null);
  }

  public static InstructionAndSchema mcqWithAnswerAiTool(RelationType relationType) {
    return mcqWithAnswerAiTool(relationType, null);
  }

  public static InstructionAndSchema mcqWithAnswerAiTool(
      RelationType relationType, NoteType noteType) {
    String relationTypeInstruction = getRelationTypeInstruction(relationType);
    String noteTypeInstruction = getNoteTypeInstruction(noteType);

    // 5. **Empty Stems When Necessary**: Leave the question stem empty if there's insufficient
    // information to create a meaningful question.
    String baseInstruction =
        """
        Please act as a Question Designer, testing my memory, mastery and understanding of my focus note.
        My notes are atomic pieces of knowledge organized hierarchically and can include relations to form lateral links.
        Your task is to create a memory-stimulating question by adhering to these guidelines:

        1. **Focus on the Focus Note**: Formulate one question EXCLUSIVELY around the focus note (its title / subject-predicate-target and details).
        2. **Leverage the Extended Graph**:
           - Use other focus note info and related notes to enrich the question formulation.
           - Avoid accidental bias by ensuring the focus note isn't falsely assumed to be the sole specialization of a general concept.
           - Related notes often serve as excellent distractor choices for the MCQs. But avoid more than 1 correct answers.
        3. **Generate Multiple-Choice Questions (MCQs)**:
           - Provide 2 to 3 options, with ONLY one correct answer.
           - Vary the length of answer choices to avoid patterns where the correct answer is consistently the longest.
           - Use markdown for both the question stem and the answer choices.
        4. **Ensure Question Self-Sufficiency**:
           - Ensure the question provides all necessary context within the stem and choices.
           - Avoid vague phrasing like "this X" or "the following X" unless the X is explicitly defined in the stem or choices.
           - IMPORTANT: Avoid using "this note"!!! User won't know which note you are referring to.
        6. **Make sure correct choice index is accurate**:
           - The correct choice is also exclusive, and plausible.
           - Ensure distractor choices are logical but clearly incorrect (without needing to be obvious).
      	7. **Choice order semantics (strictChoiceOrder)**:
           - In typical MCQs without meta-choices (‘All of the above’, ‘None of the above’, ‘Only A and B’), strictChoiceOrder must ALWAYS be false.

      """;
    String fullInstruction = baseInstruction;
    if (relationTypeInstruction != null) {
      fullInstruction = fullInstruction + "\n" + relationTypeInstruction;
    }
    if (noteTypeInstruction != null) {
      fullInstruction = fullInstruction + "\n" + noteTypeInstruction;
    }
    return new InstructionAndSchema(fullInstruction, askSingleAnswerMultipleChoiceQuestion());
  }

  private static String getRelationTypeInstruction(RelationType relationType) {
    if (relationType == null) {
      return null;
    }
    return relationType.getQuestionGenerationInstruction();
  }

  private static String getNoteTypeInstruction(NoteType noteType) {
    if (noteType == null) {
      return null;
    }
    return noteType.getQuestionGenerationInstruction();
  }

  public static InstructionAndSchema questionEvaluationAiTool(MCQWithAnswer question) {
    MultipleChoicesQuestion mcq = question.getF0__multipleChoicesQuestion();

    String messageBody =
        """
        You are an AI assistant evaluating a memory recall question for a user’s personal knowledge management (PKM) system. The user has provided a hierarchical knowledge graph centered around a focus note along with a multiple-choice question that is meant to test their recollection of that focus note. However, the user does not know which note is the focus note when answering.

        Your task is to analyze the provided question and determine whether it effectively tests the user’s memory of the focus note while adhering to the following evaluation criteria:
            1.	Selecting Correct Answers: Try to select all correct answers from the given choices.
            2.	Feasibility of the Question: Consider whether the question, without revealing the focus note title, is logically understandable and answerable based on what a user should reasonably remember.
            3.	Logical Consistency: Ensure that the question logically follows from the focus note and its knowledge graph without assuming misleading or incorrect relationships.
            4.	Relevance to the Focus Note: The question should not just be related to the extended knowledge graph; it should be closely tied to the focus note itself and not merely any related concepts.
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
    MultipleChoicesQuestion mcq = question.getF0__multipleChoicesQuestion();

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
            .formatted(new ObjectMapperConfig().objectMapper().valueToTree(mcq).toString());

    return new InstructionAndSchema(messageBody, MCQWithAnswerForRefinement.class);
  }

  public static InstructionAndSchema transcriptionToTextAiTool(String transcriptionFromAudio) {
    return new InstructionAndSchema(
        """
            You convert SRT-format audio transcriptions into coherent paragraphs with proper punctuation, formatted in Markdown. Guidelines:
              •	Output only function calls with a unified diff showing how to append the processed text to existing note details, adding necessary whitespace or a new line at the beginning.
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
        completeNoteDetails());
  }

  public static InstructionAndSchema suggestNoteTitleAiTool() {
    return new InstructionAndSchema(
        "Please suggest a better title for the note. Don't change it if it's already good enough.",
        suggestNoteTitle());
  }

  public static InstructionAndSchema generateUnderstandingChecklistAiTool() {
    return new InstructionAndSchema(
        "Please generate an understanding checklist of the note details broken down into key points. Each point should be a complete sentence that captures an important aspect of the note content. The checklist should help the user check whether they truly understood the important points in the note. There should only be a maximum of 5 points.",
        generateUnderstandingChecklist());
  }

  public static InstructionAndSchema regenerateDetailsFromPointsAiTool(List<String> points) {
    String pointsBlock = String.join("\n", points);
    String message =
        """
            Based on the following points, regenerate the note details. Rewrite them into coherent, well-structured markdown. Preserve the meaning of each point. Output only the new details.

            Points:
            %s
            """
            .formatted(pointsBlock);
    return new InstructionAndSchema(message, RegeneratedNoteDetails.class);
  }

  public static InstructionAndSchema removePointsFromDetailsAiTool(List<String> pointsToRemove) {
    String pointsBlock = String.join("\n", pointsToRemove);
    String message =
        """
            You need to remove specific points from the note details. Carefully identify and completely remove all content related to each of the following points. After removal, rewrite the remaining content into coherent, well-structured markdown while preserving all other information that is not related to the points to be removed.

            Important guidelines:
            1. Identify content that matches or relates to each point listed below
            2. Completely remove all sentences, paragraphs, or sections that contain or relate to these points
            3. Ensure the remaining content flows naturally and maintains coherence
            4. Preserve all other information that is unrelated to the points to be removed
            5. Output only the new details in markdown format

            Points to remove:
            %s
            """
            .formatted(pointsBlock);
    return new InstructionAndSchema(message, RegeneratedNoteDetails.class);
  }

  public static List<Class<?>> getAllAssistantTools() {
    return List.of(
        completeNoteDetails(),
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

  public static Class<?> completeNoteDetails() {
    return NoteDetailsCompletion.class;
  }

  public static Class<?> evaluateQuestion() {
    return QuestionEvaluation.class;
  }

  public static InstructionAndSchema promotePointToChildAiTool(String point) {
    String instruction =
        """
        You are helping extract a point from a note to create a new child note.

        Given point: "%s"

        Tasks:
        1. Generate a concise, meaningful title for the new child note based on this point
        2. Expand the point into detailed content (in markdown) for the new note
        3. Identify the related content in the parent note's details
        4. Replace that content with a brief summary (1-2 sentences) that references the key concept

        Guidelines:
        - The new note should be self-contained and comprehensive
        - The summary in parent note should maintain reading flow
        - Keep all unrelated parts of parent details unchanged
        """
            .formatted(point);

    return new InstructionAndSchema(instruction, promotePoint());
  }

  public static InstructionAndSchema promotePointToSiblingAiTool(String point) {
    String instruction =
        """
        You are helping extract a point from a note to create a new sibling note.

        Given point: "%s"

        Tasks:
        1. Generate a concise, meaningful title for the new sibling note based on this point
        2. Expand the point into detailed content (in markdown) for the new note
        3. Identify and completely remove the related content from the parent note's details

        Guidelines:
        - The new note should be self-contained and comprehensive
        - Simply remove the extracted point from parent note's details (do not replace with summary)
        - Keep all unrelated parts of parent details unchanged
        - Ensure the remaining content in parent note still reads naturally
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
