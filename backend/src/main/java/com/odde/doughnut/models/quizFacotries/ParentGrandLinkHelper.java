package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.NoteViewer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ParentGrandLinkHelper(User user, Link link, Link parentGrandLink)
    implements IParentGrandLinkHelper {

  @Override
  public Link getParentGrandLink() {
    return parentGrandLink;
  }

  @Override
  public List<Link> getCousinLinksAvoidingSiblings() {
    List<Note> linkedSiblingsOfSameLinkType = link.getLinkedSiblingsOfSameLinkType(user);
    return getUncles()
        .flatMap(
            p ->
                new NoteViewer(user, p.getSourceNote())
                    .linksOfTypeThroughReverse(link.getLinkType()))
        .filter(cousinLink -> !linkedSiblingsOfSameLinkType.contains(cousinLink.getSourceNote()))
        .collect(Collectors.toList());
  }

  private Stream<Link> getUncles() {
    List<Note> linkTargetOfType =
        new NoteViewer(user, link.getSourceNote())
            .linksOfTypeThroughDirect(List.of(link.getLinkType())).stream()
                .map(Link::getTargetNote)
                .collect(Collectors.toList());
    return parentGrandLink
        .getSiblingLinksOfSameLinkType(user)
        .filter(cl1 -> !linkTargetOfType.contains(cl1.getSourceNote()));
  }
}
