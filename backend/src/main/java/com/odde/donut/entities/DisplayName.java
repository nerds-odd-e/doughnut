package com.odde.donut.entities;

import com.odde.donut.validators.DisplayNamePathSeparators;

public record DisplayName(String value) {
  public DisplayName {
    value = DisplayNamePathSeparators.trimSurroundingWhitespace(value == null ? "" : value);
  }
}
