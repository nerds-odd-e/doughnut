package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.UserModel;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractCategoryQuizFactory
    implements QuizQuestionFactory, QuestionOptionsFactory {
  protected final ReviewPoint reviewPoint;
  protected final Link link;
  protected final QuizQuestionServant servant;
  protected final Link categoryLink;

  public AbstractCategoryQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    this.reviewPoint = reviewPoint;
    this.link = reviewPoint.getLink();
    this.servant = servant;
    if (servant != null) {
      categoryLink = servant.chooseOneCategoryLink(reviewPoint.getUser(), link).orElse(null);
    } else {
      categoryLink = null;
    }
  }

  protected List<ReviewPoint> getCategoryReviewPoints(UserModel userModel) {
    if (categoryLink == null) return List.of();
    ReviewPoint reviewPointFor = userModel.getReviewPointFor(categoryLink);
    if (reviewPointFor == null) return List.of();
    return List.of(reviewPointFor);
  }

  protected List<Link> getReverseLinksOfCousins(User user) {
    List<Note> uncles = unclesFromSameCategory(user, categoryLink);
    return categoryLink.getCousinLinksOfSameLinkType(user).stream()
        .filter(cl -> !uncles.contains(cl.getSourceNote()))
        .flatMap(
            p ->
                new NoteViewer(user, p.getSourceNote())
                    .linksOfTypeThroughReverse(link.getLinkType()))
        .collect(Collectors.toList());
  }

  protected List<Note> unclesFromSameCategory(User user, Link categoryLink) {
    NoteViewer noteViewer = new NoteViewer(user, link.getSourceNote());
    List<Note> categoryCousins = categoryLink.getCousinsOfSameLinkType(user);
    return noteViewer
        .linkTargetOfType(link.getLinkType())
        .filter(categoryCousins::contains)
        .collect(Collectors.toList());
  }

  protected Stream<Link> getCousinLinksFromSameCategoriesOfSameLinkType() {
    UserModel userModel = servant.modelFactoryService.toUserModel(reviewPoint.getUser());
    return new NoteViewer(reviewPoint.getUser(), categoryLink.getSourceNote())
        .linksOfTypeThroughReverse(link.getLinkType())
        .filter(lk -> lk != link)
        .filter(l -> userModel.getReviewPointFor(l) != null);
  }
}
