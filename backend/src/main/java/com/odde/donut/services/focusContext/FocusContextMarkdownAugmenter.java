package com.odde.donut.services.focusContext;

import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.controllers.dto.WikiTitle;
import com.odde.donut.entities.Note;
import java.util.ArrayList;
import java.util.List;

public final class FocusContextMarkdownAugmenter {
  public static final String PROPERTY_FOCUS_CONTEXT_HEADER =
      "Focus on one property of the focus note:";

  private FocusContextMarkdownAugmenter() {}

  public static String buildPropertyFocusBlock(Note focus, String propertyKey) {
    String propertyValue =
        NoteContentMarkdown.splitLeadingFrontmatter(focus.getContent())
            .flatMap(split -> split.frontmatter().getString(propertyKey))
            .orElse("");
    StringBuilder block = new StringBuilder();
    block.append(PROPERTY_FOCUS_CONTEXT_HEADER).append("\n");
    block
        .append("Focus on property \"")
        .append(propertyKey)
        .append("\" of the focus note (not the whole note).\n");
    block.append(
        "Infer what this property means from the property name, the focus note content, and"
            + " the listed link targets in the focus context.\n");
    block.append("Property key: ").append(propertyKey).append("\n");
    block.append("Property value: ").append(propertyValue).append("\n");
    return block.toString();
  }

  public static String embedPropertyFocus(String focusContextMarkdown, String propertyFocusBlock) {
    int focusNoteSection =
        focusContextMarkdown.indexOf(FocusContextConstants.FOCUS_NOTE_SECTION_START);
    if (focusNoteSection >= 0) {
      return focusContextMarkdown.substring(0, focusNoteSection)
          + "\n"
          + propertyFocusBlock
          + focusContextMarkdown.substring(focusNoteSection);
    }
    return focusContextMarkdown + "\n\n" + propertyFocusBlock;
  }

  public static String ensureWikiTitles(String focusContextMarkdown, List<WikiTitle> wikiTitles) {
    if (wikiTitles.isEmpty()) {
      return focusContextMarkdown;
    }
    List<WikiTitle> missing = new ArrayList<>();
    for (WikiTitle wikiTitle : wikiTitles) {
      String linkText = wikiTitle.getLinkText();
      if (linkText != null && !linkText.isBlank() && !focusContextMarkdown.contains(linkText)) {
        missing.add(wikiTitle);
      }
    }
    if (missing.isEmpty()) {
      return focusContextMarkdown;
    }
    StringBuilder extended = new StringBuilder(focusContextMarkdown);
    extended.append("\n\n## Link targets (focus note)\n\n");
    for (WikiTitle wikiTitle : missing) {
      extended.append("- ").append(wikiTitle.getLinkText());
      if (wikiTitle.getNoteId() != null) {
        extended.append(" (resolved note id: ").append(wikiTitle.getNoteId()).append(")");
      }
      extended.append("\n");
    }
    return extended.toString();
  }
}
