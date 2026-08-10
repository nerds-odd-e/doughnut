package com.odde.doughnut.entities;

import com.odde.doughnut.validators.DisplayNamePathSeparators;

public record DisplayName(String value) {
  public DisplayName {
    value = DisplayNamePathSeparators.trimSurroundingWhitespace(value == null ? "" : value);
  }
}
