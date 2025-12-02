package com.odde.doughnut.services.graphRAG;

public class GraphRAGConstants {
  public static final int RELATED_NOTE_DETAILS_TRUNCATE_LENGTH = 150;
  public static final double CHARACTERS_PER_TOKEN = 3.75;
  public static final int MAX_DEPTH = 3;
  public static final int MAX_TOTAL_CANDIDATES = 200;
  public static final int CHILD_CAP_MULTIPLIER = 2;
  public static final int INBOUND_CAP_MULTIPLIER = 2;
  public static final double TOKEN_BUDGET_SAFETY_MARGIN = 1.2;
}
