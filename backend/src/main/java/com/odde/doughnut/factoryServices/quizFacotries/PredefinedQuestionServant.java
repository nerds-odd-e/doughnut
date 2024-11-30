package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.LinkType;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GlobalSettingsService;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PredefinedQuestionServant {
  private final User user;
  public final Randomizer randomizer;
  final ModelFactoryService modelFactoryService;
  final int maxFillingOptionCount = 2;
  private final List<LinkType> candidateQuestionLinkTypes =
      List.of(
          LinkType.SPECIALIZE,
          LinkType.APPLICATION,
          LinkType.INSTANCE,
          LinkType.TAGGED_BY,
          LinkType.ATTRIBUTE,
          LinkType.USES,
          LinkType.RELATED_TO);

  public PredefinedQuestionServant(
      User user, Randomizer randomizer, ModelFactoryService modelFactoryService) {
    this.user = user;
    this.randomizer = randomizer;
    this.modelFactoryService = modelFactoryService;
  }

  public List<Note> getCohort(Note note, Predicate<Note> notePredicate) {
    List<Note> list = note.getNoneLinkSiblings().stream().filter(notePredicate).toList();
    if (!list.isEmpty()) return list;

    Note grand = note;
    for (int i = 0; i < 2; i++)
      if (grand.getParent() != null) {
        grand = grand.getParent();
      }
    return grand.getAllNoneLinkDescendants().filter(notePredicate).collect(Collectors.toList());
  }

  public <T> List<T> chooseFillingOptionsRandomly(List<T> candidates) {
    return randomizer.randomlyChoose(maxFillingOptionCount, candidates).toList();
  }

  public Stream<Note> getSiblingLinksOfSameLinkTypeHavingReviewPoint(Note link) {
    return linksWithReviewPoint(link.getSiblingLinksOfSameLinkType(this.user));
  }

  public Stream<Note> getLinksFromSameSourceHavingReviewPoint(Note link) {
    List<Note> list =
        new NoteViewer(this.user, link.getParent())
            .linksOfTypeThroughDirect(candidateQuestionLinkTypes);
    return linksWithReviewPoint(list.stream()).filter(l -> !link.equals(l));
  }

  private Stream<Note> linksWithReviewPoint(Stream<Note> cousinLinksOfSameLinkType) {
    return cousinLinksOfSameLinkType.filter(l -> getReviewPoint(l) != null);
  }

  public Note getParentGrandLink(Note link) {
    return randomizer
        .chooseOneRandomly(
            link.targetNoteViewer(this.user)
                .linksOfTypeThroughDirect(
                    List.of(
                        LinkType.PART,
                        LinkType.INSTANCE,
                        LinkType.SPECIALIZE,
                        LinkType.APPLICATION)))
        .orElse(null);
  }

  public List<Note> chooseBackwardPeers(Note instanceLink, Note link1) {
    List<Note> instanceReverse = instanceLink.getLinkedSiblingsOfSameLinkType(user);
    List<Note> specReverse = link1.getLinkedSiblingsOfSameLinkType(user);
    List<Note> backwardPeers =
        Stream.concat(instanceReverse.stream(), specReverse.stream())
            .filter(n -> !(instanceReverse.contains(n) && specReverse.contains(n)))
            .collect(Collectors.toList());
    return chooseFillingOptionsRandomly(backwardPeers);
  }

  public MemoryTracker getReviewPoint(Note thing) {
    UserModel userModel = modelFactoryService.toUserModel(user);
    return userModel.getReviewPointFor(thing);
  }

  public List<Note> chooseFromCohortAvoidUncles(Note note, Note answerNote) {
    List<Note> uncles =
        new NoteViewer(user, note.getParent())
            .linksOfTypeThroughDirect(List.of(note.getLinkType())).stream()
                .filter(l -> !l.equals(note))
                .map(Note::getTargetNote)
                .toList();
    return chooseCohortAndAvoid(answerNote, note.getParent(), uncles);
  }

  private List<Note> chooseCohortAndAvoid(
      Note answerNote, Note noteToAvoid, List<Note> notesToAvoid) {
    List<Note> list =
        getCohort(
            answerNote,
            n1 -> !n1.equals(answerNote) && !n1.equals(noteToAvoid) && !notesToAvoid.contains(n1));
    return randomizer.randomlyChoose(maxFillingOptionCount, list).toList();
  }

  public List<Note> chooseFromCohortAvoidSiblings(Note answerLink) {
    List<Note> linkedSiblingsOfSameLinkType = answerLink.getLinkedSiblingsOfSameLinkType(user);
    return chooseCohortAndAvoid(
        answerLink.getParent(), answerLink.getTargetNote(), linkedSiblingsOfSameLinkType);
  }

  public GlobalSettingsService getGlobalSettingsService() {
    return new GlobalSettingsService(modelFactoryService);
  }

  public List<Note> getCousinLinksAvoidingSiblings(Note link, Note parentGrandLink) {
    if (parentGrandLink == null) return List.of();
    List<Note> linkedSiblingsOfSameLinkType = link.getLinkedSiblingsOfSameLinkType(user);
    List<Note> linkTargetOfType =
        new NoteViewer(user, link.getParent())
            .linksOfTypeThroughDirect(List.of(link.getLinkType())).stream()
                .map(Note::getTargetNote)
                .toList();
    Stream<Note> uncles =
        parentGrandLink
            .getSiblingLinksOfSameLinkType(user)
            .filter(cl1 -> !linkTargetOfType.contains(cl1.getParent()));
    return uncles
        .flatMap(
            p -> new NoteViewer(user, p.getParent()).linksOfTypeThroughReverse(link.getLinkType()))
        .filter(cousinLink -> !linkedSiblingsOfSameLinkType.contains(cousinLink.getParent()))
        .toList();
  }
}
