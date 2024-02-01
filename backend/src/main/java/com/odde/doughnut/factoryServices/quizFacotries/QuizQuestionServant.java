package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.LinkType;
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

  public List<Note> chooseFromCohort(NoteBase answerNote, Predicate<Note> notePredicate) {
    List<Note> list = getCohort(answerNote, n -> !n.equals(answerNote) && notePredicate.test(n));
    return randomizer.randomlyChoose(maxFillingOptionCount, list).toList();
  }

  public List<Note> getCohort(NoteBase note, Predicate<Note> notePredicate) {
    List<Note> list =
        note.getSiblings().stream().filter(notePredicate).collect(Collectors.toList());
    if (list.size() > 0) return list;

    Note grand = modelFactoryService.convertToNote(note);
    for (int i = 0; i < 2; i++)
      if (grand.getParent() != null) {
        grand = grand.getParent();
      }
    return grand.getDescendants().filter(notePredicate).collect(Collectors.toList());
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

  public ParentGrandLinkHelper getParentGrandLinkHelper(Note link) {
    Note parentGrandLink =
        randomizer
            .chooseOneRandomly(
                link.targetNoteViewer(this.user)
                    .linksOfTypeThroughDirect(
                        List.of(
                            LinkType.PART,
                            LinkType.INSTANCE,
                            LinkType.SPECIALIZE,
                            LinkType.APPLICATION)))
            .orElse(null);
    if (parentGrandLink == null) return new NullParentGrandLinkHelper();
    return new ParentGrandLinkHelperImpl(this.user, link, parentGrandLink);
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

  public ReviewPoint getReviewPoint(Note thing) {
    UserModel userModel = modelFactoryService.toUserModel(user);
    return userModel.getReviewPointFor(thing);
  }

  public List<Note> chooseFromCohortAvoidUncles(Note note, NoteBase answerNote) {
    List<Note> uncles =
        new NoteViewer(user, note.getParent())
            .linksOfTypeThroughDirect(List.of(note.getLinkType())).stream()
                .filter(l -> !l.equals(note))
                .map(Note::getTargetNote)
                .toList();
    return chooseCohortAndAvoid(answerNote, note.getParent(), uncles);
  }

  private List<Note> chooseCohortAndAvoid(
      NoteBase answerNote, Note noteToAvoid, List<Note> notesToAvoid) {
    return chooseFromCohort(answerNote, n -> !n.equals(noteToAvoid) && !notesToAvoid.contains(n));
  }

  public List<Note> chooseFromCohortAvoidSiblings(Note answerLink) {
    List<Note> linkedSiblingsOfSameLinkType = answerLink.getLinkedSiblingsOfSameLinkType(user);
    return chooseCohortAndAvoid(
        answerLink.getParent(), answerLink.getTargetNote(), linkedSiblingsOfSameLinkType);
  }

  public GlobalSettingsService getGlobalSettingsService() {
    return new GlobalSettingsService(modelFactoryService);
  }
}
