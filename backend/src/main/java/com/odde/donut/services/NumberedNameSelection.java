package com.odde.donut.services;

import java.util.function.Predicate;

public final class NumberedNameSelection {

  private NumberedNameSelection() {}

  static String firstAvailable(String requestedName, int maxLength, Predicate<String> isOccupied) {
    return firstAvailable(requestedName, "", maxLength, isOccupied);
  }

  /** A filename is numbered before its extension: {@code example (2).png}. */
  public static String firstAvailableFilename(String filename, Predicate<String> isOccupied) {
    int dot = filename.lastIndexOf('.');
    int stemEnd = dot > 0 ? dot : filename.length();
    return firstAvailable(
        filename.substring(0, stemEnd), filename.substring(stemEnd), Integer.MAX_VALUE, isOccupied);
  }

  private static String firstAvailable(
      String stem, String extension, int maxLength, Predicate<String> isOccupied) {
    if (!isOccupied.test(stem + extension)) {
      return stem + extension;
    }
    for (int suffixNumber = 2; ; suffixNumber++) {
      String suffix = " (" + suffixNumber + ")";
      String candidate =
          stem.substring(
                  0, Math.min(stem.length(), maxLength - suffix.length() - extension.length()))
              + suffix
              + extension;
      if (!isOccupied.test(candidate)) {
        return candidate;
      }
    }
  }
}
