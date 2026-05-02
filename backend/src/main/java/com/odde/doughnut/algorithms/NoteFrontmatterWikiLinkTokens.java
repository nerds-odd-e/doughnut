package com.odde.doughnut.algorithms;

import java.text.Normalizer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** NFKC-normalized wikilink inner titles from one YAML scalar in leading frontmatter. */
public final class NoteFrontmatterWikiLinkTokens {

  private NoteFrontmatterWikiLinkTokens() {}

  /**
   * NFKC-normalized inner wikilink titles from the first {@code fieldKey:} scalar in leading YAML
   * frontmatter (e.g. {@code source} or {@code parent}).
   */
  public static Set<String> normalizedWikiLinkTokensFromYamlField(String details, String fieldKey) {
    return NoteDetailsMarkdown.splitLeadingFrontmatter(details)
        .flatMap(fm -> NoteYamlFrontmatterScalars.firstScalarValue(fm.yamlRaw(), fieldKey))
        .map(NoteFrontmatterWikiLinkTokens::normalizedWikiTitlesFromScalar)
        .orElse(Set.of());
  }

  private static Set<String> normalizedWikiTitlesFromScalar(String scalar) {
    List<String> inners = WikiLinkMarkdown.innerTitlesInOccurrenceOrder(scalar);
    if (inners.isEmpty()) {
      return Set.of();
    }
    Set<String> out = new HashSet<>();
    for (String inner : inners) {
      out.add(Normalizer.normalize(inner, Normalizer.Form.NFKC));
    }
    return Collections.unmodifiableSet(out);
  }
}
