package com.odde.doughnut.services.ai.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.controllers.dto.NoteRefinementQuestionContextDTO;
import com.odde.doughnut.services.ai.NoteExtractionResult;
import com.odde.doughnut.services.ai.NoteRefinementLayout;
import com.odde.doughnut.services.ai.NoteRefinementLayoutValidator;
import com.odde.doughnut.services.ai.RegeneratedNoteContent;
import com.odde.doughnut.services.focusContext.FocusContextConstants;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class NoteRefinementAiToolFactory {

  private NoteRefinementAiToolFactory() {}

  static InstructionAndSchema generateNoteRefinementLayoutAiTool(
      NoteRefinementQuestionContextDTO questionContext) {
    String ledToQuestionGuidance =
        questionContext == null
            ? "Set ledToQuestion to false for every item."
            : questionLedLayoutGuidance(questionContext);
    return new InstructionAndSchema(
        """
        Return one current-content layout for the note content, not alternative breakdown suggestions.

        Context:
        - The user message includes hidden "%s" with a "%s" section and optional "## Retrieved Note" sections.
        - Build the layout from the Focus Note content only. The Focus Note is the only source for layout items.
        - Retrieved Notes are secondary context only: use them to clarify scope or ambiguous Focus Note content, but do not add layout items for content that appears only in Retrieved Notes. If a Retrieved Note clarifies a Focus Note section, reflect that in the Focus Note item text.

        The layout must have at most two levels: top-level items and optional child items. Do not create grandchildren.
        Each item text should describe the Focus Note content represented by that point.
        Give every item a stable id that is unique within the layout.
        Set alreadyExtracted to true only for simple standalone wiki-link-only lines that point to content already extracted into another note, for example [[Target note]] or [[Target note|Label]]. These items should be marked Already extracted in the UI but remain selectable.
        %s
        """
            .formatted(
                FocusContextConstants.FOCUS_CONTEXT_OPEN_MARKER,
                FocusContextConstants.FOCUS_NOTE_OPEN_MARKER,
                ledToQuestionGuidance),
        NoteRefinementLayout.class);
  }

  private static String questionLedLayoutGuidance(
      NoteRefinementQuestionContextDTO questionContext) {
    StringBuilder questionBlock = new StringBuilder();
    questionBlock.append("The learner answered this multiple-choice question about the note:\n");
    questionBlock.append("Question stem:\n");
    questionBlock.append(nullToEmpty(questionContext.getStem())).append("\n\n");
    questionBlock.append("Choices:\n");
    List<String> choices =
        questionContext.getChoices() == null ? List.of() : questionContext.getChoices();
    if (choices.isEmpty()) {
      questionBlock.append("(none)\n");
    } else {
      IntStream.range(0, choices.size())
          .forEach(
              i ->
                  questionBlock
                      .append(i)
                      .append(". ")
                      .append(nullToEmpty(choices.get(i)))
                      .append('\n'));
    }
    if (questionContext.getCorrectAnswerIndex() != null) {
      questionBlock
          .append("Correct answer index: ")
          .append(questionContext.getCorrectAnswerIndex())
          .append('\n');
    }
    if (questionContext.getTestedFocus() != null && !questionContext.getTestedFocus().isBlank()) {
      questionBlock.append("Tested focus: ").append(questionContext.getTestedFocus()).append('\n');
    }
    return """
        %s
        When building the layout:
        - Separate content points that led to or are tested by this question as distinct layout items at the appropriate hierarchy level (top-level or child).
        - Set ledToQuestion=true on those items that capture the knowledge the question was testing or that led to this question.
        - Set ledToQuestion=false on all other items.
        """
        .formatted(questionBlock.toString().stripTrailing());
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  static InstructionAndSchema removeSelectedLayoutPointsFromContentAiTool(
      NoteRefinementLayout layout, List<String> selectedItemIds) {
    String layoutJson = formatLayoutForPrompt(layout);
    String selectedItemsBlock = formatSelectedItemsForPrompt(layout, selectedItemIds);
    String instruction =
        """
        You need to remove selected layout points from the note content. Carefully identify and completely remove all content related to each selected point. After removal, rewrite the remaining content into coherent, well-structured markdown while preserving all other information.

        Full note layout:
        %s

        Selected layout item ids to remove: %s

        Selected item texts:
        %s

        Important guidelines:
        1. Identify content that matches or relates to each selected layout point listed above
        2. Completely remove all sentences, paragraphs, or sections that contain or relate to these selected points
        3. Selected points may be non-contiguous; keep unrelated content unchanged
        4. Ensure the remaining content flows naturally and maintains coherence
        5. Items marked alreadyExtracted in the layout are still valid user selections; remove their represented content as requested
        6. Output only the new content in markdown format
        """
            .formatted(layoutJson, selectedItemIds, selectedItemsBlock);
    return new InstructionAndSchema(instruction, RegeneratedNoteContent.class);
  }

  static InstructionAndSchema extractNoteAiTool(
      NoteRefinementLayout layout, List<String> selectedItemIds) {
    String layoutJson = formatLayoutForPrompt(layout);
    String selectedItemsBlock = formatSelectedItemsForPrompt(layout, selectedItemIds);
    String instruction =
        """
        You are helping extract selected layout points from a note to create one new note.

        Full note layout:
        %s

        Selected layout item ids to extract together into one new note: %s

        Selected item texts:
        %s

        Tasks:
        1. Generate a concise, meaningful title for the new note based on the selected points
        2. Identify the related content in the current note for these selected points (they may be non-contiguous)
        3. Move that content to the new note's content
        4. Remove the extracted content from the current note

        Guidelines:
        - The new note's content should be based on the extracted content from current note, refined for clarity
        - Do not add new information that was not in the original content
        - Keep all unrelated parts of the current note unchanged
        - Ensure the remaining content in current note still reads naturally after removing non-contiguous selections
        - Items marked alreadyExtracted in the layout are still valid user selections; extract their represented content as requested
        - You receive focus-note context plus related notes. Prefer replacing the removed content in the original note with a natural contextual wiki link to the new note.
        - Do not add YAML frontmatter or metadata properties, such as parent:, merely to backlink the new note to the original note.
        - Include YAML frontmatter in the new note only when it is part of the extracted content itself or clearly semantically necessary.
        - Never use a generic parent property as the default extraction relationship.
        - When helpful, add wiki links from the new note body back to the original note or to relevant related notes from the provided context.
        - Wiki links are case-insensitive. Use display text when useful, for example [[Canonical Note Title|visible text]].
        - Do not invent unrelated wiki links.
        - The note title is stored separately from the body. Do not repeat newNoteTitle as a markdown heading in newNoteContent (for example "# Title" or "## Title" at the start). Begin the body with the actual content.
        """
            .formatted(layoutJson, selectedItemIds, selectedItemsBlock);

    return new InstructionAndSchema(instruction, NoteExtractionResult.class);
  }

  private static String formatLayoutForPrompt(NoteRefinementLayout layout) {
    try {
      return new ObjectMapperConfig()
          .objectMapper()
          .writerWithDefaultPrettyPrinter()
          .writeValueAsString(layout);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private static String formatSelectedItemsForPrompt(
      NoteRefinementLayout layout, List<String> selectedItemIds) {
    return NoteRefinementLayoutValidator.selectedItems(layout, selectedItemIds).stream()
        .map(item -> "- %s: \"%s\"".formatted(item.id, item.text))
        .collect(Collectors.joining("\n"));
  }
}
