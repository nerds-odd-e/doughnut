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

public class CategoryHelper {
  private final User user;
  private final Link link;
  private final QuizQuestionServant servant;
  private final Link categoryLink;

  public CategoryHelper(QuizQuestionServant servant, User user, Link link) {
    this.user = user;
    this.link = link;
    this.servant = servant;
    if (servant != null) {
      categoryLink = servant.chooseOneCategoryLink(this.user, this.link).orElse(null);
    } else {
      categoryLink = null;
    }
  }

  public Link getCategoryLink() {
    return categoryLink;
  }

  public List<ReviewPoint> getCategoryReviewPoints(UserModel userModel) {
    if (categoryLink == null) return List.of();
    ReviewPoint reviewPointFor = userModel.getReviewPointFor(categoryLink);
    if (reviewPointFor == null) return List.of();
    return List.of(reviewPointFor);
  }

  public List<Link> getReverseLinksOfCousins() {
    if (categoryLink == null) return List.of();
    List<Link> uncles = unclesFromSameCategory();
    return categoryLink.getCousinLinksOfSameLinkType(user).stream()
        .filter(cl -> !uncles.contains(cl))
        .flatMap(
            p ->
                new NoteViewer(user, p.getSourceNote())
                    .linksOfTypeThroughReverse(link.getLinkType()))
        .collect(Collectors.toList());
  }

  private List<Link> unclesFromSameCategory() {
    List<Note> linkTargetOfType =
        new NoteViewer(user, link.getSourceNote())
            .linkTargetOfType(link.getLinkType())
            .collect(Collectors.toList());
    return categoryLink.getCousinLinksOfSameLinkType(user).stream()
        .filter(cl -> linkTargetOfType.contains(cl.getSourceNote()))
        .collect(Collectors.toList());
  }

  public Stream<Link> getCousinLinksFromSameCategoriesOfSameLinkType() {
    if (categoryLink == null) return Stream.of();
    UserModel userModel = servant.modelFactoryService.toUserModel(user);
    return new NoteViewer(user, categoryLink.getSourceNote())
        .linksOfTypeThroughReverse(link.getLinkType())
        .filter(lk -> lk != link)
        .filter(l -> userModel.getReviewPointFor(l) != null);
  }
}
