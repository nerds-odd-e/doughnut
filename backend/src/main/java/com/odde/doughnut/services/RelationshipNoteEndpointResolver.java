package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.NoteDetailsMarkdown;
import com.odde.doughnut.algorithms.NoteYamlFrontmatterScalars;
import com.odde.doughnut.algorithms.WikiLinkMarkdown;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RelationshipNoteEndpointResolver {

  private final WikiLinkResolver wikiLinkResolver;

  public RelationshipNoteEndpointResolver(WikiLinkResolver wikiLinkResolver) {
    this.wikiLinkResolver = wikiLinkResolver;
  }

  public Optional<Note> resolveSemanticTarget(Note relation, User viewer) {
    String details = relation.getDetails();
    if (details == null || details.isBlank()) {
      return Optional.empty();
    }
    Optional<String> targetScalar =
        NoteDetailsMarkdown.splitLeadingFrontmatter(details)
            .flatMap(fm -> NoteYamlFrontmatterScalars.firstScalarValue(fm.yamlRaw(), "target"));
    if (targetScalar.isEmpty()) {
      return Optional.empty();
    }
    List<String> inners = WikiLinkMarkdown.innerTitlesInOccurrenceOrder(targetScalar.get());
    if (inners.isEmpty()) {
      return Optional.empty();
    }
    return wikiLinkResolver.resolveWikiInnerTitle(inners.getFirst(), viewer, relation);
  }
}
