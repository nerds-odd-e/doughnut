package com.odde.doughnut.algorithms;

import com.odde.doughnut.validators.DisplayNamePathSeparators;
import java.util.ArrayList;
import java.util.List;

/** Pure planner for title-alias to frontmatter migration previews and batch transforms. */
public final class TitleAliasMigrationTransform {

  private TitleAliasMigrationTransform() {}

  public record Preview(
      String plannedTitle,
      List<String> plannedAliases,
      String plannedContent,
      TitleAliasMigrationPreviewStatus status) {}

  /**
   * Planned title for a note, derived from the title alone. Collision detection only needs this, so
   * the migration can scan the whole corpus without loading note content.
   */
  public static String plannedTitleFor(String title) {
    String safeTitle = title == null ? "" : title;
    TitleAliasMigrationPlan.Result plan = TitleAliasMigrationPlan.from(safeTitle);
    return plan.hasMigratablePlainAliases() ? buildMigratedTitle(plan) : safeTitle;
  }

  /** Migration status for a note, derived from the title alone (content is irrelevant here). */
  public static TitleAliasMigrationPreviewStatus statusFor(String title) {
    String safeTitle = title == null ? "" : title;
    return TitleAliasMigrationPlan.from(safeTitle).hasMigratablePlainAliases()
        ? TitleAliasMigrationPreviewStatus.MIGRATE
        : TitleAliasMigrationPreviewStatus.NO_CHANGES;
  }

  public static Preview preview(String title, String content) {
    String safeTitle = title == null ? "" : title;
    String safeContent = content == null ? "" : content;
    TitleAliasMigrationPlan.Result plan = TitleAliasMigrationPlan.from(safeTitle);
    if (!plan.hasMigratablePlainAliases()) {
      return new Preview(
          safeTitle,
          FrontmatterAliases.fromNoteContent(safeContent),
          safeContent,
          TitleAliasMigrationPreviewStatus.NO_CHANGES);
    }
    String plannedTitle = buildMigratedTitle(plan);
    List<String> plannedAliases = mergedAliases(safeContent, plan.plainAliases());
    String plannedContent = contentWithAliases(safeContent, plannedAliases);
    return new Preview(
        plannedTitle, plannedAliases, plannedContent, TitleAliasMigrationPreviewStatus.MIGRATE);
  }

  static String buildMigratedTitle(TitleAliasMigrationPlan.Result plan) {
    StringBuilder title = new StringBuilder(formatFragment(plan.primary()));
    for (String suffixStem : plan.retainedSuffixFragments()) {
      title.append('／').append('~').append(suffixStem);
    }
    plan.qualifier().ifPresent(q -> title.append(" (").append(q).append(')'));
    return title.toString();
  }

  private static String formatFragment(TitleFragment fragment) {
    return fragment.suffixMarker() ? "~" + fragment.stem() : fragment.stem();
  }

  static List<String> mergedAliases(String content, List<String> titlePlainAliases) {
    List<String> merged = new ArrayList<>(FrontmatterAliases.fromNoteContent(content));
    for (String alias : titlePlainAliases) {
      String trimmed = DisplayNamePathSeparators.trimSurroundingWhitespace(alias);
      if (trimmed.isBlank()) {
        continue;
      }
      String newKey = FrontmatterAliases.normalizedLookupKey(trimmed);
      boolean alreadyPresent =
          merged.stream()
              .anyMatch(
                  existing -> FrontmatterAliases.normalizedLookupKey(existing).equals(newKey));
      if (!alreadyPresent) {
        merged.add(trimmed);
      }
    }
    return List.copyOf(merged);
  }

  private static String contentWithAliases(String content, List<String> aliases) {
    return NoteContentMarkdown.splitLeadingFrontmatter(content)
        .map(
            lf -> {
              Frontmatter updated = lf.frontmatter().setAliasesList(aliases);
              return updated.isEmpty() ? lf.body() : updated.fenced(lf.body());
            })
        .orElseGet(() -> Frontmatter.empty().setAliasesList(aliases).fenced(content));
  }
}
