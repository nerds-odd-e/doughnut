package com.odde.doughnut.services.focusContext;

public class FocusContextConstants {
  public static final int FOCUS_NOTE_DETAILS_MAX_TOKENS = 1500;
  public static final int RELATED_NOTE_DETAILS_MAX_TOKENS = 200;

  /**
   * Approximate UTF-8 token budget for note details (body text) across the focus note and all
   * related notes combined. Focus content is capped first (see {@link
   * #FOCUS_NOTE_DETAILS_MAX_TOKENS}); the remainder funds wiki expansion and folder siblings.
   */
  public static final int FOCUS_CONTEXT_COMBINED_CONTENT_TOKEN_BUDGET = 2000;

  /**
   * Base cap at graph depth 1 for inbound referrers and folder-peer sampling (per parent/anchor).
   */
  public static final int INBOUND_TOP_DEPTH_CAP = 6;

  /** Maximum URIs shown in the focus note's flat `inboundReferences` list. */
  public static final int FOCUS_INBOUND_URI_CAP = 20;

  /**
   * Max sampled items at graph depth {@code depth} (1 = from focus, 2 = from depth-1 winners, …)
   * for inbound wiki references and folder siblings: {@code floor(6 / 3^(depth-1))}.
   */
  public static int sampleCapAtGraphDepth(int depth) {
    int cap = INBOUND_TOP_DEPTH_CAP;
    for (int d = 1; d < depth; d++) {
      cap = cap / 3;
    }
    return cap;
  }

  /**
   * Below this many approximate tokens left <em>after</em> focus note details (e.g. tight {@code
   * GET /notes/{id}/graph} limits), omit folder-peer sampling: no {@code sampleSiblings} on the
   * focus note and no {@link
   * com.odde.doughnut.services.focusContext.FocusContextEdgeType#FolderSibling} rows. Wiki BFS
   * still uses the full post-focus remainder.
   */
  public static final int MIN_RELATED_TOKENS_FOR_FOLDER_PEER_CONTEXT = 100;

  /**
   * Share of the post-focus token remainder for wiki BFS (the rest is for folder siblings), after
   * subtracting focus note details cost from the combined content budget.
   */
  public static final double RELATED_NOTES_WIKI_BUDGET_FRACTION = 0.75;
}
