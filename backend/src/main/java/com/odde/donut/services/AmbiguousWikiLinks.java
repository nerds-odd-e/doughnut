package com.odde.donut.services;

import com.odde.donut.algorithms.AuthoredNoteReference;
import com.odde.donut.algorithms.AuthoredNoteReferences;
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
    for (AuthoredNoteReference.WikiPortablePathTarget wiki :
        AuthoredNoteReferences.uniqueWikiPortablePathTargets(content)) {
      String token = wiki.authoredLink();
      if (skipAuthored.contains(token)) {
        continue;
      }
      if (!wikiLinkResolver.isAmbiguousToken(token, focusNote, viewer)) {
        continue;
      }
      out.add(WikiLinks.ambiguous(wiki));
    }
    return List.copyOf(out);
  }
}
