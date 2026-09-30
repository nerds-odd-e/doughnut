package com.odde.donut.entities;

/** A property of the focus note to ask about; {@code value} is a list item, or '' for a scalar. */
public record PropertyFocus(String key, String value) {
  /** The list item this focus names, or null for a scalar. */
  public String listItemOrNull() {
    return value.isEmpty() ? null : value;
  }
}
