package com.odde.doughnut.services;

import java.nio.charset.StandardCharsets;

/** UTF-8 byte length divided by ~3.75 bytes per token (embedding and note-detail budgeting). */
public final class ApproximateUtf8TokenBudget {
  private static final double BYTES_PER_TOKEN = 3.75d;

  private ApproximateUtf8TokenBudget() {}

  /**
   * Truncate text so its approximate token count (using ~3.75 UTF-8 bytes per token) does not
   * exceed maxTokens.
   */
  public static String truncateByApproxTokens(String text, int maxTokens) {
    if (text == null || text.isEmpty()) return text;
    int maxBytes = (int) Math.floor(maxTokens * BYTES_PER_TOKEN);
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    if (bytes.length <= maxBytes) return text;

    int low = 0;
    int high = text.length();
    while (low < high) {
      int mid = (low + high + 1) / 2;
      int len = text.substring(0, mid).getBytes(StandardCharsets.UTF_8).length;
      if (len <= maxBytes) {
        low = mid;
      } else {
        high = mid - 1;
      }
    }
    return text.substring(0, low);
  }

  /** Approximate token count for budgeting (UTF-8 byte length / ~3.75 per token). */
  public static int estimateApproxTokens(String text) {
    if (text == null || text.isEmpty()) {
      return 0;
    }
    return (int) Math.ceil(text.getBytes(StandardCharsets.UTF_8).length / BYTES_PER_TOKEN);
  }
}
