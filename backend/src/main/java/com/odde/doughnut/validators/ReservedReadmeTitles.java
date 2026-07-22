package com.odde.doughnut.validators;

/**
 * Note titles reserved for notebook/folder readme content (not ordinary notes). Matches {@code
 * readme} and {@code readme.md} case-insensitively.
 */
public final class ReservedReadmeTitles {

  public static final String RESERVED_MESSAGE =
      "'readme' and 'readme.md' are reserved for notebook and folder readme content.";

  private ReservedReadmeTitles() {}

  public static boolean isReserved(String title) {
    if (title == null) {
      return false;
    }
    String trimmed = title.trim();
    return trimmed.equalsIgnoreCase("readme") || trimmed.equalsIgnoreCase("readme.md");
  }
}
