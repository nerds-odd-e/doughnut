package com.odde.donut.algorithms;

import java.util.List;
import java.util.Optional;

/**
 * Reads {@code question_generation_instruction} and the per-property-type {@code
 * question_generation_instructions_by_property} map from note or readme frontmatter.
 */
public final class FrontmatterQuestionGenerationInstruction {

  private static final List<String> KEYS =
      List.of("question_generation_instruction", "questionGenerationInstruction");

  private static final String BY_PROPERTY_KEY = "question_generation_instructions_by_property";

  private FrontmatterQuestionGenerationInstruction() {}

  public static Optional<String> fromNoteContent(String content) {
    return leadingFrontmatter(content)
        .flatMap(FrontmatterQuestionGenerationInstruction::fromFrontmatter);
  }

  /**
   * The {@code question_generation_instructions_by_property} entry whose property type matches
   * {@code propertyType} ignoring case.
   */
  public static Optional<String> forPropertyType(String content, String propertyType) {
    return leadingFrontmatter(content)
        .flatMap(frontmatter ->
            frontmatter.getStringMap(BY_PROPERTY_KEY).entrySet().stream()
                .filter(entry ->
                    PropertyKeyNaming.propertyType(entry.getKey()).equalsIgnoreCase(propertyType))
                .map(entry -> entry.getValue().trim())
                .findFirst());
  }

  private static Optional<Frontmatter> leadingFrontmatter(String content) {
    if (content == null || content.isBlank()) {
      return Optional.empty();
    }
    return NoteContentMarkdown.splitLeadingFrontmatter(content)
        .map(NoteContentMarkdown.LeadingFrontmatter::frontmatter);
  }

  private static Optional<String> fromFrontmatter(Frontmatter frontmatter) {
    for (String key : KEYS) {
      Optional<String> value =
          frontmatter.getString(key).map(String::trim).filter(s -> !s.isEmpty());
      if (value.isPresent()) {
        return value;
      }
    }
    return Optional.empty();
  }
}
