package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.NoteViewer;
import java.util.List;
import java.util.stream.Stream;

public record ParentGrandLinkHelperImpl(User user, Note link, LinkingNote parentGrandLink)
    implements ParentGrandLinkHelper {

  @Override
  public LinkingNote getParentGrandLink() {
    return parentGrandLink;
  }

  @Override
  public List<LinkingNote> getCousinLinksAvoidingSiblings() {
    List<Note> linkedSiblingsOfSameLinkType = link.getLinkedSiblingsOfSameLinkType(user);
    return getUncles()
        .flatMap(
            p -> new NoteViewer(user, p.getParent()).linksOfTypeThroughReverse(link.getLinkType()))
        .filter(cousinLink -> !linkedSiblingsOfSameLinkType.contains(cousinLink.getParent()))
        .toList();
  }

  private Stream<LinkingNote> getUncles() {
    List<Note> linkTargetOfType =
        new NoteViewer(user, link.getParent())
            .linksOfTypeThroughDirect(List.of(link.getLinkType())).stream()
                .map(Note::getTargetNote)
                .toList();
    return parentGrandLink
        .getSiblingLinksOfSameLinkType(user)
        .filter(cl1 -> !linkTargetOfType.contains(cl1.getParent()));
  }
}
