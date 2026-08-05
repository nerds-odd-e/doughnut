package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import java.util.ArrayList;
import java.util.List;

/** Pending frontmatter {@code aliases} / {@code overlaps} lists for {@link NoteBuilder}. */
final class NoteFrontmatterLists {
  private final List<String> plainAliases = new ArrayList<>();
  private final List<String> overlapWikiLinkInners = new ArrayList<>();
  private final List<String> legacyAliasOverlapWikiLinkInners = new ArrayList<>();
  private boolean refreshAliasIndex;

  void addPlainAliases(String... aliases) {
    plainAliases.addAll(List.of(aliases));
    refreshAliasIndex = true;
  }

  void addOverlapPartner(Note partner) {
    Notebook notebook = partner.getNotebook();
    overlapWikiLinkInners.add(notebook.getName() + ":" + partner.getTitle());
  }

  void addOverlapWikiLink(String wikiLinkInner) {
    overlapWikiLinkInners.add(wikiLinkInner);
  }

  void addLegacyOverlapPartner(Note partner) {
    Notebook notebook = partner.getNotebook();
    legacyAliasOverlapWikiLinkInners.add(notebook.getName() + ":" + partner.getTitle());
  }

  void addLegacyOverlapWikiLink(String wikiLinkInner) {
    legacyAliasOverlapWikiLinkInners.add(wikiLinkInner);
    refreshAliasIndex = true;
  }

  boolean shouldRefreshAliasIndex() {
    return refreshAliasIndex;
  }

  /** Markdown with leading frontmatter lists, or empty when nothing pending. */
  String composedContentOrEmpty() {
    if (plainAliases.isEmpty()
        && overlapWikiLinkInners.isEmpty()
        && legacyAliasOverlapWikiLinkInners.isEmpty()) {
      return "";
    }
    StringBuilder yaml = new StringBuilder("---\n");
    if (!plainAliases.isEmpty() || !legacyAliasOverlapWikiLinkInners.isEmpty()) {
      yaml.append("aliases:\n");
      for (String alias : plainAliases) {
        yaml.append("  - ").append(alias).append('\n');
      }
      for (String inner : legacyAliasOverlapWikiLinkInners) {
        yaml.append("  - \"[[").append(inner).append("]]\"\n");
      }
    }
    if (!overlapWikiLinkInners.isEmpty()) {
      yaml.append("overlaps:\n");
      for (String inner : overlapWikiLinkInners) {
        yaml.append("  - \"[[").append(inner).append("]]\"\n");
      }
    }
    yaml.append("---\n\nBody text");
    return yaml.toString();
  }
}
