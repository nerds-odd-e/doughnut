package com.odde.doughnut.algorithms;

import java.util.List;

/** Supported frontmatter property value: scalar string or one-level list of strings. */
public sealed interface FrontmatterPropertyValue
    permits FrontmatterPropertyValue.Scalar, FrontmatterPropertyValue.ListItems {

  record Scalar(String value) implements FrontmatterPropertyValue {}

  record ListItems(List<String> items) implements FrontmatterPropertyValue {}
}
