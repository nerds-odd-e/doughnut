package com.odde.donut.algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Plans {@code note_property_index} rows from parsed frontmatter. */
public final class NotePropertyIndexPlanner {

  /**
   * One index row: a scalar has one row with an empty {@code propertyValue}; a list has one row per
   * distinct item with the item as its value.
   */
  public record PlannedRow(
      String propertyKey, int itemIndex, String propertyValue, String sourceLocalKey) {}

  private static final int MAX_PROPERTY_VALUE_LENGTH = 255;

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
          rows.add(new PlannedRow(key, 0, "", sourceLocalKeyFor(scalar.value(), canonicalOrigin)));
      case FrontmatterPropertyValue.ListItems listItems -> {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < listItems.items().size(); i++) {
          String item = listItems.items().get(i);
          if (item.isBlank() || item.length() > MAX_PROPERTY_VALUE_LENGTH || !seen.add(item)) {
            continue;
          }
          rows.add(new PlannedRow(key, i, item, sourceLocalKeyFor(item, canonicalOrigin)));
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
