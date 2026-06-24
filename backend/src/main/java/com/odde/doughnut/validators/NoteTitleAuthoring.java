package com.odde.doughnut.validators;

import com.odde.doughnut.algorithms.TitleAliasMigrationPlan;

public final class NoteTitleAuthoring {

  public static final String PLAIN_TITLE_ALIAS_MESSAGE =
      "Title must not use ／ for alternative spellings; add aliases in note frontmatter instead.";

  private NoteTitleAuthoring() {}

  public static boolean hasPlainTitleAliasSegments(String title) {
    if (title == null) {
      return false;
    }
    return TitleAliasMigrationPlan.from(title).hasMigratablePlainAliases();
  }
}
