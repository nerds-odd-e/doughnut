package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.Link.LinkType;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.factoryServices.quizFacotries.factories.NullParentGrandLinkHelper;
import com.odde.doughnut.factoryServices.quizFacotries.factories.ParentGrandLinkHelper;
import com.odde.doughnut.factoryServices.quizFacotries.factories.ParentGrandLinkHelperImpl;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AiAdvisorService;
import com.odde.doughnut.services.GlobalSettingsService;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QuizQuestionServant {
  private final User user;
  public final Randomizer randomizer;
  final ModelFactoryService modelFactoryService;
  public final AiAdvisorService aiAdvisorService;
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

  public QuizQuestionServant(
      User user,
      Randomizer randomizer,
      ModelFactoryService modelFactoryService,
      AiAdvisorService aiAdvisorService) {
    this.user = user;
    this.randomizer = randomizer;
    this.modelFactoryService = modelFactoryService;
    this.aiAdvisorService = aiAdvisorService;
  }

  public List<Note> chooseFromCohort(Note answerNote, Predicate<Note> notePredicate) {
    List<Note> list = getCohort(answerNote, notePredicate);
    return randomizer.randomlyChoose(maxFillingOptionCount, list);
  }

  public List<Note> getCohort(Note note, Predicate<Note> notePredicate) {
    List<Note> list =
        note.getSiblings().stream().filter(notePredicate).collect(Collectors.toList());
    if (list.size() > 1) return list;

    return note.getGrandAsPossible().getDescendantsInBreathFirstOrder().stream()
        .filter(notePredicate)
        .collect(Collectors.toList());
  }

  private Optional<Link> chooseOneCategoryLink(Link link) {
    return randomizer.chooseOneRandomly(link.categoryLinksOfTarget(this.user));
  }

  public <T> List<T> chooseFillingOptionsRandomly(List<T> candidates) {
    return randomizer.randomlyChoose(maxFillingOptionCount, candidates);
  }

  public Stream<Link> getSiblingLinksOfSameLinkTypeHavingReviewPoint(Link link) {
    Stream<Link> siblingLinksOfSameLinkType = link.getSiblingLinksOfSameLinkType(this.user);
    return linksWithReviewPoint(siblingLinksOfSameLinkType);
  }

  public Stream<Link> getLinksFromSameSourceHavingReviewPoint(Link link) {
    Stream<Link> stream =
        new NoteViewer(this.user, link.getSourceNote())
            .linksOfTypeThroughDirect(candidateQuestionLinkTypes).stream();
    return linksWithReviewPoint(stream).filter(l -> !link.equals(l));
  }

  private Stream<Link> linksWithReviewPoint(Stream<Link> cousinLinksOfSameLinkType) {
    return cousinLinksOfSameLinkType.filter(l -> getReviewPoint(l.getThing()) != null);
  }

  public List<ReviewPoint> getReviewPoints(Link link) {
    ReviewPoint reviewPointFor = getReviewPoint(link.getThing());
    if (reviewPointFor == null) return List.of();
    return List.of(reviewPointFor);
  }

  public ParentGrandLinkHelper getParentGrandLinkHelper(Link link) {
    Link parentGrandLink = chooseOneCategoryLink(link).orElse(null);
    if (parentGrandLink == null) return new NullParentGrandLinkHelper();
    return new ParentGrandLinkHelperImpl(this.user, link, parentGrandLink);
  }

  public List<Note> chooseBackwardPeers(Link instanceLink, Link link1) {
    List<Note> instanceReverse = instanceLink.getLinkedSiblingsOfSameLinkType(user);
    List<Note> specReverse = link1.getLinkedSiblingsOfSameLinkType(user);
    List<Note> backwardPeers =
        Stream.concat(instanceReverse.stream(), specReverse.stream())
            .filter(n -> !(instanceReverse.contains(n) && specReverse.contains(n)))
            .collect(Collectors.toList());
    return chooseFillingOptionsRandomly(backwardPeers);
  }

  public ReviewPoint getReviewPoint(Thing thing) {
    UserModel userModel = modelFactoryService.toUserModel(user);
    return userModel.getReviewPointFor(thing);
  }

  public List<Note> chooseFromCohortAvoidUncles(Link link1, Note answerNote) {
    List<Note> uncles = link1.getPiblingOfTheSameLinkType(user);
    return chooseCohortAndAvoid(answerNote, link1.getSourceNote(), uncles);
  }

  private List<Note> chooseCohortAndAvoid(
      Note answerNote, Note noteToAvoid, List<Note> notesToAvoid) {
    return chooseFromCohort(
        answerNote,
        n -> !n.equals(answerNote) && !n.equals(noteToAvoid) && !notesToAvoid.contains(n));
  }

  public List<Link> chooseLinkFromCohortAvoidSiblingsOfSameLinkType(Link link1, Note answerNote1) {
    return chooseFromCohortAvoidSiblings(link1, answerNote1).stream()
        .filter(
            n1 ->
                !new NoteViewer(user, n1)
                    .linksOfTypeThroughDirect(List.of(link1.getLinkType()))
                    .isEmpty())
        .map(n -> n.getLinks().get(0))
        .collect(Collectors.toList());
  }

  public List<Note> chooseFromCohortAvoidSiblings(Link link1, Note answerNote1) {
    List<Note> linkedSiblingsOfSameLinkType = link1.getLinkedSiblingsOfSameLinkType(user);
    return chooseCohortAndAvoid(answerNote1, link1.getTargetNote(), linkedSiblingsOfSameLinkType);
  }

  public GlobalSettingsService getGobalSettingsService() {
    return new GlobalSettingsService(modelFactoryService);
  }
}
