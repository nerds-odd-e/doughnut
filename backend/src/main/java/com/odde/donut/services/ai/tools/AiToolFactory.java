package com.odde.donut.services.ai.tools;

import com.odde.donut.controllers.dto.BookLayoutReorganizationSuggestion;
import com.odde.donut.controllers.dto.NoteRefinementQuestionContextDTO;
import com.odde.donut.controllers.dto.QuestionContestResult;
import com.odde.donut.entities.Mcq;
import com.odde.donut.services.ai.*;
import com.odde.donut.services.focusContext.FocusContextConstants;
import java.util.List;

public class AiToolFactory {

  public static Class<?> askSingleAnswerMultipleChoiceQuestion() {
    return GeneratedMcq.class;
  }

  public static InstructionAndSchema mcqAiTool(boolean focusNoteContentEmpty) {
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
       - The user message contains hidden "%s".
       - The learner cannot see this context.
       - "Focus Note" is the primary source.
       - "Retrieved Note" sections are secondary context only.
       '''

       Rules:
       %s
       - Retrieved Notes may clarify context or provide distractors, but must not be necessary to know the correct answer.
       - Do not focus the question on wiki-linked parts of the Focus Note; those notes have their own memory trackers. Use them only as supporting context or distractors.
       - Do not use external knowledge as the basis for the correct answer.
       - If the Focus Note is weak, generic, uncertain, truncated, or mostly empty, test only the most concrete stable point available and note the limitation in validationRationale.
       - The stem must be self-contained.
       - Learner-facing fields must not say "focus note", "retrieved note", "above context", "this note", or "according to the context".
       - Provide one correct answer and exactly three distractors.
       - Do not prefix choices with labels such as "A.", "B.", "C.", "1.", "2.", or "3."; provide only the choice text.
       - Exactly one choice must be correct under a reasonable interpretation.
       - Distractors must be plausible but clearly incorrect.
       - Do not use meta-choices such as "All of the above", "None of the above", "Both A and B", "A and C only", or any choice that refers to another choice.
       - Each choice must be independent and safe to reorder.
       - Avoid making the correct answer consistently longer or more specific than the distractors.
       - Markdown is allowed only when useful for clarity.

       Before output, silently verify:
       - One and only one correct answer.
       %s
       - No hidden-context labels appear in learner-facing text.
       - No choice labels or numbering appear inside the choices.
       - No meta-choices or order-dependent choices are used.
       - The question tests recall rather than outside knowledge.

       Output:
       - Return only JSON matching the provided schema.

    """
        .formatted(
            FocusContextConstants.FOCUS_CONTEXT_OPEN_MARKER,
            correctAnswerSupportRule,
            verifyGroundingRule);
  }

  public static InstructionAndSchema generateNoteRefinementLayoutAiTool(
      NoteRefinementQuestionContextDTO questionContext) {
    return NoteRefinementAiToolFactory.generateNoteRefinementLayoutAiTool(questionContext);
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

  public static InstructionAndSchema removeSelectedLayoutPointsFromContentAiTool(
      NoteRefinementLayout layout, List<String> selectedItemIds) {
    return NoteRefinementAiToolFactory.removeSelectedLayoutPointsFromContentAiTool(
        layout, selectedItemIds);
  }

  public static List<Class<?>> getAllAssistantTools() {
    return List.of(
        completeNoteContent(), suggestNoteTitle(), askSingleAnswerMultipleChoiceQuestion());
  }

  public static Class<?> suggestNoteTitle() {
    return TitleReplacement.class;
  }

  public static Class<?> noteRefinementLayout() {
    return NoteRefinementLayout.class;
  }

  public static Class<?> completeNoteContent() {
    return NoteContentCompletion.class;
  }

  public static InstructionAndSchema extractNoteAiTool(
      NoteRefinementLayout layout, List<String> selectedItemIds) {
    return NoteRefinementAiToolFactory.extractNoteAiTool(layout, selectedItemIds);
  }

  public static Class<?> noteExtractionResult() {
    return NoteExtractionResult.class;
  }

  public static String buildRegenerateQuestionMessage(
      QuestionContestResult contestResult, Mcq mcq) {
    return RegenerateQuestionMessage.build(contestResult, mcq);
  }
}
