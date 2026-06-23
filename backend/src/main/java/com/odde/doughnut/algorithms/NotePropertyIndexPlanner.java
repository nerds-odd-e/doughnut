package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.List;

/** Plans {@code note_property_index} rows from parsed frontmatter. */
public final class NotePropertyIndexPlanner {

  public record PlannedRow(
      String propertyKey, int itemIndex, String valueText, boolean listProperty) {}

  private NotePropertyIndexPlanner() {}

  public static List<PlannedRow> plannedRows(Frontmatter frontmatter) {
    List<PlannedRow> rows = new ArrayList<>();
    for (String key : frontmatter.keys()) {
      if (PropertyKeyNaming.isExcludedFromPropertyIndexing(key)) {
        continue;
      }
      frontmatter.getPropertyValue(key).ifPresent(pv -> appendRows(rows, key, pv));
    }
    return List.copyOf(rows);
  }

  private static void appendRows(
      List<PlannedRow> rows, String key, FrontmatterPropertyValue propertyValue) {
    switch (propertyValue) {
      case FrontmatterPropertyValue.Scalar scalar ->
          rows.add(new PlannedRow(key, 0, scalar.value(), false));
      case FrontmatterPropertyValue.ListItems listItems -> {
        if (listItems.items().isEmpty()) {
          return;
        }
        List<PlannedRow> linkRows = new ArrayList<>();
        for (int i = 0; i < listItems.items().size(); i++) {
          String item = listItems.items().get(i);
          if (!WikiLinkMarkdown.innerTitlesInOccurrenceOrder(item).isEmpty()) {
            linkRows.add(new PlannedRow(key, i, item, true));
          }
        }
        if (linkRows.isEmpty()) {
          rows.add(new PlannedRow(key, 0, "", true));
        } else {
          rows.addAll(linkRows);
        }
      }
    }
  }
}
