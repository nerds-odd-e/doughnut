package com.odde.donut.algorithms;

import java.util.List;

/** Supported frontmatter property value: scalar string or one-level list of strings. */
public sealed interface FrontmatterPropertyValue
    permits FrontmatterPropertyValue.Scalar, FrontmatterPropertyValue.ListItems {

  /** The value's strings: a scalar as one string, a list as its items. */
  List<String> strings();

  record Scalar(String value) implements FrontmatterPropertyValue {
    @Override
    public List<String> strings() {
      return List.of(value);
    }
  }

  record ListItems(List<String> items) implements FrontmatterPropertyValue {
    @Override
    public List<String> strings() {
      return items;
    }
  }
}
