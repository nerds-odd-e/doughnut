package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.NoteViewer;
import java.util.List;
import java.util.stream.Stream;

public record ParentGrandLinkHelperImpl(User user, Note link, Thing parentGrandLink)
    implements ParentGrandLinkHelper {

  @Override
  public Thing getParentGrandLink() {
    return parentGrandLink;
  }

  @Override
  public List<Thing> getCousinLinksAvoidingSiblings() {
    List<Note> linkedSiblingsOfSameLinkType = link.getThing().getLinkedSiblingsOfSameLinkType(user);
    return getUncles()
        .flatMap(
            p ->
                new NoteViewer(user, p.getParentNote())
                    .linksOfTypeThroughReverse(link.getLinkType()))
        .filter(cousinLink -> !linkedSiblingsOfSameLinkType.contains(cousinLink.getParentNote()))
        .toList();
  }

  private Stream<Thing> getUncles() {
    List<Note> linkTargetOfType =
        new NoteViewer(user, link.getParent())
            .linksOfTypeThroughDirect(List.of(link.getLinkType())).stream()
                .map(Thing::getTargetNote)
                .toList();
    return parentGrandLink
        .getSiblingLinksOfSameLinkType(user)
        .filter(cl1 -> !linkTargetOfType.contains(cl1.getParentNote()));
  }
}
