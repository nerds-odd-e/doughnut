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

  public Stream<Note> chooseFromCohort(Note answerNote, Predicate<Note> notePredicate) {
    return randomizer.randomlyChoose(maxFillingOptionCount, getCohort(answerNote, notePredicate));
  }

  public Stream<Note> getCohort(Note note, Predicate<Note> notePredicate) {
    List<Note> list = note.getSiblings().filter(notePredicate).toList();
    if (list.size() > 1) return list.stream();

    return this.modelFactoryService
        .toNoteModel(note.getGrandAsPossible())
        .getDescendantsInBreathFirstOrder()
        .stream()
        .filter(notePredicate);
  }

  private Optional<Link> chooseOneCategoryLink(Link link) {
    return randomizer.chooseOneRandomly(link.categoryLinksOfTarget(this.user));
  }

  public <T> Stream<T> chooseFillingOptionsRandomly(Stream<T> candidates) {
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

  public ParentGrandLinkHelper getParentGrandLinkHelper(Link link) {
    Link parentGrandLink = chooseOneCategoryLink(link).orElse(null);
    if (parentGrandLink == null) return new NullParentGrandLinkHelper();
    return new ParentGrandLinkHelperImpl(this.user, link, parentGrandLink);
  }

  public Stream<Note> chooseBackwardPeers(Link instanceLink, Link link1) {
    List<Note> instanceReverse = instanceLink.getLinkedSiblingsOfSameLinkType(user);
    List<Note> specReverse = link1.getLinkedSiblingsOfSameLinkType(user);
    Stream<Note> backwardPeers =
        Stream.concat(instanceReverse.stream(), specReverse.stream())
            .filter(n -> !(instanceReverse.contains(n) && specReverse.contains(n)));
    return chooseFillingOptionsRandomly(backwardPeers);
  }

  public ReviewPoint getReviewPoint(Thing thing) {
    UserModel userModel = modelFactoryService.toUserModel(user);
    return userModel.getReviewPointFor(thing);
  }

  public Stream<Note> chooseFromCohortAvoidUncles(Link link1, Note answerNote) {
    List<Note> uncles = link1.getPiblingOfTheSameLinkType(user);
    return chooseCohortAndAvoid(answerNote, link1.getSourceNote(), uncles);
  }

  private Stream<Note> chooseCohortAndAvoid(
      Note answerNote, Note noteToAvoid, List<Note> notesToAvoid) {
    return chooseFromCohort(
        answerNote,
        n -> !n.equals(answerNote) && !n.equals(noteToAvoid) && !notesToAvoid.contains(n));
  }

  public List<Link> chooseLinkFromCohortAvoidSiblingsOfSameLinkType(Link link, Note answerNote) {
    return chooseFromCohortAvoidSiblings(link, answerNote)
        .flatMap(n -> n.getLinks().stream())
        .collect(Collectors.toList());
  }

  public Stream<Note> chooseFromCohortAvoidSiblings(Link link1, Note answerNote1) {
    List<Note> linkedSiblingsOfSameLinkType = link1.getLinkedSiblingsOfSameLinkType(user);
    return chooseCohortAndAvoid(answerNote1, link1.getTargetNote(), linkedSiblingsOfSameLinkType);
  }

  public GlobalSettingsService getGlobalSettingsService() {
    return new GlobalSettingsService(modelFactoryService);
  }
}
