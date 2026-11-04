package com.odde.donut.algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Plans {@code note_property_index} rows from parsed frontmatter. */
public final class NotePropertyIndexPlanner {

  /**
   * One index row: a scalar has one row with an empty {@code propertyValue}; a list has one row per
   * distinct item with the item as its value. {@code content} is the parsed text the row stands
   * for: the scalar's value, or the item itself.
   */
  public record PlannedRow(
      String propertyKey,
      int itemIndex,
      String propertyValue,
      String content,
      String sourceLocalKey) {
    public ValueKey valueKey() {
      return new ValueKey(propertyKey, propertyValue);
    }
  }

  /**
   * A property value's identity, which its change date belongs to: a scalar by its key (its {@code
   * propertyValue} is empty), a list item by its key and text regardless of position.
   */
  public record ValueKey(String propertyKey, String propertyValue) {}

  /** A value with the content that tells whether it was edited. */
  private record DatedValue(ValueKey valueKey, String content) {}

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

  /** Values in {@code after} that are absent from, or differ in content from, {@code before}. */
  public static Set<ValueKey> changedValues(Frontmatter before, Frontmatter after) {
    Set<DatedValue> beforeValues = datedValues(before);
    return datedValues(after).stream()
        .filter(value -> !beforeValues.contains(value))
        .map(DatedValue::valueKey)
        .collect(Collectors.toSet());
  }

  private static Set<DatedValue> datedValues(Frontmatter frontmatter) {
    return plannedRows(frontmatter).stream()
        .map(row -> new DatedValue(row.valueKey(), row.content()))
        .collect(Collectors.toSet());
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
                  key, 0, "", scalar.value(), sourceLocalKeyFor(scalar.value(), canonicalOrigin)));
      case FrontmatterPropertyValue.ListItems listItems -> {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < listItems.items().size(); i++) {
          String item = listItems.items().get(i);
          if (item.isBlank() || item.length() > MAX_PROPERTY_VALUE_LENGTH || !seen.add(item)) {
            continue;
          }
          rows.add(new PlannedRow(key, i, item, item, sourceLocalKeyFor(item, canonicalOrigin)));
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
