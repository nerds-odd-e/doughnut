package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Notebook;
import lombok.Getter;
import lombok.Setter;

/**
 * Spelling recall question. Stem is markdown with {@code <mark>} cloze masks; frontend converts to
 * HTML.
 */
public class SpellingQuestion {
  @Getter @Setter private String stem;
  @Getter @Setter private Notebook notebook;

  public SpellingQuestion(String stem, Notebook notebook) {
    this.stem = stem;
    this.notebook = notebook;
  }
}
