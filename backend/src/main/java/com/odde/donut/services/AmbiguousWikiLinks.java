package com.odde.donut.services;

import com.odde.donut.algorithms.NoteContentMarkdown;
import com.odde.donut.algorithms.WikiLinkMarkdown;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class AmbiguousWikiLinks {

  private final WikiLinkResolver wikiLinkResolver;

  AmbiguousWikiLinks(WikiLinkResolver wikiLinkResolver) {
    this.wikiLinkResolver = wikiLinkResolver;
  }

  List<WikiLink> forAuthoredTokensNotIn(Note focusNote, User viewer, Set<String> skipAuthored) {
    String content = focusNote.getContent();
    if (content == null || content.isBlank()) {
      return List.of();
    }
    List<WikiLink> out = new ArrayList<>();
    List<String> tokens = NoteContentMarkdown.authoredTokensInOccurrenceOrder(content);
    for (String token : WikiLinkMarkdown.uniqueAuthoredTokensPreserveOrder(tokens)) {
      if (skipAuthored.contains(token)) {
        continue;
      }
      if (!wikiLinkResolver.isAmbiguousToken(token, focusNote, viewer)) {
        continue;
      }
      WikiLinkMarkdown.WikiInnerSplit parts = WikiLinkMarkdown.splitInner(token);
      out.add(
          new WikiLink(
              token,
              parts.portablePath().format(),
              parts.displayText(),
              WikiLink.Resolution.AMBIGUOUS,
              null));
    }
    return List.copyOf(out);
  }
}
