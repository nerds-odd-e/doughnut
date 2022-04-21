package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.UserModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FromDifferentPartAsQuizFactory implements QuizQuestionFactory, QuestionOptionsFactory {
  protected final ReviewPoint reviewPoint;
  protected final Link link;
  private QuizQuestionServant servant;
  private List<Note> cachedFillingOptions = null;
  private Optional<Link> categoryLink;

  public FromDifferentPartAsQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    this.reviewPoint = reviewPoint;
    this.link = reviewPoint.getLink();
    this.servant = servant;
  }

  @Override
  public int minimumOptionCount() {
    return 2;
  }

  @Override
  public List<Note> allWrongAnswers() {
    List<Note> result =
        new ArrayList<>(reviewPoint.getLink().getCousinsOfSameLinkType(reviewPoint.getUser()));
    result.add(link.getSourceNote());
    return result;
  }

  @Override
  public List<Note> generateFillingOptions(QuizQuestionServant servant) {
    if (cachedFillingOptions == null) {
      List<Link> cousinLinks = link.getCousinLinksOfSameLinkType(reviewPoint.getUser());
      cachedFillingOptions =
          servant.randomizer.randomlyChoose(5, cousinLinks).stream()
              .map(Link::getSourceNote)
              .collect(Collectors.toList());
    }
    return cachedFillingOptions;
  }

  @Override
  public Link getCategoryLink() {
    return this.categoryLink.orElse(null);
  }

  @Override
  public Note generateAnswerNote() {
    User user = reviewPoint.getUser();
    categoryLink = servant.chooseOneCategoryLink(user, link);
    return categoryLink
        .filter(cl -> noUncles(user, cl))
        .map(lk -> lk.getReverseLinksOfCousins(user, link.getLinkType()))
        .flatMap(servant.randomizer::chooseOneRandomly)
        .map(Link::getSourceNote)
        .orElse(null);
  }

  private boolean noUncles(User user, Link categoryLink) {
    NoteViewer noteViewer = new NoteViewer(user, link.getSourceNote());
    List<Note> categoryCousins = categoryLink.getCousinsOfSameLinkType(user);
    return noteViewer.linkTargetOfType(link.getLinkType()).noneMatch(categoryCousins::contains);
  }

  @Override
  public List<ReviewPoint> getViceReviewPoints(UserModel userModel) {
    return categoryLink
        .map(userModel::getReviewPointFor)
        .map(List::of)
        .orElse(Collections.emptyList());
  }
}
