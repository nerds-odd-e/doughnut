package com.odde.doughnut.algorithms;

import java.util.List;
import java.util.Optional;

/** Reads {@code question_generation_instruction} from note or readme frontmatter. */
public final class FrontmatterQuestionGenerationInstruction {

  private static final List<String> KEYS =
      List.of("question_generation_instruction", "questionGenerationInstruction");

  private FrontmatterQuestionGenerationInstruction() {}

  public static Optional<String> fromNoteContent(String content) {
    if (content == null || content.isBlank()) {
      return Optional.empty();
    }
    return NoteContentMarkdown.splitLeadingFrontmatter(content)
        .map(NoteContentMarkdown.LeadingFrontmatter::frontmatter)
        .flatMap(FrontmatterQuestionGenerationInstruction::fromFrontmatter);
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
