package com.odde.donut.services;

import java.util.function.Predicate;

final class NumberedNameSelection {

  private NumberedNameSelection() {}

  static String firstAvailable(String requestedName, int maxLength, Predicate<String> isOccupied) {
    if (!isOccupied.test(requestedName)) {
      return requestedName;
    }
    for (int suffixNumber = 2; ; suffixNumber++) {
      String suffix = " (" + suffixNumber + ")";
      String candidate =
          requestedName.substring(0, Math.min(requestedName.length(), maxLength - suffix.length()))
              + suffix;
      if (!isOccupied.test(candidate)) {
        return candidate;
      }
    }
  }
}
