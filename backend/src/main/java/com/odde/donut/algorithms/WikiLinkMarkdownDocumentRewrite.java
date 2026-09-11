package com.odde.donut.algorithms;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

/**
 * Rewrites wiki Portable-path tokens inside a Markdown document. Ordinary Markdown links, including
 * note-ID URLs, are left unchanged.
 */
public final class WikiLinkMarkdownDocumentRewrite {

  private WikiLinkMarkdownDocumentRewrite() {}

  /** Converts OS-invalid characters in wiki tokens. */
  public static String replaceOsInvalidCharsInAuthoredTokens(String markdown) {
    if (markdown == null || markdown.isEmpty()) {
      return markdown;
    }
    String content = markdown;
    for (String token :
        new LinkedHashSet<>(WikiLinkMarkdown.authoredTokensInOccurrenceOrder(markdown))) {
      String converted = WikiLinkMarkdownRewrite.replaceOsInvalidCharsInStoredLinkInner(token);
      if (!converted.equals(token)) {
        content = replaceWikiLinksMatchingTrimmedInner(content, token, converted);
      }
    }
    return content;
  }

  public static String replaceWikiLinksMatchingTrimmedInner(
      String markdown, String oldInnerTrimmed, String newInner) {
    if (markdown == null || markdown.isEmpty()) {
      return markdown;
    }
    Optional<NoteLeadingFrontmatter.VerbatimSplit> frontmatter =
        NoteLeadingFrontmatter.splitVerbatim(markdown);
    if (frontmatter.isEmpty()) {
      return replaceInMarkdown(markdown, oldInnerTrimmed, newInner);
    }
    NoteLeadingFrontmatter.VerbatimSplit split = frontmatter.get();
    String yaml = rewriteSupportedFrontmatterValues(split.yamlRaw(), oldInnerTrimmed, newInner);
    String body = replaceInMarkdown(split.body(), oldInnerTrimmed, newInner);
    if (yaml.equals(split.yamlRaw()) && body.equals(split.body())) {
      return markdown;
    }
    return split.rebuild(yaml, body);
  }

  private static String replaceInMarkdown(
      String markdown, String oldInnerTrimmed, String newInner) {
    Matcher matcher = WikiLinkMarkdown.INNER_LINK_PATTERN.matcher(markdown);
    StringBuilder out = new StringBuilder();
    int last = 0;
    while (matcher.find()) {
      out.append(markdown, last, matcher.start());
      String innerTrimmed = matcher.group(1).trim();
      if (innerTrimmed.equals(oldInnerTrimmed)) {
        out.append("[[").append(newInner).append("]]");
      } else {
        out.append(matcher.group(0));
      }
      last = matcher.end();
    }
    out.append(markdown.substring(last));
    return out.toString();
  }

  private static String rewriteSupportedFrontmatterValues(
      String yamlRaw, String oldInnerTrimmed, String newInner) {
    Node document = new Yaml().compose(new StringReader(yamlRaw));
    if (!(document instanceof MappingNode mapping)) {
      return yamlRaw;
    }
    List<ScalarNode> supportedValues = new ArrayList<>();
    mapping
        .getValue()
        .forEach(
            tuple -> {
              Node value = tuple.getValueNode();
              if (value instanceof ScalarNode scalar) {
                supportedValues.add(scalar);
              } else if (value instanceof SequenceNode sequence
                  && sequence.getValue().stream().allMatch(ScalarNode.class::isInstance)) {
                sequence.getValue().forEach(item -> supportedValues.add((ScalarNode) item));
              }
            });

    List<Replacement> replacements = new ArrayList<>();
    for (ScalarNode scalar : supportedValues) {
      String transformed = replaceInMarkdown(scalar.getValue(), oldInnerTrimmed, newInner);
      if (transformed.equals(scalar.getValue())) {
        continue;
      }
      int start = yamlRaw.offsetByCodePoints(0, scalar.getStartMark().getIndex());
      int end = yamlRaw.offsetByCodePoints(0, scalar.getEndMark().getIndex());
      String source = yamlRaw.substring(start, end);
      String rewrittenSource = replaceInMarkdown(source, oldInnerTrimmed, newInner);
      if (scalar.getScalarStyle() == DumperOptions.ScalarStyle.DOUBLE_QUOTED
          || rewrittenSource.equals(source)) {
        rewrittenSource = dumpScalar(transformed, scalar.getScalarStyle());
      }
      replacements.add(new Replacement(start, end, rewrittenSource));
    }
    replacements.sort(Comparator.comparingInt(Replacement::start).reversed());
    StringBuilder rewritten = new StringBuilder(yamlRaw);
    replacements.forEach(r -> rewritten.replace(r.start(), r.end(), r.text()));
    return rewritten.toString();
  }

  private static String dumpScalar(String value, DumperOptions.ScalarStyle style) {
    DumperOptions options = new DumperOptions();
    options.setDefaultScalarStyle(style);
    options.setSplitLines(false);
    String dumped = new Yaml(options).dump(value);
    return dumped.endsWith("\n") ? dumped.substring(0, dumped.length() - 1) : dumped;
  }

  private record Replacement(int start, int end, String text) {}
}
