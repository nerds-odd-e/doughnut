package com.odde.doughnut.services.focusContext;

public class FocusContextConstants {
  public static final int FOCUS_NOTE_DETAILS_MAX_TOKENS = 2000;
  public static final int RELATED_NOTE_DETAILS_MAX_TOKENS = 500;
  public static final int RELATED_NOTES_TOTAL_BUDGET_TOKENS = 2500;
  public static final int MAX_FOLDER_SIBLINGS_PER_NOTE = 5;

  /**
   * Share of {@link #RELATED_NOTES_TOTAL_BUDGET_TOKENS} for wiki BFS (remainder is for folder
   * siblings).
   */
  public static final double RELATED_NOTES_WIKI_BUDGET_FRACTION = 0.75;
}
