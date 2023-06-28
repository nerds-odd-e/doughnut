package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Link.LinkType;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AiAdvisorService;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QuizQuestionServant {
  final Randomizer randomizer;
  final ModelFactoryService modelFactoryService;
  final AiAdvisorService aiAdvisorService;
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
      Randomizer randomizer,
      ModelFactoryService modelFactoryService,
      AiAdvisorService aiAdvisorService) {
    this.randomizer = randomizer;
    this.modelFactoryService = modelFactoryService;
    this.aiAdvisorService = aiAdvisorService;
  }

  List<Note> chooseFromCohort(Note answerNote, Predicate<Note> notePredicate) {
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

  Optional<Link> chooseOneCategoryLink(User user, Link link) {
    return randomizer.chooseOneRandomly(link.categoryLinksOfTarget(user));
  }

  <T> List<T> chooseFillingOptionsRandomly(List<T> candidates) {
    return randomizer.randomlyChoose(maxFillingOptionCount, candidates);
  }

  public Stream<Link> getSiblingLinksOfSameLinkTypeHavingReviewPoint(Link link, User user) {
    Stream<Link> siblingLinksOfSameLinkType = link.getSiblingLinksOfSameLinkType(user);
    return linksWithReviewPoint(siblingLinksOfSameLinkType, user);
  }

  public Stream<Link> getLinksFromSameSourceHavingReviewPoint(User user, Link link) {
    Stream<Link> stream =
        new NoteViewer(user, link.getSourceNote())
            .linksOfTypeThroughDirect(candidateQuestionLinkTypes).stream();
    return linksWithReviewPoint(stream, user).filter(l -> !link.equals(l));
  }

  private Stream<Link> linksWithReviewPoint(Stream<Link> cousinLinksOfSameLinkType, User user) {
    UserModel userModel = modelFactoryService.toUserModel(user);
    return cousinLinksOfSameLinkType.filter(l -> userModel.getReviewPointFor(l) != null);
  }

  public List<ReviewPoint> getReviewPoints(Link link, User user) {
    UserModel userModel = modelFactoryService.toUserModel(user);
    ReviewPoint reviewPointFor = userModel.getReviewPointFor(link);
    if (reviewPointFor == null) return List.of();
    return List.of(reviewPointFor);
  }

  ParentGrandLinkHelper getParentGrandLinkHelper(User user, Link link) {
    Link parentGrandLink = chooseOneCategoryLink(user, link).orElse(null);
    if (parentGrandLink == null) return new NullParentGrandLinkHelper();
    return new ParentGrandLinkHelperImpl(user, link, parentGrandLink);
  }
}
