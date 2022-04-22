package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.UserModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FromDifferentPartAsQuizFactory extends AbstractCategoryQuizFactory {
  private List<Note> cachedFillingOptions = null;
  private final Link categoryLink;

  public FromDifferentPartAsQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    super(reviewPoint, servant);
    User user = reviewPoint.getUser();
    if (servant != null) {
      categoryLink = servant.chooseOneCategoryLink(user, link).orElse(null);
    } else {
      categoryLink = null;
    }
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
  public List<Note> generateFillingOptions() {
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
    return this.categoryLink;
  }

  @Override
  public Note generateAnswerNote() {
    User user = reviewPoint.getUser();
    List<Note> uncles = unclesFromSameCategory(user, categoryLink);
    if (uncles.size() > 0) return null;
    return servant
        .randomizer
        .chooseOneRandomly(categoryLink.getReverseLinksOfCousins(user, link.getLinkType()))
        .map(Link::getSourceNote)
        .orElse(null);
  }

  private List<Note> unclesFromSameCategory(User user, Link categoryLink) {
    NoteViewer noteViewer = new NoteViewer(user, link.getSourceNote());
    List<Note> categoryCousins = categoryLink.getCousinsOfSameLinkType(user);
    List<Note> unclesOfCategory =
        noteViewer
            .linkTargetOfType(link.getLinkType())
            .filter(categoryCousins::contains)
            .collect(Collectors.toList());
    return unclesOfCategory;
  }

  @Override
  public List<ReviewPoint> getViceReviewPoints(UserModel userModel) {
    if (categoryLink == null) return List.of();
    ReviewPoint reviewPointFor = userModel.getReviewPointFor(categoryLink);
    if (reviewPointFor == null) return List.of();
    return List.of(reviewPointFor);
  }
}
