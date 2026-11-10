package com.odde.donut.services.focusContext;

import com.odde.donut.algorithms.FrontmatterPropertyValue;
import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.entities.Note;
import java.util.Optional;

/** Validated perspective of a current note; occurrence describes rather than identifies history. */
public record ResolvedPropertyFocus(
    String key, FrontmatterPropertyValue property, String itemValue, Integer occurrence) {
  public static Optional<ResolvedPropertyFocus> resolve(
      Note note, String key, String itemValue, Integer occurrence) {
    return NoteContentMarkdown.splitLeadingFrontmatter(note.getContent())
        .flatMap(split -> split.frontmatter().getPropertyValueExact(key))
        .filter(value -> {
          if (itemValue == null) return occurrence == null;
          if (!(value instanceof FrontmatterPropertyValue.ListItems list)) return false;
          int selectedOccurrence = occurrence == null ? 1 : occurrence;
          return selectedOccurrence > 0
              && list.items().stream().filter(itemValue::equals).count() >= selectedOccurrence;
        })
        .map(value ->
            new ResolvedPropertyFocus(
                key,
                value,
                itemValue,
                itemValue == null ? null : occurrence == null ? 1 : occurrence));
  }
}
