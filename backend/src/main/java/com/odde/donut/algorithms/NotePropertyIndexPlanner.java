package com.odde.donut.algorithms;

import java.util.ArrayList;
import java.util.List;

/** Plans {@code note_property_index} rows from parsed frontmatter. */
public final class NotePropertyIndexPlanner {

  public record PlannedRow(
      String propertyKey,
      int itemIndex,
      String valueText,
      boolean listProperty,
      String sourceLocalKey) {}

  private NotePropertyIndexPlanner() {}

  public static List<PlannedRow> plannedRows(Frontmatter frontmatter) {
    return plannedRows(frontmatter, CanonicalDonutOrigin.production());
  }

  public static List<PlannedRow> plannedRows(
      Frontmatter frontmatter, CanonicalDonutOrigin canonicalOrigin) {
    List<PlannedRow> rows = new ArrayList<>();
    for (String key : frontmatter.keys()) {
      if (PropertyKeyNaming.isExcludedFromPropertyIndexing(key)) {
        continue;
      }
      frontmatter.getPropertyValue(key).ifPresent(pv -> appendRows(rows, key, pv, canonicalOrigin));
    }
    return List.copyOf(rows);
  }

  private static void appendRows(
      List<PlannedRow> rows,
      String key,
      FrontmatterPropertyValue propertyValue,
      CanonicalDonutOrigin canonicalOrigin) {
    switch (propertyValue) {
      case FrontmatterPropertyValue.Scalar scalar ->
          rows.add(
              new PlannedRow(
                  key,
                  0,
                  scalar.value(),
                  false,
                  sourceLocalKeyFor(scalar.value(), canonicalOrigin)));
      case FrontmatterPropertyValue.ListItems listItems -> {
        if (listItems.items().isEmpty()) {
          return;
        }
        List<PlannedRow> referenceRows = new ArrayList<>();
        for (int i = 0; i < listItems.items().size(); i++) {
          String item = listItems.items().get(i);
          String sourceLocalKey = sourceLocalKeyFor(item, canonicalOrigin);
          if (sourceLocalKey != null) {
            referenceRows.add(new PlannedRow(key, i, item, true, sourceLocalKey));
          }
        }
        if (referenceRows.isEmpty()) {
          rows.add(new PlannedRow(key, 0, "", true, null));
        } else {
          rows.addAll(referenceRows);
        }
      }
    }
  }

  private static String sourceLocalKeyFor(String valueText, CanonicalDonutOrigin canonicalOrigin) {
    return AuthoredNoteReferences.fromMarkdownFragment(valueText, canonicalOrigin).stream()
        .findFirst()
        .map(AuthoredNoteReference::sourceLocalKey)
        .orElse(null);
  }
}
