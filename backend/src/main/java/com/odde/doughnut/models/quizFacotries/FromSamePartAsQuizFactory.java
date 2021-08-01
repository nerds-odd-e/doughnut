package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.json.LinkViewed;
import com.odde.doughnut.models.UserModel;

import java.util.*;
import java.util.stream.Collectors;

public class FromSamePartAsQuizFactory implements QuizQuestionFactory {
    private Link cachedAnswerLink = null;
    private List<Note> cachedFillingOptions = null;
    protected final ReviewPoint reviewPoint;
    protected final Link link;
    private Optional<Link> categoryLink = null;

    public FromSamePartAsQuizFactory(ReviewPoint reviewPoint) {
        this.reviewPoint = reviewPoint;
        this.link = reviewPoint.getLink();
    }

    @Override
    public List<Note> generateFillingOptions(QuizQuestionServant servant) {
        if (cachedFillingOptions == null) {
            categoryLink = servant.chooseOneCategoryLink(reviewPoint.getUser(), link);
            cachedFillingOptions = categoryLink
                    .map(lk->
                    servant.randomizer.randomlyChoose(
                    5, link.getRemoteCousinOfDifferentCategory(lk, reviewPoint.getUser()))
                            .stream().map(Link::getSourceNote).collect(Collectors.toList())).orElse(Collections.emptyList());
        }
        return cachedFillingOptions;
    }

    @Override
    public String generateInstruction() {
        return "<p>Which one <mark>" +link.getLinkTypeLabel() +"</mark> the same <mark>" + categoryLink.map(lk->lk.getTargetNote().getTitle()).orElse("") + "</mark> as:";
    }

    @Override
    public String generateMainTopic() {
        return link.getSourceNote().getTitle();
    }

    @Override
    public Note generateAnswerNote(QuizQuestionServant servant) {
        if (getAnswerLink(servant) == null) return null;
        return getAnswerLink(servant).getSourceNote();
    }

    @Override
    public Map<Link.LinkType, LinkViewed> generateHintLinks() {
        return null;
    }

    @Override
    public int minimumFillingOptionCount() {
        return 1;
    }

    @Override
    public int minimumViceReviewPointCount() {
        return 1;
    }

    @Override
    public List<ReviewPoint> getViceReviewPoints(UserModel userModel) {
        if (cachedAnswerLink != null) {
            ReviewPoint answerLinkReviewPoint = userModel.getReviewPointFor(cachedAnswerLink);
            List<ReviewPoint> result = new ArrayList<>();
            result.add(answerLinkReviewPoint);
            categoryLink.map(userModel::getReviewPointFor).ifPresent(result::add);
            return result;
        }
        return Collections.emptyList();
    }

    protected Link getAnswerLink(QuizQuestionServant servant) {
        if (cachedAnswerLink == null) {
            UserModel userModel = servant.modelFactoryService.toUserModel(reviewPoint.getUser());
            List<Link> backwardPeers = link.getCousinLinksOfSameLinkType(reviewPoint.getUser()).stream()
                    .filter(l->userModel.getReviewPointFor(l) != null).collect(Collectors.toUnmodifiableList());
            cachedAnswerLink = servant.randomizer.chooseOneRandomly(backwardPeers);
        }
        return cachedAnswerLink;
    }

}