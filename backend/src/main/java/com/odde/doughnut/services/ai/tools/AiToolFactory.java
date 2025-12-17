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
    return switch (noteType) {
      case CONCEPT ->
          """
          **Special Instruction for Concept Note**: The focus note represents a concept or idea. Generate questions that test conceptual understanding:
          - Ask how the concept works, why it matters, or what principles it embodies
          - Test understanding of relationships: how this concept connects to or differs from related concepts
          - Ask about applications: how the concept is used in practice or real-world scenarios
          - Test deeper understanding: mechanisms, causes, effects, or underlying principles
          - Ask about the concept's significance, implications, or role in a broader context
          The question should require reasoning and understanding, not just recall of facts or definitions.
          """;
      case SOURCE ->
          """
          **Special Instruction for Source Note**: The focus note represents a source of information (book, article, website, etc.). Generate questions that test knowledge of the source:
          - Ask about key information, ideas, or content from the source
          - Test understanding of the source's main arguments, themes, or contributions
          - Ask about the source's author, publication details, or context
          - Test recall of specific details, quotes, or examples from the source
          - Ask about how the source relates to other notes or concepts
          The question should focus on what can be learned from or about the source.
          """;
      case PERSON ->
          """
          **Special Instruction for Person Note**: The focus note represents a person. Generate questions that test knowledge about the person:
          - Ask about the person's background, achievements, or contributions
          - Test understanding of the person's role, significance, or impact
          - Ask about the person's relationships, connections, or associations with other notes
          - Test recall of specific details about the person's life, work, or characteristics
          - Ask about the person's relevance to other concepts or events
          The question should focus on what makes this person notable or important.
          """;
      case EXPERIENCE ->
          """
          **Special Instruction for Experience Note**: The focus note represents a personal experience or event. Generate questions that test recall of the experience:
          - Ask about what happened: specific events, actions, or occurrences described in the note
          - Test temporal recall: when events occurred, sequence of events, or chronological details
          - Ask about who was involved, where events took place, or other factual details from the experience
          - Test recall of personal insights, reflections, emotions, or significance described in the note
          - Ask about connections: how this experience relates to other events or what it led to
          The question should focus on recalling the specific experience documented, not on general knowledge.
          """;
      case INITIATIVE ->
          """
          **Special Instruction for Initiative Note**: The focus note represents a project, initiative, or undertaking. Generate questions that test knowledge of the initiative:
          - Ask about the initiative's goals, objectives, or purpose
          - Test understanding of the initiative's scope, timeline, or key milestones
          - Ask about the initiative's outcomes, results, or impact
          - Test recall of specific details about how the initiative was executed or managed
          - Ask about the initiative's relationship to other notes, concepts, or people
          The question should focus on what the initiative is, what it achieved, or how it was carried out.
          """;
      case QUEST ->
          """
          **Special Instruction for Quest Note**: The focus note represents a quest, goal, or objective. Generate questions that test knowledge of the quest:
          - Ask about the quest's purpose, goals, or desired outcomes
          - Test understanding of the quest's requirements, steps, or milestones
          - Ask about progress, status, or completion of the quest
          - Test recall of specific details about the quest's context, challenges, or strategies
          - Ask about the quest's relationship to other notes, experiences, or initiatives
          The question should focus on what the quest is, what it aims to achieve, or how it progresses.
          """;
      default -> null;
    };
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

  public static InstructionAndSchema generateSummaryAiTool() {
    return new InstructionAndSchema(
        "Please generate a summary of the note details broken down into key points. Each point should be a complete sentence that captures an important aspect of the note content. The summary should help the user quickly understand the main ideas in the note. There should only be a maximum of 5 points.",
        generateSummary());
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

  public static Class<?> generateSummary() {
    return NoteSummary.class;
  }

  public static Class<?> completeNoteDetails() {
    return NoteDetailsCompletion.class;
  }

  public static Class<?> evaluateQuestion() {
    return QuestionEvaluation.class;
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
