package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Link.LinkType;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.models.UserModel;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QuizQuestionServant {
  final Randomizer randomizer;
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

  public QuizQuestionServant(Randomizer randomizer, ModelFactoryService modelFactoryService) {
    this.randomizer = randomizer;
    this.modelFactoryService = modelFactoryService;
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

  public Stream<Link> getCousinLinksOfSameLinkTypeHavingReviewPoint(Link link, User user) {
    Stream<Link> cousinLinksOfSameLinkType = link.getCousinLinksOfSameLinkType(user);
    return linksWithReviewPoint(cousinLinksOfSameLinkType, user);
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
}
