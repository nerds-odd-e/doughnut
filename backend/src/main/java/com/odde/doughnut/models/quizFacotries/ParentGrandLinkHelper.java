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

public class ParentGrandLinkHelper {
  private final User user;
  private final Link link;
  final QuizQuestionServant servant;
  private final Link parentGrandLink;

  public ParentGrandLinkHelper(QuizQuestionServant servant, User user, Link link) {
    this.user = user;
    this.link = link;
    this.servant = servant;
    if (servant != null) {
      parentGrandLink = servant.chooseOneCategoryLink(this.user, this.link).orElse(null);
    } else {
      parentGrandLink = null;
    }
  }

  public Link getParentGrandLink() {
    return parentGrandLink;
  }

  public List<ReviewPoint> getCategoryReviewPoints() {
    UserModel userModel = servant.modelFactoryService.toUserModel(user);
    if (parentGrandLink == null) return List.of();
    ReviewPoint reviewPointFor = userModel.getReviewPointFor(parentGrandLink);
    if (reviewPointFor == null) return List.of();
    return List.of(reviewPointFor);
  }

  public List<Link> getCousinLinksAvoidingSiblings() {
    if (parentGrandLink == null) return List.of();
    //    List<Note> linkedSiblingsOfSameLinkType = link.getLinkedSiblingsOfSameLinkType(user);
    return getUncles()
        .flatMap(
            p ->
                new NoteViewer(user, p.getSourceNote())
                    .linksOfTypeThroughReverse(link.getLinkType()))
        .collect(Collectors.toList());
  }

  private Stream<Link> getUncles() {
    List<Note> linkTargetOfType =
        new NoteViewer(user, link.getSourceNote())
            .linkTargetOfType(link.getLinkType())
            .collect(Collectors.toList());
    return parentGrandLink
        .getSiblingLinksOfSameLinkType(user)
        .filter(cl1 -> !linkTargetOfType.contains(cl1.getSourceNote()));
  }
}
